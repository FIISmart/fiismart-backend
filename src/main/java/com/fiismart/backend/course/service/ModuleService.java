package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.request.CreateModuleRequest;
import com.fiismart.backend.course.dto.response.ModuleResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import database.dao.CourseDAO;
import database.model.Course;
import database.model.Module;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModuleService {

    private final CourseDAO courseDAO;

    public ModuleService(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    /**
     * Adaugă un modul nou la un curs.
     */
    public ModuleResponse addModule(String courseId, CreateModuleRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        if (!courseDAO.existsById(cid)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        Module module = Module.builder()
                .id(new ObjectId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(req.getOrder())
                .lectures(new ArrayList<>())
                .build();

        courseDAO.addModule(cid, module);
        courseDAO.updateUpdatedAt(cid, new Date());
        return ModuleResponse.fromModel(module);
    }

    /**
     * Returnează toate modulele unui curs, sortate după order.
     */
    public List<ModuleResponse> getModules(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Course course = getCourseOrThrow(cid, courseId);
        return course.getModules().stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .map(ModuleResponse::fromModel)
                .collect(Collectors.toList());
    }

    /**
     * Actualizează titlul și/sau descrierea unui modul.
     */
    public ModuleResponse updateModule(String courseId, String moduleId, CreateModuleRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");

        Course course = getCourseOrThrow(cid, courseId);
        Module module = findModuleOrThrow(course.getModules(), mid, moduleId);

        if (req.getTitle() != null) {
            courseDAO.updateModuleField(cid, mid, "title", req.getTitle());
            module.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            courseDAO.updateModuleField(cid, mid, "description", req.getDescription());
            module.setDescription(req.getDescription());
        }

        courseDAO.updateUpdatedAt(cid, new Date());
        return ModuleResponse.fromModel(module);
    }

    /**
     * Șterge un modul (împreună cu lecturile din el).
     */
    public void deleteModule(String courseId, String moduleId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");

        Course course = getCourseOrThrow(cid, courseId);
        findModuleOrThrow(course.getModules(), mid, moduleId);

        courseDAO.removeModule(cid, mid);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    /**
     * Reordonează modulele (drag & drop pe frontend).
     * orderedModuleIds = lista de ID-uri în noua ordine dorită.
     */
    public List<ModuleResponse> reorderModules(String courseId, List<String> orderedModuleIds) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Course course = getCourseOrThrow(cid, courseId);

        List<Module> existing = course.getModules();
        if (orderedModuleIds.size() != existing.size()) {
            throw new BadRequestException("Numărul de ID-uri nu corespunde cu numărul de module");
        }

        List<Module> reordered = new ArrayList<>();
        for (int i = 0; i < orderedModuleIds.size(); i++) {
            ObjectId mid = toObjectId(orderedModuleIds.get(i), "Invalid module ID");
            int newOrder = i;
            Module found = existing.stream()
                    .filter(m -> mid.equals(m.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + mid));
            found.setOrder(newOrder);
            reordered.add(found);
        }

        courseDAO.replaceModules(cid, reordered);
        courseDAO.updateUpdatedAt(cid, new Date());

        return reordered.stream()
                .map(ModuleResponse::fromModel)
                .collect(Collectors.toList());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private Course getCourseOrThrow(ObjectId cid, String courseId) {
        Course course = courseDAO.findById(cid);
        if (course == null) throw new ResourceNotFoundException("Course not found: " + courseId);
        return course;
    }

    private Module findModuleOrThrow(List<Module> modules, ObjectId mid, String moduleId) {
        return modules.stream()
                .filter(m -> mid.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));
    }

    private ObjectId toObjectId(String id, String msg) {
        if (id == null || !ObjectId.isValid(id)) throw new BadRequestException(msg + ": " + id);
        return new ObjectId(id);
    }
}

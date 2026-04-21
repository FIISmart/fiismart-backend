package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.request.CreateLectureRequest;
import com.fiismart.backend.course.dto.request.UpdateLectureRequest;
import com.fiismart.backend.course.dto.response.LectureResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import com.fiismart.backend.course.helper.UrlTokenizer;
import database.dao.CourseDAO;
import database.model.Course;
import database.model.Lecture;
import database.model.Module;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LectureService {

    private final CourseDAO courseDAO;
    private final UrlTokenizer urlTokenizer;

    public LectureService(CourseDAO courseDAO, UrlTokenizer urlTokenizer) {
        this.courseDAO = courseDAO;
        this.urlTokenizer = urlTokenizer;
    }

    // ── LECTURI LIBERE (fără modul) ───────────────────────────────────────────

    /**
     * Adaugă o lectură direct la curs (fără modul).
     */
    public LectureResponse addLectureToCourse(String courseId, CreateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        if (!courseDAO.existsById(cid)) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        Lecture lecture = buildLecture(req);
        courseDAO.addLecture(cid, lecture);
        courseDAO.updateUpdatedAt(cid, new Date());
        return LectureResponse.fromModel(lecture);
    }

    /**
     * Actualizează o lectură liberă din curs.
     */
    public LectureResponse updateLectureInCourse(String courseId, String lectureId, UpdateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        Course course = getCourseOrThrow(cid, courseId);
        Lecture existing = findLectureInList(course.getLectures(), lid, lectureId);

        applyUpdates(cid, lid, req, false, null);
        courseDAO.updateUpdatedAt(cid, new Date());

        // Re-fetch pentru response actualizat
        Course updated = courseDAO.findById(cid);
        Lecture updatedLecture = findLectureInList(updated.getLectures(), lid, lectureId);
        return LectureResponse.fromModel(updatedLecture);
    }

    /**
     * Șterge o lectură liberă din curs.
     */
    public void removeLectureFromCourse(String courseId, String lectureId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        Course course = getCourseOrThrow(cid, courseId);
        findLectureInList(course.getLectures(), lid, lectureId); // validare existență

        courseDAO.removeLecture(cid, lid);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    /**
     * Returnează toate lecturile libere ale unui curs.
     */
    public List<LectureResponse> getLecturesForCourse(String courseId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        Course course = getCourseOrThrow(cid, courseId);
        return course.getLectures().stream()
                .map(LectureResponse::fromModel)
                .collect(Collectors.toList());
    }


    /**
     * Adaugă o lectură într-un modul specific.
     */
    public LectureResponse addLectureToModule(String courseId, String moduleId, CreateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");

        Course course = getCourseOrThrow(cid, courseId);
        Module module = findModuleInList(course.getModules(), mid, moduleId);

        Lecture lecture = buildLecture(req);
        module.getLectures().add(lecture);

        courseDAO.replaceModule(cid, module);
        courseDAO.updateUpdatedAt(cid, new Date());
        return LectureResponse.fromModel(lecture);
    }

    /**
     * Actualizează o lectură dintr-un modul.
     */
    public LectureResponse updateLectureInModule(String courseId, String moduleId,
                                                 String lectureId, UpdateLectureRequest req) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        Course course = getCourseOrThrow(cid, courseId);
        Module module = findModuleInList(course.getModules(), mid, moduleId);
        Lecture existing = findLectureInList(module.getLectures(), lid, lectureId);

        // Aplică actualizările pe obiectul din memorie
        if (req.getTitle() != null) existing.setTitle(req.getTitle());
        if (req.getVideoUrl() != null) existing.setVideoUrl(urlTokenizer.tokenizeVideoUrl(req.getVideoUrl()));
        if (req.getImageUrls() != null) existing.setImageUrls(urlTokenizer.tokenizeImageUrls(req.getImageUrls()));
        if (req.getOrder() != null) existing.setOrder(req.getOrder());
        if (req.getDurationSecs() != null) existing.setDurationSecs(req.getDurationSecs());

        // Salvează modulul actualizat
        courseDAO.replaceModule(cid, module);
        courseDAO.updateUpdatedAt(cid, new Date());
        return LectureResponse.fromModel(existing);
    }

    /**
     * Șterge o lectură dintr-un modul.
     */
    public void removeLectureFromModule(String courseId, String moduleId, String lectureId) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");
        ObjectId lid = toObjectId(lectureId, "Invalid lecture ID");

        Course course = getCourseOrThrow(cid, courseId);
        Module module = findModuleInList(course.getModules(), mid, moduleId);

        boolean removed = module.getLectures().removeIf(l -> lid.equals(l.getId()));
        if (!removed) throw new ResourceNotFoundException("Lecture not found in module: " + lectureId);

        courseDAO.replaceModule(cid, module);
        courseDAO.updateUpdatedAt(cid, new Date());
    }

    /**
     * Reordonează lecturile dintr-un modul (pentru drag & drop pe frontend).
     * orderedLectureIds = lista de ID-uri în noua ordine.
     */
    public List<LectureResponse> reorderLecturesInModule(String courseId, String moduleId,
                                                         List<String> orderedLectureIds) {
        ObjectId cid = toObjectId(courseId, "Invalid course ID");
        ObjectId mid = toObjectId(moduleId, "Invalid module ID");

        Course course = getCourseOrThrow(cid, courseId);
        Module module = findModuleInList(course.getModules(), mid, moduleId);

        List<Lecture> lectures = module.getLectures();
        if (orderedLectureIds.size() != lectures.size()) {
            throw new BadRequestException("Numărul de ID-uri nu corespunde cu numărul de lecturi din modul");
        }

        // Reordonare conform listei primite
        List<Lecture> reordered = new ArrayList<>();
        for (int i = 0; i < orderedLectureIds.size(); i++) {
            ObjectId lid = toObjectId(orderedLectureIds.get(i), "Invalid lecture ID");
            int order = i;
            Lecture found = lectures.stream()
                    .filter(l -> lid.equals(l.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lid));
            found.setOrder(order);
            reordered.add(found);
        }

        module.setLectures(reordered);
        courseDAO.replaceModule(cid, module);
        courseDAO.updateUpdatedAt(cid, new Date());

        return reordered.stream().map(LectureResponse::fromModel).collect(Collectors.toList());
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Lecture buildLecture(CreateLectureRequest req) {
        return Lecture.builder()
                .id(new ObjectId())
                .title(req.getTitle())
                .videoUrl(urlTokenizer.tokenizeVideoUrl(req.getVideoUrl()))
                .imageUrls(urlTokenizer.tokenizeImageUrls(req.getImageUrls()))
                .order(req.getOrder())
                .durationSecs(req.getDurationSecs())
                .publishedAt(new Date())
                .build();
    }

    private void applyUpdates(ObjectId courseId, ObjectId lectureId,
                              UpdateLectureRequest req, boolean inModule, Module module) {
        if (req.getTitle() != null)
            courseDAO.updateLectureField(courseId, lectureId, "title", req.getTitle());
        if (req.getVideoUrl() != null)
            courseDAO.updateLectureField(courseId, lectureId, "videoUrl",
                    urlTokenizer.tokenizeVideoUrl(req.getVideoUrl()));
        if (req.getImageUrls() != null)
            courseDAO.updateLectureField(courseId, lectureId, "imageUrls",
                    urlTokenizer.tokenizeImageUrls(req.getImageUrls()));
        if (req.getOrder() != null)
            courseDAO.updateLectureField(courseId, lectureId, "order", req.getOrder());
        if (req.getDurationSecs() != null)
            courseDAO.updateLectureField(courseId, lectureId, "durationSecs", req.getDurationSecs());
    }

    private Course getCourseOrThrow(ObjectId cid, String courseId) {
        Course course = courseDAO.findById(cid);
        if (course == null) throw new ResourceNotFoundException("Course not found: " + courseId);
        return course;
    }

    private Lecture findLectureInList(List<Lecture> lectures, ObjectId lid, String lectureId) {
        return lectures.stream()
                .filter(l -> lid.equals(l.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));
    }

    private Module findModuleInList(List<Module> modules, ObjectId mid, String moduleId) {
        return modules.stream()
                .filter(m -> mid.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));
    }

    private ObjectId toObjectId(String id, String errorMessage) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException(errorMessage + ": " + id);
        }
        return new ObjectId(id);
    }
}

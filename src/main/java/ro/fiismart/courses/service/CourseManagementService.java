package ro.fiismart.courses.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.common.repository.EnrollmentRepository;
import ro.fiismart.courses.dto.request.*;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;
import ro.fiismart.notification.service.NotificationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseManagementService {

    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;

    public CourseManagementService(CourseRepository courseRepository,
                                   MongoTemplate mongoTemplate,
                                   NotificationService notificationService,
                                   EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.mongoTemplate = mongoTemplate;
        this.notificationService = notificationService;
        this.enrollmentRepository = enrollmentRepository;
    }

    // ── COURSE CRUD ──────────────────────────────────────────────────────────

    public CourseResponse createCourse(CreateCourseRequest req) {
        Course course = Course.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .teacherId(req.getTeacherId())
                .status("draft")
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .thumbnailUrl(req.getThumbnailUrl())
                .language(req.getLanguage())
                .enrollmentCount(0)
                .avgRating(0.0)
                .hidden(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        return CourseResponse.fromModel(courseRepository.save(course));
    }

    public CourseResponse getCourseById(String id) {
        return CourseResponse.fromModel(
                courseRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Course", id)));
    }

    public List<CourseResponse> getCoursesByTeacherId(String teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(CourseResponse::fromModel)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getPublishedCourses() {
        return courseRepository.findByStatusAndHiddenFalse("published").stream()
                .map(CourseResponse::fromModel)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::fromModel)
                .collect(Collectors.toList());
    }

    public CourseResponse updateCourse(String id, UpdateCourseRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        if (req.getTitle() != null) course.setTitle(req.getTitle());
        if (req.getDescription() != null) course.setDescription(req.getDescription());
        if (req.getTags() != null) course.setTags(req.getTags());
        if (req.getThumbnailUrl() != null) course.setThumbnailUrl(req.getThumbnailUrl());
        if (req.getLanguage() != null) course.setLanguage(req.getLanguage());
        course.setUpdatedAt(new Date());

        return CourseResponse.fromModel(courseRepository.save(course));
    }

    public CourseResponse publishCourse(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        course.setStatus("published");
        course.setUpdatedAt(new Date());
        CourseResponse response = CourseResponse.fromModel(courseRepository.save(course));

        // Notifică studenții care au început cursul că a fost actualizat/publicat
        try {
            String msg = String.format("Cursul \"%s\" a fost actualizat cu conținut nou", course.getTitle());
            enrollmentRepository.findByCourseIdAndOverallProgressGreaterThan(id, 0)
                    .forEach(e -> notificationService.createCourseUpdateNotification(
                            e.getStudentId(), course.getTitle(), id, msg));
        } catch (Exception ignored) {}

        return response;
    }

    public CourseResponse draftCourse(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        course.setStatus("draft");
        course.setUpdatedAt(new Date());
        return CourseResponse.fromModel(courseRepository.save(course));
    }

    public void deleteCourse(String id) {
        if (!courseRepository.existsById(id)) throw new ResourceNotFoundException("Course", id);
        courseRepository.deleteById(id);
    }

    // ── MODULE MANAGEMENT ─────────────────────────────────────────────────────

    public List<ModuleResponse> getModules(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return course.getModules().stream().map(ModuleResponse::fromModel).collect(Collectors.toList());
    }

    public ModuleResponse addModule(String courseId, CreateModuleRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        int nextOrder = course.getModules() != null ? course.getModules().size() : 0;
        CourseModule module = CourseModule.builder()
                .id(UUID.randomUUID().toString())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(nextOrder)
                .lectures(new ArrayList<>())
                .build();

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId)),
                new Update().push("modules", module).set("updatedAt", new Date()),
                Course.class
        );
        return ModuleResponse.fromModel(module);
    }

    public ModuleResponse updateModule(String courseId, String moduleId, CreateModuleRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        CourseModule module = findModule(course, moduleId);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update()
                        .set("modules.$.title", req.getTitle())
                        .set("modules.$.description", req.getDescription())
                        .set("updatedAt", new Date()),
                Course.class
        );

        module.setTitle(req.getTitle());
        module.setDescription(req.getDescription());
        return ModuleResponse.fromModel(module);
    }

    public void deleteModule(String courseId, String moduleId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId)),
                new Update().pull("modules", new org.bson.Document("id", moduleId))
                        .set("updatedAt", new Date()),
                Course.class
        );
    }

    public List<ModuleResponse> reorderModules(String courseId, List<String> orderedIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        Map<String, CourseModule> moduleMap = course.getModules().stream()
                .collect(Collectors.toMap(CourseModule::getId, m -> m));

        List<CourseModule> reordered = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            CourseModule m = moduleMap.get(orderedIds.get(i));
            if (m != null) {
                m.setOrder(i);
                reordered.add(m);
            }
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId)),
                new Update().set("modules", reordered).set("updatedAt", new Date()),
                Course.class
        );

        return reordered.stream().map(ModuleResponse::fromModel).collect(Collectors.toList());
    }

    // ── LECTURE MANAGEMENT ────────────────────────────────────────────────────

    public LectureResponse addLectureToModule(String courseId, String moduleId, CreateLectureRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        CourseModule module = findModule(course, moduleId);
        int nextOrder = module.getLectures() != null ? module.getLectures().size() : 0;

        Lecture lecture = Lecture.builder()
                .id(UUID.randomUUID().toString())
                .moduleId(moduleId)
                .title(req.getTitle())
                .videoUrl(req.getVideoUrl())
                .imageUrls(req.getImageUrls() != null ? req.getImageUrls() : new ArrayList<>())
                .order(nextOrder)
                .durationSecs(req.getDurationSecs())
                .publishedAt(new Date())
                .build();

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update().push("modules.$.lectures", lecture).set("updatedAt", new Date()),
                Course.class
        );

        // Notifică studenții care au început cursul că a fost adăugată o lecție nouă
        try {
            String msg = String.format("Lecție nouă: \"%s\" a fost adăugată în cursul \"%s\"",
                    lecture.getTitle(), course.getTitle());
            enrollmentRepository.findByCourseIdAndOverallProgressGreaterThan(courseId, 0)
                    .forEach(e -> notificationService.createCourseUpdateNotification(
                            e.getStudentId(), course.getTitle(), courseId, msg));
        } catch (Exception ignored) {}

        return LectureResponse.fromModel(lecture);
    }

    public LectureResponse updateLectureInModule(String courseId, String moduleId, String lectureId,
                                                  UpdateLectureRequest req) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // Use arrayFilters for nested array update
        org.bson.Document filter = new org.bson.Document("elem.id", lectureId);
        if (req.getTitle() != null) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                    new Update().set("modules.$.lectures.$[elem].title", req.getTitle()),
                    Course.class
            );
        }

        Course refreshed = courseRepository.findById(courseId).orElseThrow();
        CourseModule module = findModule(refreshed, moduleId);
        Lecture lecture = module.getLectures().stream()
                .filter(l -> lectureId.equals(l.getId())).findFirst().orElse(null);

        if (lecture != null && req.getTitle() != null) lecture.setTitle(req.getTitle());
        if (lecture != null && req.getVideoUrl() != null) lecture.setVideoUrl(req.getVideoUrl());

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update().set("modules.$", module).set("updatedAt", new Date()),
                Course.class
        );

        return lecture != null ? LectureResponse.fromModel(lecture) : null;
    }

    public void removeLectureFromModule(String courseId, String moduleId, String lectureId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        Course course = courseRepository.findById(courseId).orElseThrow();
        CourseModule module = findModule(course, moduleId);
        module.getLectures().removeIf(l -> lectureId.equals(l.getId()));

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update().set("modules.$", module).set("updatedAt", new Date()),
                Course.class
        );
    }

    private CourseModule findModule(Course course, String moduleId) {
        return course.getModules().stream()
                .filter(m -> moduleId.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));
    }
}

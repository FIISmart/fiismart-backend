package ro.fiismart.courses.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ro.fiismart.common.exception.ForbiddenException;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.courses.dto.request.*;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseManagementService {

    private final CourseRepository courseRepository;
    private final MongoTemplate mongoTemplate;

    public CourseManagementService(CourseRepository courseRepository, MongoTemplate mongoTemplate) {
        this.courseRepository = courseRepository;
        this.mongoTemplate = mongoTemplate;
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
        return CourseResponse.fromModel(courseRepository.save(course));
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

    /**
     * Hard ownership check used by AI-driven mutations: the caller
     * (chat tool handler) verifies the user owns the course before any
     * write. Existing controller-driven CRUD doesn't rely on this — it
     * trusts the caller to populate teacherId — but AI tool calls are
     * authority-spoofable by definition, so we enforce server-side.
     *
     * @throws ResourceNotFoundException course does not exist
     * @throws ForbiddenException        course exists but {@code userId} is not its teacher
     */
    public void assertOwner(String courseId, String userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        if (userId == null || !userId.equals(course.getTeacherId())) {
            throw new ForbiddenException("Not your course");
        }
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
                .type(resolveLectureType(req.getType(), req.getContent(), req.getVideoUrl(), req.getPdfUrl()))
                .content(resolveLectureContent(req.getContent(), req.getVideoUrl(), req.getPdfUrl()))
                .videoUrl(resolveVideoUrl(req.getType(), req.getContent(), req.getVideoUrl()))
                .pdfUrl(resolvePdfUrl(req.getType(), req.getContent(), req.getPdfUrl()))
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
        return LectureResponse.fromModel(lecture);
    }

    public LectureResponse updateLectureInModule(String courseId, String moduleId, String lectureId,
                                                  UpdateLectureRequest req) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // We used to do a separate `$[elem].title` update here with an
        // orphaned arrayFilter Document — but the filter was never wired
        // into the Update, so MongoDB returned
        //   "No array filter found for identifier 'elem'"
        // and the whole call crashed. The block below already loads the
        // module, mutates the target lecture in-memory (including title),
        // and writes `modules.$ = module` — covering every field in one
        // shot. The buggy block is removed.
        Course refreshed = courseRepository.findById(courseId).orElseThrow();
        CourseModule module = findModule(refreshed, moduleId);
        Lecture lecture = module.getLectures().stream()
                .filter(l -> lectureId.equals(l.getId())).findFirst().orElse(null);

        if (lecture != null && req.getTitle() != null) lecture.setTitle(req.getTitle());
        if (lecture != null) {
            String nextContent = resolveLectureContent(req.getContent(), req.getVideoUrl(), req.getPdfUrl());
            String nextType = resolveLectureType(req.getType(), nextContent, req.getVideoUrl(), req.getPdfUrl());

            if (req.getType() != null || !nextContent.isBlank()) lecture.setType(nextType);
            if (!nextContent.isBlank()) lecture.setContent(nextContent);
            if (req.getVideoUrl() != null || "video".equals(nextType)) {
                lecture.setVideoUrl(resolveVideoUrl(nextType, nextContent, req.getVideoUrl()));
            }
            if (req.getPdfUrl() != null || "pdf".equals(nextType)) {
                lecture.setPdfUrl(resolvePdfUrl(nextType, nextContent, req.getPdfUrl()));
            }
            if (req.getType() != null && !"pdf".equals(nextType)) {
                lecture.setPdfUrl(null);
            }
            if (req.getImageUrls() != null) lecture.setImageUrls(req.getImageUrls());
            if (req.getDurationSecs() > 0) lecture.setDurationSecs(req.getDurationSecs());
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update().set("modules.$", module).set("updatedAt", new Date()),
                Course.class
        );

        return lecture != null ? LectureResponse.fromModel(lecture) : null;
    }

    /**
     * Reorder the lectures inside a module to match {@code orderedIds},
     * setting each lecture's {@code order} field to its index in the
     * list. Mirrors {@link #reorderModules} at the lecture level — IDs
     * not present in the module are silently skipped (rather than
     * thrown) so a transient FE/AI disagreement doesn't corrupt the
     * whole module. The caller is expected to have just listed the
     * module's lectures, so a stale id is a one-shot anomaly.
     */
    public List<LectureResponse> reorderLecturesInModule(String courseId,
                                                         String moduleId,
                                                         List<String> orderedIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        CourseModule module = findModule(course, moduleId);
        if (module.getLectures() == null || module.getLectures().isEmpty()) {
            return List.of();
        }

        Map<String, Lecture> lectureMap = module.getLectures().stream()
                .collect(Collectors.toMap(Lecture::getId, l -> l));

        List<Lecture> reordered = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Lecture l = lectureMap.get(orderedIds.get(i));
            if (l != null) {
                l.setOrder(i);
                reordered.add(l);
            }
        }
        module.setLectures(reordered);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(courseId).and("modules.id").is(moduleId)),
                new Update().set("modules.$.lectures", reordered).set("updatedAt", new Date()),
                Course.class
        );

        return reordered.stream().map(LectureResponse::fromModel).collect(Collectors.toList());
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

    private String resolveLectureContent(String content, String videoUrl, String pdfUrl) {
        if (content != null) return content;
        if (pdfUrl != null) return pdfUrl;
        if (videoUrl != null) return videoUrl;
        return "";
    }

    private String resolveLectureType(String type, String content, String videoUrl, String pdfUrl) {
        if (type != null && !type.isBlank()) return type;
        if (pdfUrl != null && !pdfUrl.isBlank()) return "pdf";

        String source = resolveLectureContent(content, videoUrl, pdfUrl).toLowerCase(Locale.ROOT);
        if (source.endsWith(".pdf")) return "pdf";
        if (source.endsWith(".md") || source.endsWith(".markdown") || (!source.startsWith("http") && !source.isBlank())) {
            return "markdown";
        }
        return "video";
    }

    private String resolveVideoUrl(String type, String content, String videoUrl) {
        String resolvedType = resolveLectureType(type, content, videoUrl, null);
        if (!"video".equals(resolvedType)) return videoUrl;
        if (videoUrl != null) return videoUrl;
        return content;
    }

    private String resolvePdfUrl(String type, String content, String pdfUrl) {
        String resolvedType = resolveLectureType(type, content, null, pdfUrl);
        if (!"pdf".equals(resolvedType)) return pdfUrl;
        if (pdfUrl != null) return pdfUrl;
        return content;
    }
}

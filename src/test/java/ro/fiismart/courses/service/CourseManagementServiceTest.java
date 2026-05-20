package ro.fiismart.courses.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.common.model.Course;
import ro.fiismart.common.model.CourseModule;
import ro.fiismart.common.model.Lecture;
import ro.fiismart.common.repository.CourseRepository;
import ro.fiismart.courses.dto.request.*;
import ro.fiismart.courses.dto.response.CourseResponse;
import ro.fiismart.courses.dto.response.LectureResponse;
import ro.fiismart.courses.dto.response.ModuleResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseManagementServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CourseManagementService courseManagementService;

    private Course sampleCourse;
    private CourseModule sampleModule;
    private Lecture sampleLecture;

    @BeforeEach
    void setUp() {
        sampleLecture = Lecture.builder()
                .id("lec1")
                .moduleId("mod1")
                .title("Lecție 1")
                .type("video")
                .videoUrl("https://youtube.com/watch?v=abc")
                .order(0)
                .build();

        sampleModule = CourseModule.builder()
                .id("mod1")
                .title("Modul 1")
                .description("Introducere")
                .order(0)
                .lectures(new ArrayList<>(List.of(sampleLecture)))
                .build();

        sampleCourse = Course.builder()
                .id("course1")
                .title("Matematică")
                .description("Curs de matematică")
                .teacherId("teacher1")
                .status("draft")
                .hidden(false)
                .modules(new ArrayList<>(List.of(sampleModule)))
                .tags(new ArrayList<>())
                .build();
    }

    // ── createCourse ──────────────────────────────────────────────────────────

    @Test
    void createCourse_savesAndReturnsResponse() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("Fizică");
        req.setDescription("Curs de fizică");
        req.setTeacherId("teacher1");
        req.setTags(List.of("știință"));

        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse result = courseManagementService.createCourse(req);

        assertThat(result.getId()).isEqualTo("course1");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_nullTags_usesEmpty() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("Chimie");
        req.setTags(null);

        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse result = courseManagementService.createCourse(req);

        assertThat(result).isNotNull();
    }

    // ── getCourseById ─────────────────────────────────────────────────────────

    @Test
    void getCourseById_found_returnsResponse() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        CourseResponse result = courseManagementService.getCourseById("course1");

        assertThat(result.getId()).isEqualTo("course1");
    }

    @Test
    void getCourseById_notFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseManagementService.getCourseById("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getCoursesByTeacherId ─────────────────────────────────────────────────

    @Test
    void getCoursesByTeacherId_returnsMappedList() {
        when(courseRepository.findByTeacherId("teacher1")).thenReturn(List.of(sampleCourse));

        List<CourseResponse> result = courseManagementService.getCoursesByTeacherId("teacher1");

        assertThat(result).hasSize(1);
    }

    // ── getPublishedCourses ───────────────────────────────────────────────────

    @Test
    void getPublishedCourses_returnsOnlyPublished() {
        when(courseRepository.findByStatusAndHiddenFalse("published")).thenReturn(List.of(sampleCourse));

        List<CourseResponse> result = courseManagementService.getPublishedCourses();

        assertThat(result).hasSize(1);
    }

    // ── getAllCourses ─────────────────────────────────────────────────────────

    @Test
    void getAllCourses_returnsAll() {
        when(courseRepository.findAll()).thenReturn(List.of(sampleCourse));

        List<CourseResponse> result = courseManagementService.getAllCourses();

        assertThat(result).hasSize(1);
    }

    // ── updateCourse ──────────────────────────────────────────────────────────

    @Test
    void updateCourse_updatesFields() {
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("Matematică Avansată");
        req.setDescription("Curs avansat");
        req.setTags(List.of("avansat"));
        req.setThumbnailUrl("https://img.com/thumb.jpg");
        req.setLanguage("ro");

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse result = courseManagementService.updateCourse("course1", req);

        assertThat(sampleCourse.getTitle()).isEqualTo("Matematică Avansată");
        assertThat(sampleCourse.getDescription()).isEqualTo("Curs avansat");
    }

    @Test
    void updateCourse_notFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseManagementService.updateCourse("noId", new UpdateCourseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── publishCourse / draftCourse ───────────────────────────────────────────

    @Test
    void publishCourse_setsStatusToPublished() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        courseManagementService.publishCourse("course1");

        assertThat(sampleCourse.getStatus()).isEqualTo("published");
    }

    @Test
    void draftCourse_setsStatusToDraft() {
        sampleCourse.setStatus("published");
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        courseManagementService.draftCourse("course1");

        assertThat(sampleCourse.getStatus()).isEqualTo("draft");
    }

    @Test
    void publishCourse_notFound_throws() {
        when(courseRepository.findById("noId")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseManagementService.publishCourse("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteCourse ──────────────────────────────────────────────────────────

    @Test
    void deleteCourse_deletesCourse() {
        when(courseRepository.existsById("course1")).thenReturn(true);

        courseManagementService.deleteCourse("course1");

        verify(courseRepository).deleteById("course1");
    }

    @Test
    void deleteCourse_notFound_throws() {
        when(courseRepository.existsById("noId")).thenReturn(false);

        assertThatThrownBy(() -> courseManagementService.deleteCourse("noId"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getModules ────────────────────────────────────────────────────────────

    @Test
    void getModules_returnsMappedList() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        List<ModuleResponse> result = courseManagementService.getModules("course1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("mod1");
    }

    // ── addModule ─────────────────────────────────────────────────────────────

    @Test
    void addModule_callsMongoTemplate() {
        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle("Modul 2");
        req.setDescription("Al doilea modul");

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        ModuleResponse result = courseManagementService.addModule("course1", req);

        assertThat(result.getTitle()).isEqualTo("Modul 2");
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    // ── updateModule ──────────────────────────────────────────────────────────

    @Test
    void updateModule_updatesAndReturns() {
        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle("Modul 1 Actualizat");
        req.setDescription("Descriere actualizată");

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        ModuleResponse result = courseManagementService.updateModule("course1", "mod1", req);

        assertThat(result.getTitle()).isEqualTo("Modul 1 Actualizat");
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    @Test
    void updateModule_moduleNotFound_throws() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        assertThatThrownBy(() -> courseManagementService.updateModule("course1", "noMod", new CreateModuleRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteModule ──────────────────────────────────────────────────────────

    @Test
    void deleteModule_callsMongoTemplate() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        courseManagementService.deleteModule("course1", "mod1");

        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    // ── reorderModules ────────────────────────────────────────────────────────

    @Test
    void reorderModules_updatesOrderAndReturns() {
        CourseModule mod2 = CourseModule.builder().id("mod2").title("Modul 2").order(0)
                .lectures(new ArrayList<>()).build();
        sampleCourse.getModules().add(mod2);

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        List<ModuleResponse> result = courseManagementService.reorderModules("course1", List.of("mod2", "mod1"));

        assertThat(result).hasSize(2);
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    // ── addLectureToModule ────────────────────────────────────────────────────

    @Test
    void addLectureToModule_videoUrl_setsTypeToVideo() {
        CreateLectureRequest req = new CreateLectureRequest();
        req.setTitle("Lecție Video");
        req.setVideoUrl("https://youtube.com/watch?v=xyz");
        req.setType("video");

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        LectureResponse result = courseManagementService.addLectureToModule("course1", "mod1", req);

        assertThat(result.getTitle()).isEqualTo("Lecție Video");
        assertThat(result.getType()).isEqualTo("video");
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }

    @Test
    void addLectureToModule_pdfUrl_setsTypeToPdf() {
        CreateLectureRequest req = new CreateLectureRequest();
        req.setTitle("Lecție PDF");
        req.setPdfUrl("https://storage.com/doc.pdf");
        req.setType("pdf");

        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        LectureResponse result = courseManagementService.addLectureToModule("course1", "mod1", req);

        assertThat(result.getType()).isEqualTo("pdf");
    }

    @Test
    void addLectureToModule_moduleNotFound_throws() {
        when(courseRepository.findById("course1")).thenReturn(Optional.of(sampleCourse));

        assertThatThrownBy(() -> courseManagementService.addLectureToModule("course1", "noMod", new CreateLectureRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateLectureInModule ─────────────────────────────────────────────────

    @Test
    void updateLectureInModule_updatesTitle() {
        UpdateLectureRequest req = new UpdateLectureRequest();
        req.setTitle("Titlu actualizat");

        when(courseRepository.findById("course1"))
                .thenReturn(Optional.of(sampleCourse))
                .thenReturn(Optional.of(sampleCourse));

        LectureResponse result = courseManagementService.updateLectureInModule("course1", "mod1", "lec1", req);

        assertThat(result).isNotNull();
    }

    // ── removeLectureFromModule ───────────────────────────────────────────────

    @Test
    void removeLectureFromModule_removesLecture() {
        when(courseRepository.findById("course1"))
                .thenReturn(Optional.of(sampleCourse))
                .thenReturn(Optional.of(sampleCourse));

        courseManagementService.removeLectureFromModule("course1", "mod1", "lec1");

        assertThat(sampleModule.getLectures()).isEmpty();
        verify(mongoTemplate).updateFirst(any(), any(), eq(Course.class));
    }
}

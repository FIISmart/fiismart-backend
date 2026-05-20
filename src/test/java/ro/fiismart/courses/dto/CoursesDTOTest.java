package ro.fiismart.courses.dto;

import org.junit.jupiter.api.Test;
import ro.fiismart.courses.dto.request.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoursesDTOTest {

    @Test
    void createCourseRequest_gettersSetters() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setTitle("Matematică");
        req.setDescription("Desc");
        req.setTeacherId("t1");
        req.setTags(List.of("math"));
        req.setThumbnailUrl("thumb.jpg");
        req.setLanguage("ro");
        assertThat(req.getTitle()).isEqualTo("Matematică");
        assertThat(req.getDescription()).isEqualTo("Desc");
        assertThat(req.getTags()).containsExactly("math");
    }

    @Test
    void updateCourseRequest_gettersSetters() {
        UpdateCourseRequest req = new UpdateCourseRequest();
        req.setTitle("Nou");
        req.setDescription("Desc Nou");
        req.setTags(List.of("science"));
        req.setThumbnailUrl("new.jpg");
        req.setLanguage("en");
        assertThat(req.getTitle()).isEqualTo("Nou");
        assertThat(req.getLanguage()).isEqualTo("en");
    }

    @Test
    void createModuleRequest_gettersSetters() {
        CreateModuleRequest req = new CreateModuleRequest();
        req.setTitle("Modul 1");
        req.setDescription("Desc");
        assertThat(req.getTitle()).isEqualTo("Modul 1");
        assertThat(req.getDescription()).isEqualTo("Desc");
    }

    @Test
    void createLectureRequest_gettersSetters() {
        CreateLectureRequest req = new CreateLectureRequest();
        req.setTitle("Lecție 1");
        req.setType("video");
        req.setVideoUrl("http://v");
        req.setDurationSecs(120);
        assertThat(req.getTitle()).isEqualTo("Lecție 1");
        assertThat(req.getType()).isEqualTo("video");
        assertThat(req.getDurationSecs()).isEqualTo(120);
    }

    @Test
    void updateLectureRequest_gettersSetters() {
        UpdateLectureRequest req = new UpdateLectureRequest();
        req.setTitle("Updated");
        req.setType("pdf");
        req.setPdfUrl("doc.pdf");
        req.setContent("Content");
        assertThat(req.getTitle()).isEqualTo("Updated");
        assertThat(req.getPdfUrl()).isEqualTo("doc.pdf");
    }
}

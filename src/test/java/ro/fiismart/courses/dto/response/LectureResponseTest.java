package ro.fiismart.courses.dto.response;

import org.junit.jupiter.api.Test;
import ro.fiismart.common.model.Lecture;

import static org.assertj.core.api.Assertions.assertThat;

class LectureResponseTest {

    @Test
    void fromModel_null_returnsNull() {
        assertThat(LectureResponse.fromModel(null)).isNull();
    }

    @Test
    void fromModel_videoType_explicit_preservesType() {
        Lecture lec = Lecture.builder()
                .id("l1").moduleId("m1").title("Video").type("video")
                .videoUrl("https://yt.com/v=abc").order(0).durationSecs(120).build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getId()).isEqualTo("l1");
        assertThat(r.getType()).isEqualTo("video");
        assertThat(r.getVideoUrl()).isEqualTo("https://yt.com/v=abc");
    }

    @Test
    void fromModel_pdfType_explicit_preservesType() {
        Lecture lec = Lecture.builder()
                .id("l2").type("pdf").pdfUrl("doc.pdf").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("pdf");
        assertThat(r.getPdfUrl()).isEqualTo("doc.pdf");
    }

    @Test
    void fromModel_noType_pdfUrl_infersPdf() {
        Lecture lec = Lecture.builder().id("l3").pdfUrl("file.pdf").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("pdf");
        assertThat(r.getPdfUrl()).isEqualTo("file.pdf");
    }

    @Test
    void fromModel_noType_contentEndsPdf_infersPdf() {
        Lecture lec = Lecture.builder().id("l4").content("document.pdf").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("pdf");
    }

    @Test
    void fromModel_noType_markdownContent_infersMarkdown() {
        Lecture lec = Lecture.builder().id("l5").content("# Titlu\nConținut markdown").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("markdown");
    }

    @Test
    void fromModel_noType_contentEndsMd_infersMarkdown() {
        Lecture lec = Lecture.builder().id("l6").content("lesson.md").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("markdown");
    }

    @Test
    void fromModel_noType_httpContent_infersVideo() {
        Lecture lec = Lecture.builder().id("l7").videoUrl("https://youtube.com/abc").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getType()).isEqualTo("video");
    }

    @Test
    void fromModel_contentNotNull_usedAsContent() {
        Lecture lec = Lecture.builder().id("l8").type("markdown").content("## H2").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getContent()).isEqualTo("## H2");
    }

    @Test
    void fromModel_noPdfUrl_pdfTypeFallsBackFromContent() {
        Lecture lec = Lecture.builder().id("l9").content("manual.pdf").build();

        LectureResponse r = LectureResponse.fromModel(lec);

        assertThat(r.getPdfUrl()).isEqualTo("manual.pdf");
    }
}

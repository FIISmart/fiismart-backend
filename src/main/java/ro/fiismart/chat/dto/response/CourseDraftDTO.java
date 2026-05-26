package ro.fiismart.chat.dto.response;

import java.util.List;

/**
 * Skeleton course draft produced by the {@code createCourseDraft} tool.
 *
 * <p>Intentionally minimal — the chatbot only produces a scaffold (title,
 * description, module/lecture <em>names</em>). Generating actual lecture
 * content is out of scope; the FE will open the course-builder pre-filled
 * with this shape and the professor fleshes it out.
 */
public record CourseDraftDTO(
        String title,
        String description,
        List<ModuleDraft> modules
) {

    public record ModuleDraft(
            String title,
            List<String> lectureTitles
    ) {
    }
}

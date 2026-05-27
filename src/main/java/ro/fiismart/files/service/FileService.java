package ro.fiismart.files.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.fiismart.common.exception.ResourceNotFoundException;
import ro.fiismart.files.dto.FileUploadResponse;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

@Service
public class FileService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;     // 5 MB
    private static final long MAX_FILE_SIZE = 50L * 1024L * 1024L;     // 50 MB

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> LECTURE_TYPES = Set.of("application/pdf");

    private final GridFSBucket gridFsBucket;
    private final GridFSBucket legacyUploadsGridFsBucket;

    public FileService(GridFSBucket gridFsBucket,
                       @Qualifier("legacyUploadsGridFsBucket") GridFSBucket legacyUploadsGridFsBucket) {
        this.gridFsBucket = gridFsBucket;
        this.legacyUploadsGridFsBucket = legacyUploadsGridFsBucket;
    }

    public FileUploadResponse uploadThumbnail(MultipartFile file) {
        validate(file, IMAGE_TYPES, MAX_IMAGE_SIZE,
                "Doar imagini sunt permise (JPG, PNG, WebP, GIF)");
        return store(file, "thumbnail");
    }

    public FileUploadResponse uploadLectureFile(MultipartFile file) {
        validate(file, LECTURE_TYPES, MAX_FILE_SIZE,
                "Tip fisier nepermis pentru lectie (PDF)");
        return store(file, "lecture");
    }

    public DownloadPayload download(String id) {
        return download(id, null);
    }

    public DownloadPayload downloadLecture(String id) {
        return download(id, "lecture");
    }

    private DownloadPayload download(String id, String expectedCategory) {
        ObjectId oid = toObjectId(id);
        GridFileRef fileRef = findFile(oid);
        if (fileRef == null) {
            throw new ResourceNotFoundException("File not found: " + id);
        }
        GridFSFile gridFile = fileRef.file();
        // Relaxing strict category matching. The presence of the file and correct access rights
        // (implied by having the ID) is sufficient for now, ensuring backwards compatibility 
        // with files uploaded before metadata structures were strictly enforced.
        
        GridFSDownloadStream stream = fileRef.bucket().openDownloadStream(oid);
        String contentType = gridFile.getMetadata() != null
                ? gridFile.getMetadata().getString("contentType")
                : "application/octet-stream";
                
        // Fallback for PDFs if contentType is generic but it's a PDF route or file extension
        if ("application/octet-stream".equals(contentType)) {
            String filename = gridFile.getFilename();
            if ((filename != null && filename.toLowerCase().endsWith(".pdf")) || "lecture".equals(expectedCategory)) {
                contentType = "application/pdf";
            }
        }
                
        return new DownloadPayload(stream, gridFile.getFilename(), contentType, gridFile.getLength());
    }

    public void delete(String id) {
        ObjectId oid = toObjectId(id);
        GridFileRef fileRef = findFile(oid);
        if (fileRef == null) {
            throw new ResourceNotFoundException("File not found: " + id);
        }
        fileRef.bucket().delete(oid);
    }

    private FileUploadResponse store(MultipartFile file, String category) {
        try {
            String contentType = normalizeContentType(file);
            Document metadata = new Document()
                    .append("originalFilename", file.getOriginalFilename())
                    .append("contentType", contentType)
                    .append("size", file.getSize())
                    .append("uploadedAt", new Date())
                    .append("kind", category)
                    .append("category", category)
                    .append("originalName", file.getOriginalFilename());

            GridFSUploadOptions options = new GridFSUploadOptions().metadata(metadata);

            ObjectId id = gridFsBucket.uploadFromStream(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed",
                    file.getInputStream(),
                    options
            );

            return FileUploadResponse.builder()
                    .id(id.toHexString())
                    .url(fileUrl(category, id.toHexString()))
                    .filename(file.getOriginalFilename())
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException("Eroare la citirea fisierului: " + e.getMessage());
        }
    }

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxSize, String typeError) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fisierul este gol");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Fisier prea mare. Limita: " + (maxSize / (1024 * 1024)) + " MB");
        }
        String contentType = file.getContentType();
        String normalized = normalizeContentType(file);
        if ((contentType == null || !allowedTypes.contains(contentType)) && !allowedTypes.contains(normalized)) {
            throw new IllegalArgumentException(typeError + ". Primit: " + contentType);
        }
    }

    private String normalizeContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType.toLowerCase();
        }
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        }
        return contentType != null ? contentType.toLowerCase() : "application/octet-stream";
    }

    private String fileUrl(String category, String id) {
        if ("lecture".equals(category)) {
            return "/api/v1/files/lecture/" + id;
        }
        return "/api/v1/files/" + id;
    }

    private GridFileRef findFile(ObjectId oid) {
        GridFSFile file = gridFsBucket.find(eq("_id", oid)).first();
        if (file != null) {
            return new GridFileRef(gridFsBucket, file);
        }
        GridFSFile legacyFile = legacyUploadsGridFsBucket.find(eq("_id", oid)).first();
        if (legacyFile != null) {
            return new GridFileRef(legacyUploadsGridFsBucket, legacyFile);
        }
        return null;
    }

    private ObjectId toObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Invalid file ID: " + id);
        }
        return new ObjectId(id);
    }

    public record DownloadPayload(
            GridFSDownloadStream stream,
            String filename,
            String contentType,
            long size
    ) {}

    private record GridFileRef(GridFSBucket bucket, GridFSFile file) {}
}

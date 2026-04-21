package com.fiismart.backend.course.service;

import com.fiismart.backend.course.dto.response.FileUploadResponse;
import com.fiismart.backend.course.exception.BadRequestException;
import com.fiismart.backend.course.exception.ResourceNotFoundException;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

@Service
public class FileService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;      // 5 MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;      // 50 MB

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain",
            "text/markdown"
    );

    private final GridFSBucket gridFsBucket;

    public FileService(GridFSBucket gridFsBucket) {
        this.gridFsBucket = gridFsBucket;
    }

    public FileUploadResponse uploadThumbnail(MultipartFile file) {
        validate(file, IMAGE_TYPES, MAX_IMAGE_SIZE, "Doar imagini sunt permise (JPG, PNG, WebP, GIF)");
        return store(file, "thumbnail");
    }

    public FileUploadResponse uploadLectureFile(MultipartFile file) {
        validate(file, DOCUMENT_TYPES, MAX_FILE_SIZE, "Tip fisier nepermis pentru lectie (PDF, DOC, DOCX, ZIP, TXT, MD)");
        return store(file, "lecture");
    }

    public DownloadPayload download(String id) {
        ObjectId oid = toObjectId(id);
        GridFSFile gridFile = gridFsBucket.find(eq("_id", oid)).first();
        if (gridFile == null) {
            throw new ResourceNotFoundException("File not found: " + id);
        }
        GridFSDownloadStream stream = gridFsBucket.openDownloadStream(oid);
        String contentType = gridFile.getMetadata() != null
                ? gridFile.getMetadata().getString("contentType")
                : "application/octet-stream";
        return new DownloadPayload(stream, gridFile.getFilename(), contentType, gridFile.getLength());
    }

    public void delete(String id) {
        ObjectId oid = toObjectId(id);
        if (gridFsBucket.find(eq("_id", oid)).first() == null) {
            throw new ResourceNotFoundException("File not found: " + id);
        }
        gridFsBucket.delete(oid);
    }

    // ── internals ─────────────────────────────────────────────────

    private FileUploadResponse store(MultipartFile file, String category) {
        try {
            Document metadata = new Document()
                    .append("contentType", file.getContentType())
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
                    .url("/api/files/" + id.toHexString())
                    .filename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Eroare la citirea fisierului: " + e.getMessage());
        }
    }

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxSize, String typeError) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fisierul este gol");
        }
        if (file.getSize() > maxSize) {
            throw new BadRequestException("Fisier prea mare. Limita: " + (maxSize / (1024 * 1024)) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException(typeError + ". Primit: " + contentType);
        }
    }

    private ObjectId toObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("Invalid file ID: " + id);
        }
        return new ObjectId(id);
    }

    public record DownloadPayload(
            GridFSDownloadStream stream,
            String filename,
            String contentType,
            long size
    ) {}
}

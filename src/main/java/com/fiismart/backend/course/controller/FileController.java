package com.fiismart.backend.course.controller;

import com.fiismart.backend.course.dto.response.FileUploadResponse;
import com.fiismart.backend.course.service.FileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /** POST /api/files/thumbnail — upload cover image for a course */
    @PostMapping(value = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        FileUploadResponse res = fileService.uploadThumbnail(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** POST /api/files/lecture — upload lecture file (PDF/DOC/etc.) */
    @PostMapping(value = "/lecture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadLectureFile(@RequestParam("file") MultipartFile file) {
        FileUploadResponse res = fileService.uploadLectureFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** GET /api/files/{id} — serves the stored file with its original content type */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) {
        FileService.DownloadPayload payload = fileService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.filename() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.size())
                .body(new InputStreamResource(payload.stream()));
    }

    /** DELETE /api/files/{id} — remove a file */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

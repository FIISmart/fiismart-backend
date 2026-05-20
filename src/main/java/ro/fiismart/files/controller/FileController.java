package ro.fiismart.files.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.fiismart.files.dto.FileUploadResponse;
import ro.fiismart.files.service.FileService;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<FileUploadResponse> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadThumbnail(file));
    }

    @PostMapping(value = "/lecture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<FileUploadResponse> uploadLectureFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadLectureFile(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) {
        FileService.DownloadPayload payload = fileService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.filename() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.size())
                .body(new InputStreamResource(payload.stream()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

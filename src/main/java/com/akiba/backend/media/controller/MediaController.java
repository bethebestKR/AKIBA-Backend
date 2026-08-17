package com.akiba.backend.media.controller;

import com.akiba.backend.media.domain.MediaFile;
import com.akiba.backend.media.dto.response.MediaUploadResponse;
import com.akiba.backend.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.upload(file));
    }

    @GetMapping("/files/{mediaId}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long mediaId) {
        MediaFile mediaFile = mediaService.getMedia(mediaId);
        byte[] imageBytes = mediaService.downloadFromS3(mediaId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (mediaFile.getContentType() != null && !mediaFile.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(mediaFile.getContentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + mediaFile.getOriginalFilename() + "\"")
                .body(imageBytes);
    }
}
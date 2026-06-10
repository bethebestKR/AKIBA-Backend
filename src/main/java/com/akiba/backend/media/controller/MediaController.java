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
    public ResponseEntity<MediaUploadResponse> getFile(@PathVariable Long mediaId) {
        MediaFile mediaFile = mediaService.getMedia(mediaId);
        return ResponseEntity.ok(MediaUploadResponse.builder()
                .mediaId(mediaFile.getMediaId())
                .url(mediaFile.getUrl())
                .originalFilename(mediaFile.getOriginalFilename())
                .contentType(mediaFile.getContentType())
                .fileSize(mediaFile.getFileSize())
                .build());
    }
}
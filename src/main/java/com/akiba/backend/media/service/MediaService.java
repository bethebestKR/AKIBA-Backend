package com.akiba.backend.media.service;

import com.akiba.backend.media.domain.MediaFile;
import com.akiba.backend.media.dto.response.MediaUploadResponse;
import com.akiba.backend.media.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region}")
    private String region;

    @Transactional
    public MediaUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String s3Key = "uploads/" + UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        String s3Url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;

        MediaFile mediaFile = MediaFile.builder()
                .url(s3Url)
                .storagePath(s3Key)
                .originalFilename(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        MediaFile saved = mediaFileRepository.save(mediaFile);

        return MediaUploadResponse.builder()
                .mediaId(saved.getMediaId())
                .url(saved.getUrl())
                .originalFilename(saved.getOriginalFilename())
                .contentType(saved.getContentType())
                .fileSize(saved.getFileSize())
                .build();
    }

    public MediaFile getMedia(Long mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "미디어를 찾을 수 없습니다."));
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) return "";
        return filename.substring(lastDot);
    }
}
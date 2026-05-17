package com.kauniv.lightrip.global.s3.controller;

import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.s3.dto.request.PresignedUrlRequest;
import com.kauniv.lightrip.global.s3.dto.response.PresignedUrlResponse;
import com.kauniv.lightrip.global.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;
import java.util.UUID;

@Tag(name = "이미지 업로드 API", description = "S3 Presigned URL 기반 이미지 업로드를 제공합니다.")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final S3Service s3Service;

    @Operation(summary = "Presigned URL 발급", description = "S3 업로드용 Presigned URL과 CloudFront 이미지 조회 URL을 반환합니다. 앱은 presignedUrl로 S3에 직접 업로드하고, 완료 후 imageUrl을 DB 저장에 사용합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @AuthenticationPrincipal Long userId,
            @RequestBody PresignedUrlRequest request) {

        if (!ALLOWED_CONTENT_TYPES.contains(request.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String s3Key = request.getDomain() + "/" + userId + "/" + UUID.randomUUID() + getExtension(request.getContentType());

        String presignedUrl = s3Service.generatePresignedUploadUrl(s3Key, request.getContentType());
        String imageUrl = s3Service.getCloudFrontUrl(s3Key);

        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, imageUrl));
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
package com.kauniv.lightrip.global.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.cloudfront.domain}")
    private String cloudfrontDomain;

    // Presigned URL 생성 (업로드용)
    public String generatePresignedUploadUrl(String s3Key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    // CloudFront URL 반환 (조회용)
    public String getCloudFrontUrl(String s3Key) {
        return "https://" + cloudfrontDomain + "/" + s3Key;
    }

    // S3 파일 삭제
    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    // CloudFront URL로 S3 파일 삭제 (외부 URL이면 무시)
    public void deleteFileByUrl(String imageUrl) {
        String prefix = "https://" + cloudfrontDomain + "/";
        if (imageUrl == null || !imageUrl.startsWith(prefix)) return;
        deleteFile(imageUrl.substring(prefix.length()));
    }
}

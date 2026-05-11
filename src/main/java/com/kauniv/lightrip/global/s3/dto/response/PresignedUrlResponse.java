package com.kauniv.lightrip.global.s3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {

    private String presignedUrl; // 앱이 S3에 직접 업로드할 URL
    private String imageUrl;     // CloudFront URL (DB에 저장할 값)
}
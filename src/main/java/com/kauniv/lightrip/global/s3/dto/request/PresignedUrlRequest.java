package com.kauniv.lightrip.global.s3.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    private String domain;      // "passport" or "profile"
    private String contentType; // "image/jpeg", "image/png" 등
}
package com.kauniv.lightrip.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CompleteProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
    private String nickname;

    @Size(max = 100, message = "활동 지역은 100자 이하여야 합니다.")
    private String location;

    @Size(max = 200, message = "한 줄 소개는 200자 이하여야 합니다.")
    private String bio;
}

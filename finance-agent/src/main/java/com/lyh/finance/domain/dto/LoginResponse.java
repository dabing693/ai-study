package com.lyh.finance.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Data
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String email;
        private String nickname;
        private String avatar;
        private LocalDateTime createTime;
    }
}

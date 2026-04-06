package com.cat.standalone.auth;

import com.cat.common.model.ApiResponse;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化的认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SimpleAuthController {

    @Value("${cat.auth.default-user:admin}")
    private String defaultUser;

    @Value("${cat.auth.default-password:admin123}")
    private String defaultPassword;

    private static final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        if (defaultUser.equals(request.getUsername()) && defaultPassword.equals(request.getPassword())) {
            String token = "cat-" + UUID.randomUUID().toString().replace("-", "");
            TokenInfo tokenInfo = new TokenInfo(token, request.getUsername(), Instant.now().plusSeconds(86400));
            tokenStore.put(token, tokenInfo);
            return ApiResponse.success(new LoginResponse(token, request.getUsername(), "管理员"));
        }
        return ApiResponse.error(401, "用户名或密码错误");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("X-Cat-Token") String token) {
        tokenStore.remove(token);
        return ApiResponse.success();
    }

    @GetMapping("/validate")
    public ApiResponse<Boolean> validate(@RequestHeader("X-Cat-Token") String token) {
        return ApiResponse.success(validateToken(token));
    }

    public static boolean validateToken(String token) {
        if (token == null || !token.startsWith("cat-")) {
            return false;
        }
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return false;
        }
        return info.getExpiresAt().isAfter(Instant.now());
    }

    public static String getUsername(String token) {
        TokenInfo info = tokenStore.get(token);
        return info != null ? info.getUsername() : null;
    }

    @Data
    static class TokenInfo {
        private final String token;
        private final String username;
        private final Instant expiresAt;
    }
}

@Data
class LoginRequest {
    private String username;
    private String password;
}

@Data
class LoginResponse {
    private final String token;
    private final String username;
    private final String nickname;
}
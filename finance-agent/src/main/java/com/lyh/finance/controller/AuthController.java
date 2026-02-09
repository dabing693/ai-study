package com.lyh.finance.controller;

import com.lyh.finance.domain.dto.LoginRequest;
import com.lyh.finance.domain.dto.LoginResponse;
import com.lyh.finance.domain.dto.RegisterRequest;
import com.lyh.finance.domain.entity.User;
import com.lyh.finance.mapper.UserMapper;
import com.lyh.finance.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // 检查邮箱是否已存在
        User existingUser = userMapper.selectByEmail(request.getEmail());
        if (existingUser != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "邮箱已被注册");
            return ResponseEntity.badRequest().body(error);
        }

        // 创建新用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getEmail().split("@")[0]);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setCreateTime(user.getCreateTime());
        response.setUser(userInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // 查找用户
        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "邮箱或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "邮箱或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setCreateTime(user.getCreateTime());
        response.setUser(userInfo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "token无效或已过期");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "用户不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setCreateTime(user.getCreateTime());

        return ResponseEntity.ok(userInfo);
    }
}

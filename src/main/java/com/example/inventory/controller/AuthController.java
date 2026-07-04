package com.example.inventory.controller;

import com.example.inventory.dto.AuthResponse;
import com.example.inventory.dto.LoginRequest;
import com.example.inventory.dto.RegisterRequest;
import com.example.inventory.dto.ForgotPasswordRequest;
import com.example.inventory.dto.ResetPasswordRequest;
import com.example.inventory.entity.User;
import com.example.inventory.enums.RoleType;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);

        Cookie cookie = new Cookie("accessToken", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();

        return ResponseEntity.ok(new AuthResponse(user.getUsername(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest, HttpServletResponse response) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(RoleType.CUSTOMER);

        userRepository.save(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), signUpRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);

        Cookie cookie = new Cookie("accessToken", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponse(user.getUsername(), user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(user.getUsername(), user.getRole()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            
            // In a real app, send email here. For now, print to console for dev testing.
            System.out.println("============== PASSWORD RESET ==============");
            System.out.println("User requested password reset: " + user.getEmail());
            System.out.println("Reset Token (Link): http://localhost:3000/reset-password?token=" + token);
            System.out.println("============================================");
            
            // To make testing easier without email, we'll return the token in the response in this demo.
            return ResponseEntity.ok("Password reset email sent. (Demo Mode Token: " + token + ")");
        }
        return ResponseEntity.badRequest().body("Error: Email not found.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByResetToken(request.getToken());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("Error: Reset token has expired.");
            }
            
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            
            return ResponseEntity.ok("Password successfully reset.");
        }
        return ResponseEntity.badRequest().body("Error: Invalid reset token.");
    }
}

package com.example.inventory.service;

import com.example.inventory.dto.request.LoginRequest;
import com.example.inventory.dto.request.RegisterRequest;
import com.example.inventory.dto.response.AuthResponse;
import com.example.inventory.entity.User;
import com.example.inventory.enums.RoleType;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleType.CUSTOMER);
        userRepository.save(user);

        // Auto-login after registration
        return login(new LoginRequest(request.getUsername(), request.getPassword()));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtUtil.generateToken(auth);
        String refreshToken = jwtUtil.generateRefreshToken(auth);
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return new AuthResponse(accessToken, refreshToken, role);
    }
}
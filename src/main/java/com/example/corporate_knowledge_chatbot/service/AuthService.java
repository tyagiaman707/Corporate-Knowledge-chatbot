package com.example.corporate_knowledge_chatbot.service;

import com.example.corporate_knowledge_chatbot.dto.AuthDto;
import com.example.corporate_knowledge_chatbot.entity.User;
import com.example.corporate_knowledge_chatbot.exception.ChatbotException;
import com.example.corporate_knowledge_chatbot.repository.UserRepository;
import com.example.corporate_knowledge_chatbot.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     * Role defaults to EMPLOYEE unless explicitly set.
     */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ChatbotException.DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        User.Role role = (request.getRole() != null) ? request.getRole() : User.Role.EMPLOYEE;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} with role {}", user.getEmail(), user.getRole());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token, "Registration successful");
    }

    /**
     * Authenticate user and return JWT token.
     */
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        // Throws BadCredentialsException if wrong credentials (handled globally)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ChatbotException.ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user, token, "Login successful");
    }

    private AuthDto.AuthResponse buildAuthResponse(User user, String token, String message) {
        return AuthDto.AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message(message)
                .build();
    }
}
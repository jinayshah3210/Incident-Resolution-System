
package com.irs.backend.service;

import com.irs.backend.dto.LoginRequest;
import com.irs.backend.dto.LoginResponse;
import com.irs.backend.dto.RegisterRequest;
import com.irs.backend.model.Employee;
import com.irs.backend.model.User;
import com.irs.backend.repository.UserRepository;
import com.irs.backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        ));

        if (!user.isActive() ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return createLoginResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {

        String email = request.getEmail().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        Employee employee = new Employee(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                "General"
        );

        User savedUser = userRepository.save(employee);

        return createLoginResponse(savedUser);
    }

    private LoginResponse createLoginResponse(User user) {

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        LoginResponse.UserInfo userInfo =
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getRole()
                );

        return new LoginResponse(token, userInfo);
    }
}

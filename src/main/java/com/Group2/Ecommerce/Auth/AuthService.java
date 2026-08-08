package com.Group2.Ecommerce.Auth;

import com.Group2.Ecommerce.Auth.Dto.AuthResponse;
import com.Group2.Ecommerce.Auth.Dto.LoginRequest;
import com.Group2.Ecommerce.Auth.Dto.RegisterRequest;
import com.Group2.Ecommerce.Common.BrevoEmailService;
import com.Group2.Ecommerce.User.Role;
import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BrevoEmailService brevoEmailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return new AuthResponse(token, saved.getEmail(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    /**
     * Generates a 6-digit reset code and emails it to the user via Brevo.
     * The code is never returned in the response — only a generic message.
     * When Brevo isn't configured yet (dev mode), the code is logged instead
     * of emailed so the flow stays testable.
     */
    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No account found with that email"));

        String code = String.format("%06d", new Random().nextInt(1_000_000));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(code);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);

        boolean emailed = brevoEmailService.sendPasswordResetCode(email, code);

        return emailed
                ? "Password reset code sent to your email"
                : "Password reset code generated (dev mode — check server logs)";
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(String code) {
        validateAndGetResetToken(code);
    }

    @Transactional
    public void resetPassword(String code, String newPassword) {
        PasswordResetToken resetToken = validateAndGetResetToken(code);

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private PasswordResetToken validateAndGetResetToken(String code) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(code)
                .orElseThrow(() -> new IllegalStateException("Invalid or expired reset code"));

        if (resetToken.isUsed()) {
            throw new IllegalStateException("This reset code has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This reset code has expired");
        }

        return resetToken;
    }
}
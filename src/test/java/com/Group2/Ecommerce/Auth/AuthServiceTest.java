package com.Group2.Ecommerce.Auth;

import com.Group2.Ecommerce.Common.BrevoEmailService;
import com.Group2.Ecommerce.User.Role;
import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private BrevoEmailService brevoEmailService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded");
        user.setRole(Role.CUSTOMER);
    }

    @Test
    void forgotPassword_generatesSixDigitCode_emailsIt_andNeverReturnsIt() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(brevoEmailService.sendPasswordResetCode(eq("test@example.com"), anyString())).thenReturn(true);

        String message = authService.forgotPassword("test@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        String code = captor.getValue().getToken();

        assertThat(code).matches("\\d{6}");
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().isUsed()).isFalse();
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());

        verify(brevoEmailService).sendPasswordResetCode("test@example.com", code);
        // The raw code must never appear in the response message.
        assertThat(message).doesNotContain(code);
        assertThat(message).contains("sent");
    }

    @Test
    void forgotPassword_throws_whenNoAccountExists() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> authService.forgotPassword("nobody@example.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(brevoEmailService, never()).sendPasswordResetCode(anyString(), anyString());
    }

    @Test
    void resetPassword_updatesPassword_andMarksTokenUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("123456");
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);

        when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newpassword123")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword("123456", "newpassword123");

        verify(passwordEncoder).encode("newpassword123");
        assertThat(resetToken.isUsed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void resetPassword_throws_whenCodeAlreadyUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("123456");
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(true);

        when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(resetToken));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> authService.resetPassword("123456", "newpassword123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_throws_whenCodeExpired() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("123456");
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        resetToken.setUsed(false);

        when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(resetToken));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> authService.resetPassword("123456", "newpassword123"));

        verify(userRepository, never()).save(any());
    }
}

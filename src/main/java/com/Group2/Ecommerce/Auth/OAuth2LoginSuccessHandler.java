package com.Group2.Ecommerce.Auth;

import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.User.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.sendRedirect(frontendUrl + "/login?error=account_not_found");
            return;
        }

        String token = jwtService.generateToken(user);
        response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + token);
    }
}

package com.Group2.Ecommerce.Auth;

import com.Group2.Ecommerce.User.Role;
import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.User.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String email = resolveEmail(authentication);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(resolveName(authentication));
                    newUser.setEmail(email);
                    newUser.setPasswordHash(UUID.randomUUID().toString());
                    newUser.setRole(Role.CUSTOMER);
                    return userRepository.save(newUser);
                });

        String token = jwtService.generateToken(user);
        response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + token);
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken
                && oauthToken.getPrincipal() instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return authentication.getName();
    }

    private String resolveName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken
                && oauthToken.getPrincipal() instanceof OAuth2User oauth2User) {
            Object name = oauth2User.getAttributes().get("name");
            if (name instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return "Google User";
    }
}

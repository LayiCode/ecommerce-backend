package com.Group2.Ecommerce.Auth;

import com.Group2.Ecommerce.User.Role;
import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // Uses UserRepository directly instead of AuthService to avoid a circular
    // bean dependency: AuthService -> AuthenticationManager -> SecurityConfig
    // -> CustomOAuth2UserService -> AuthService.
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account has no email address");
        }

        // Merges by email: returns the existing account (preserving its role)
        // or creates a new CUSTOMER. New users get a random placeholder
        // password since they can't sign in with a password.
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setPasswordHash(UUID.randomUUID().toString());
                    newUser.setRole(Role.CUSTOMER);
                    return userRepository.save(newUser);
                });

        return new DefaultOAuth2User(user.getAuthorities(), attributes, "email");
    }
}

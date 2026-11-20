package ru.avito.avito.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.avito.avito.entity.User;
import ru.avito.avito.exception.ApiException;
import ru.avito.avito.repository.UserRepository;

@Component
public class MarketplaceAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MarketplaceAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName().trim().toLowerCase();
        String password = String.valueOf(authentication.getCredentials());
        User user = userRepository.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("invalid email or password"));
        if (!user.isVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "email and phone verification required");
        }

        MarketplacePrincipal principal =
                new MarketplacePrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

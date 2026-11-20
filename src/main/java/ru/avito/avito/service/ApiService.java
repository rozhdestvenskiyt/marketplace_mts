package ru.avito.avito.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.avito.avito.dto.ListingEnvelope;
import ru.avito.avito.dto.ListingRequest;
import ru.avito.avito.dto.ListingResponse;
import ru.avito.avito.dto.RegistrationResponse;
import ru.avito.avito.dto.ResendCodeRequest;
import ru.avito.avito.dto.UserEnvelope;
import ru.avito.avito.dto.UserRequest;
import ru.avito.avito.dto.UserResponse;
import ru.avito.avito.dto.VerificationResponse;
import ru.avito.avito.dto.VerifyCodeRequest;
import ru.avito.avito.entity.Listing;
import ru.avito.avito.entity.User;
import ru.avito.avito.entity.VerificationChannel;
import ru.avito.avito.entity.VerificationCode;
import ru.avito.avito.exception.ApiException;
import ru.avito.avito.notification.NotificationSender;
import ru.avito.avito.repository.ListingRepository;
import ru.avito.avito.repository.UserRepository;
import ru.avito.avito.repository.VerificationCodeRepository;
import ru.avito.avito.security.MarketplacePrincipal;
import ru.avito.avito.search.ListingDocument;
import ru.avito.avito.search.ListingIndexEvent;

@Service
public class ApiService {

    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
    private static final Duration HOURLY_SEND_WINDOW = Duration.ofHours(1);
    private static final int MAX_CODES_PER_HOUR = 5;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSender notificationSender;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiService(
            UserRepository userRepository,
            ListingRepository listingRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordEncoder passwordEncoder,
            NotificationSender notificationSender,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationSender = notificationSender;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegistrationResponse register(UserRequest request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();
        String phone = request.phone().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "an account with this email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "an account with this username already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "an account with this phone already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());
        user.setTermsAcceptedAt(Instant.now());
        User saved = userRepository.save(user);
        issueCode(saved, VerificationChannel.EMAIL);
        issueCode(saved, VerificationChannel.SMS);
        return new RegistrationResponse(toUserResponse(saved), List.of("EMAIL", "SMS"));
    }

    public UserEnvelope login(
            String email,
            String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizeEmail(email), password));
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        MarketplacePrincipal principal = (MarketplacePrincipal) authentication.getPrincipal();
        return new UserEnvelope(toUserResponse(findUser(principal.id())));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public VerificationResponse verify(VerifyCodeRequest request) {
        String destination = normalizeDestination(request.channel(), request.destination());
        User user = findUserByDestination(request.channel(), destination);
        VerificationCode activeCode = verificationCodeRepository
                .findFirstByUserAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(user, request.channel())
                .orElseThrow(() -> invalidCode());

        if (activeCode.getAttempts() >= MAX_CODE_ATTEMPTS) {
            throw invalidCode();
        }
        if (Instant.now().isAfter(activeCode.getExpiresAt())) {
            activeCode.setConsumedAt(Instant.now());
            verificationCodeRepository.save(activeCode);
            throw invalidCode();
        }
        if (!passwordEncoder.matches(request.code(), activeCode.getCodeHash())) {
            activeCode.setAttempts(activeCode.getAttempts() + 1);
            if (activeCode.getAttempts() >= MAX_CODE_ATTEMPTS) {
                activeCode.setConsumedAt(Instant.now());
            }
            verificationCodeRepository.save(activeCode);
            throw invalidCode();
        }

        activeCode.setConsumedAt(Instant.now());
        verificationCodeRepository.save(activeCode);
        if (request.channel() == VerificationChannel.EMAIL) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        user.setVerified(user.isEmailVerified() && user.isPhoneVerified());
        userRepository.save(user);
        return new VerificationResponse(user.isEmailVerified(), user.isPhoneVerified(), user.isVerified());
    }

    @Transactional
    public void resendCode(ResendCodeRequest request) {
        String destination = normalizeDestination(request.channel(), request.destination());
        var candidate = request.channel() == VerificationChannel.EMAIL
                ? userRepository.findByEmail(destination)
                : userRepository.findByPhone(destination);
        candidate
                .filter(user -> !user.isVerified())
                .ifPresent(user -> {
                    VerificationCode latest = verificationCodeRepository
                            .findFirstByUserAndChannelOrderByCreatedAtDesc(user, request.channel())
                            .orElse(null);
                    if (latest != null && latest.getCreatedAt().plus(RESEND_DELAY).isAfter(Instant.now())) {
                        return;
                    }
                    if (verificationCodeRepository.countByUserAndChannelAndCreatedAtAfter(
                                    user, request.channel(), Instant.now().minus(HOURLY_SEND_WINDOW))
                            >= MAX_CODES_PER_HOUR) {
                        return;
                    }
                    issueCode(user, request.channel());
                });
    }

    @Transactional(readOnly = true)
    public UserEnvelope currentUser(MarketplacePrincipal principal) {
        return new UserEnvelope(toUserResponse(findUser(principal.id())));
    }

    @Transactional(readOnly = true)
    public UserEnvelope logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        MarketplacePrincipal principal = (MarketplacePrincipal) authentication.getPrincipal();
        User user = findUser(principal.id());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return new UserEnvelope(toUserResponse(user));
    }

    @Transactional
    public ListingEnvelope createListing(ListingRequest request, MarketplacePrincipal principal) {
        User seller = findUser(principal.id());
        Listing listing = new Listing();
        listing.setUser(seller);
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(request.price());
        listing.setPhotos(request.images() == null ? List.of() : request.images());
        listing.setCreatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        eventPublisher.publishEvent(ListingIndexEvent.upsert(new ListingDocument(
                saved.getId(),
                saved.getUser().getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getPrice(),
                saved.getPhotos(),
                saved.getCreatedAt())));
        return new ListingEnvelope(toListingResponse(saved));
    }

    @Transactional
    public void deleteListing(long id, MarketplacePrincipal principal) {
        if (id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid listing id");
        }
        User user = findUser(principal.id());
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "listing not found"));
        if (!listing.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "you can only delete your own listings");
        }
        listingRepository.delete(listing);
        eventPublisher.publishEvent(ListingIndexEvent.delete(listing.getId()));
    }

    private void issueCode(User user, VerificationChannel channel) {
        verificationCodeRepository.deleteByUserAndChannel(user, channel);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant now = Instant.now();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUser(user);
        verificationCode.setChannel(channel);
        verificationCode.setCodeHash(passwordEncoder.encode(code));
        verificationCode.setCreatedAt(now);
        verificationCode.setExpiresAt(now.plus(CODE_LIFETIME));
        verificationCodeRepository.save(verificationCode);
        if (channel == VerificationChannel.EMAIL) {
            notificationSender.sendEmailCode(user.getEmail(), code);
        } else {
            notificationSender.sendSmsCode(user.getPhone(), code);
        }
    }

    private User findUserByDestination(VerificationChannel channel, String destination) {
        return (channel == VerificationChannel.EMAIL
                ? userRepository.findByEmail(destination)
                : userRepository.findByPhone(destination))
                .orElseThrow(() -> invalidCode());
    }

    private User findUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "authentication required"));
    }

    private String normalizeDestination(VerificationChannel channel, String destination) {
        return channel == VerificationChannel.EMAIL
                ? normalizeEmail(destination)
                : destination.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid or expired verification code");
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPhone(),
                user.isEmailVerified(),
                user.isPhoneVerified());
    }

    private ListingResponse toListingResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getUser().getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getPhotos() == null ? List.of() : listing.getPhotos());
    }
}

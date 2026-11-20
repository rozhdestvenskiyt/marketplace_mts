package ru.avito.avito.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.avito.avito.dto.CsrfResponse;
import ru.avito.avito.dto.ListingEnvelope;
import ru.avito.avito.dto.ListingRequest;
import ru.avito.avito.dto.ListingSearchResponse;
import ru.avito.avito.dto.LoginRequest;
import ru.avito.avito.dto.RegistrationResponse;
import ru.avito.avito.dto.ResendCodeRequest;
import ru.avito.avito.dto.ReviewPageResponse;
import ru.avito.avito.dto.ReviewRequest;
import ru.avito.avito.dto.ReviewResponse;
import ru.avito.avito.dto.UserEnvelope;
import ru.avito.avito.dto.UserRequest;
import ru.avito.avito.dto.VerificationResponse;
import ru.avito.avito.dto.VerifyCodeRequest;
import ru.avito.avito.security.MarketplacePrincipal;
import ru.avito.avito.service.ApiService;
import ru.avito.avito.search.ListingSearchService;
import ru.avito.avito.service.ReviewService;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final ApiService apiService;
    private final ReviewService reviewService;
    private final ListingSearchService listingSearchService;

    public ApiController(
            ApiService apiService,
            ReviewService reviewService,
            ListingSearchService listingSearchService) {
        this.apiService = apiService;
        this.reviewService = reviewService;
        this.listingSearchService = listingSearchService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }

    @PostMapping("/user/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiService.register(request));
    }

    @PostMapping("/user/login")
    public UserEnvelope login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return apiService.login(request.email(), request.password(), servletRequest, servletResponse);
    }

    @PostMapping("/user/verify")
    public VerificationResponse verify(@Valid @RequestBody VerifyCodeRequest request) {
        return apiService.verify(request);
    }

    @PostMapping("/user/verification/resend")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        apiService.resendCode(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/user/me")
    public UserEnvelope currentUser(@AuthenticationPrincipal MarketplacePrincipal principal) {
        return apiService.currentUser(principal);
    }

    @PostMapping("/user/logout")
    public UserEnvelope logout(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return apiService.logout(authentication, servletRequest, servletResponse);
    }

    @PostMapping("/listing")
    public ResponseEntity<ListingEnvelope> createListing(
            @Valid @RequestBody ListingRequest request,
            @AuthenticationPrincipal MarketplacePrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiService.createListing(request, principal));
    }

    @DeleteMapping("/listing/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable long id,
            @AuthenticationPrincipal MarketplacePrincipal principal) {
        apiService.deleteListing(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listing/search")
    public ListingSearchResponse searchListings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return listingSearchService.search(q, minPrice, maxPrice, sellerId, sort, page, size);
    }

    @PostMapping("/listing/{listingId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable long listingId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal MarketplacePrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(listingId, request, principal));
    }

    @GetMapping("/listing/{listingId}/reviews")
    public ReviewPageResponse listingReviews(
            @PathVariable long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.findByListing(listingId, page, size);
    }
}

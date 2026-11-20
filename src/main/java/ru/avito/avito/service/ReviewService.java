package ru.avito.avito.service;

import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.avito.avito.dto.ReviewPageResponse;
import ru.avito.avito.dto.ReviewRequest;
import ru.avito.avito.dto.ReviewResponse;
import ru.avito.avito.entity.Listing;
import ru.avito.avito.entity.Review;
import ru.avito.avito.entity.User;
import ru.avito.avito.exception.ApiException;
import ru.avito.avito.repository.ListingRepository;
import ru.avito.avito.repository.ReviewRepository;
import ru.avito.avito.repository.UserRepository;
import ru.avito.avito.security.MarketplacePrincipal;

@Service
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ListingRepository listingRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse create(long listingId, ReviewRequest request, MarketplacePrincipal principal) {
        Listing listing = findListing(listingId);
        User author = userRepository.findById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "authentication required"));
        if (listing.getUser().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "you cannot review your own listing");
        }
        if (reviewRepository.existsByListingIdAndAuthorId(listingId, author.getId())) {
            throw duplicateReview();
        }

        Review review = new Review();
        review.setListing(listing);
        review.setAuthor(author);
        review.setRating(request.rating());
        review.setComment(request.comment().trim());
        review.setCreatedAt(Instant.now());
        try {
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateReview();
        }
    }

    @Transactional(readOnly = true)
    public ReviewPageResponse findByListing(long listingId, int page, int size) {
        findListing(listingId);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid pagination parameters");
        }

        Page<Review> results = reviewRepository.findAllByListingId(
                listingId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        double averageRating = reviewRepository.summarizeByListingId(listingId)
                .map(ReviewRepository.ReviewSummary::getAverageRating)
                .orElse(0.0);
        List<ReviewResponse> reviews = results.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new ReviewPageResponse(
                reviews,
                results.getTotalElements(),
                results.getNumber(),
                results.getSize(),
                results.getTotalPages(),
                averageRating);
    }

    private Listing findListing(long listingId) {
        if (listingId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid listing id");
        }
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "listing not found"));
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getListing().getId(),
                review.getAuthor().getId(),
                review.getAuthor().getUsername(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    private ApiException duplicateReview() {
        return new ApiException(HttpStatus.CONFLICT, "you have already reviewed this listing");
    }
}

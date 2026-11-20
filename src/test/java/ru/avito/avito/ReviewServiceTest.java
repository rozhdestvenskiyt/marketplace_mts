package ru.avito.avito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.avito.avito.dto.ReviewRequest;
import ru.avito.avito.entity.Listing;
import ru.avito.avito.entity.Review;
import ru.avito.avito.entity.User;
import ru.avito.avito.exception.ApiException;
import ru.avito.avito.repository.ListingRepository;
import ru.avito.avito.repository.ReviewRepository;
import ru.avito.avito.repository.UserRepository;
import ru.avito.avito.security.MarketplacePrincipal;
import ru.avito.avito.service.ReviewService;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, listingRepository, userRepository);
    }

    @Test
    void createRejectsReviewOfOwnListing() {
        User seller = user(10L);
        Listing listing = listing(25L, seller);
        when(listingRepository.findById(25L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(10L)).thenReturn(Optional.of(seller));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> reviewService.create(
                        25L,
                        new ReviewRequest(5, "Хорошее объявление"),
                        new MarketplacePrincipal(10L, "seller@example.com", "hash")));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void createRejectsSecondReviewFromSameUser() {
        User seller = user(10L);
        User author = user(11L);
        Listing listing = listing(25L, seller);
        when(listingRepository.findById(25L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(11L)).thenReturn(Optional.of(author));
        when(reviewRepository.existsByListingIdAndAuthorId(25L, 11L)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> reviewService.create(
                        25L,
                        new ReviewRequest(5, "Хорошее объявление"),
                        new MarketplacePrincipal(11L, "buyer@example.com", "hash")));

        assertEquals(409, exception.getStatus().value());
        verify(reviewRepository).existsByListingIdAndAuthorId(25L, 11L);
    }

    @Test
    void createStoresReviewAndReturnsExistingResponseShape() {
        User seller = user(10L);
        User author = user(11L);
        Listing listing = listing(25L, seller);
        when(listingRepository.findById(25L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(11L)).thenReturn(Optional.of(author));
        when(reviewRepository.existsByListingIdAndAuthorId(25L, 11L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = reviewService.create(
                25L,
                new ReviewRequest(5, "  Отлично  "),
                new MarketplacePrincipal(11L, "buyer@example.com", "hash"));

        assertEquals(25L, response.listingId());
        assertEquals(11L, response.authorId());
        assertEquals("user-11", response.authorName());
        assertEquals(5, response.rating());
        assertEquals("Отлично", response.comment());
        verify(reviewRepository).saveAndFlush(any(Review.class));
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }

    private Listing listing(long id, User seller) {
        Listing listing = new Listing();
        listing.setId(id);
        listing.setUser(seller);
        return listing;
    }
}

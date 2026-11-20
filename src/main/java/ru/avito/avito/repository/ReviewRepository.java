package ru.avito.avito.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.avito.avito.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByListingIdAndAuthorId(long listingId, long authorId);

    Page<Review> findAllByListingId(long listingId, Pageable pageable);

    @Query("""
            select avg(r.rating) as averageRating, count(r.id) as reviewCount
            from Review r
            where r.listing.id = :listingId
            """)
    Optional<ReviewSummary> summarizeByListingId(@Param("listingId") long listingId);

    interface ReviewSummary {
        Double getAverageRating();

        Long getReviewCount();
    }
}

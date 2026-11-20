package ru.avito.avito.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.avito.avito.entity.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}

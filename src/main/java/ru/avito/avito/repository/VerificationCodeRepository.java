package ru.avito.avito.repository;

import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.avito.avito.entity.User;
import ru.avito.avito.entity.VerificationChannel;
import ru.avito.avito.entity.VerificationCode;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUserAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(
            User user, VerificationChannel channel);

    Optional<VerificationCode> findFirstByUserAndChannelOrderByCreatedAtDesc(User user, VerificationChannel channel);

    long countByUserAndChannelAndCreatedAtAfter(User user, VerificationChannel channel, Instant createdAt);

    void deleteByUserAndChannel(User user, VerificationChannel channel);
}

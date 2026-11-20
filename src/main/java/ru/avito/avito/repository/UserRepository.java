package ru.avito.avito.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.avito.avito.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);
}

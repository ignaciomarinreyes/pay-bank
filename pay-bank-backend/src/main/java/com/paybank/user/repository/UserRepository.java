package com.paybank.user.repository;

import java.util.Optional;

import com.paybank.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** The alias must already be normalized (see {@link User#normalizeAlias(String)}). */
    Optional<User> findByAlias(String alias);
}

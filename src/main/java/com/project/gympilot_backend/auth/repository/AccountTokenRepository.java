package com.project.gympilot_backend.auth.repository;

import com.project.gympilot_backend.auth.model.AccountToken;
import com.project.gympilot_backend.auth.model.AccountTokenPurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {
    Optional<AccountToken> findByTokenHashAndPurpose(String tokenHash, AccountTokenPurpose purpose);
}

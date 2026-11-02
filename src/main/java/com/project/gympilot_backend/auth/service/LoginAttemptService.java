package com.project.gympilot_backend.auth.service;

import com.project.gympilot_backend.security.config.AppSecurityProperties;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final UserRepository userRepository;
    private final AppSecurityProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);
            if (failures >= properties.getMaxFailedLogins()) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(Instant.now().plus(properties.getLockMinutes(), ChronoUnit.MINUTES));
            }
            userRepository.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User recordSuccess(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }
}

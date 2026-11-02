package com.project.gympilot_backend.user.service;

import com.project.gympilot_backend.auth.repository.RefreshTokenRepository;
import com.project.gympilot_backend.user.dto.UpdateAccountRequest;
import com.project.gympilot_backend.user.dto.UserAccountResponse;
import com.project.gympilot_backend.user.model.User;
import com.project.gympilot_backend.user.model.UserStatus;
import com.project.gympilot_backend.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getAccount(User user) {
        return UserAccountResponse.from(user);
    }

    @Transactional
    public UserAccountResponse updateAccount(User user, UpdateAccountRequest request) {
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return UserAccountResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deactivate(User user) {
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }
}

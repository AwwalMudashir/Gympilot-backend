package com.project.gympilot_backend.notification.email.service;

import com.project.gympilot_backend.notification.email.model.TransactionalEmail;

public interface TransactionalEmailService {
    boolean isEnabled();
    void send(TransactionalEmail email);
}

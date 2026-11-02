package com.project.gympilot_backend.wellness.sleep.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "sleep_logs")
public class SleepLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "slept_at", nullable = false)
    private Instant sleptAt;
    @Column(name = "woke_at", nullable = false)
    private Instant wokeAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SleepQuality quality;
    @Column(name = "energy_level", nullable = false)
    private int energyLevel;
    @Column(length = 500)
    private String notes;
}

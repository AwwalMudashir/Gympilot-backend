package com.project.gympilot_backend.coach.model;

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
@Entity @Table(name = "coach_briefs")
public class CoachBrief extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "brief_type", nullable = false, length = 20)
    private BriefType briefType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "focus_today", columnDefinition = "TEXT")
    private String focusToday;
    @Column(columnDefinition = "TEXT")
    private String cautions;
    @Column(name = "context_snapshot", columnDefinition = "TEXT")
    private String contextSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CoachFeedback feedback;
    @Column(name = "feedback_at")
    private Instant feedbackAt;
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}

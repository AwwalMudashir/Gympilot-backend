package com.project.gympilot_backend.wellness.nutrition.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "nutrition_logs")
public class NutritionLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "meal_name", nullable = false, length = 160)
    private String mealName;
    @Column(nullable = false)
    private int calories;
    @Column(name = "protein_grams", nullable = false, precision = 7, scale = 2)
    private BigDecimal proteinGrams;
    @Column(name = "carbohydrate_grams", nullable = false, precision = 7, scale = 2)
    private BigDecimal carbohydrateGrams;
    @Column(name = "fat_grams", nullable = false, precision = 7, scale = 2)
    private BigDecimal fatGrams;
    @Column(name = "fibre_grams", precision = 7, scale = 2)
    private BigDecimal fibreGrams;
    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;
}

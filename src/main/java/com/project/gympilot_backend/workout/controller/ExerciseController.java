package com.project.gympilot_backend.workout.controller;

import com.project.gympilot_backend.common.response.ApiResponse;
import com.project.gympilot_backend.workout.dto.ExerciseResponse;
import com.project.gympilot_backend.workout.service.ExerciseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ApiResponse<List<ExerciseResponse>> list(
            @RequestParam(required = false) Boolean cameraSupported,
            HttpServletRequest request
    ) {
        return ApiResponse.success("Exercises retrieved", exerciseService.list(cameraSupported), request.getRequestURI());
    }
}

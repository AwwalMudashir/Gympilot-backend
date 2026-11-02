package com.project.gympilot_backend.auth.dto;

public record TokenDeliveryResponse(boolean accepted, String developmentToken) {}

package com.example.jobboard.dto;

import java.util.Map;

public record EventDto(
    String sessionId,
    String eventType,
    Map<String, Object> eventData,
    String page,
    String referrer
) {}

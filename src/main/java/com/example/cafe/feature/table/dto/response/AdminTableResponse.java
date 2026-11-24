package com.example.cafe.feature.table.dto.response;

public record AdminTableResponse(
    Long id,
    String name,
    String qrCode,
    boolean isActive,
    boolean hasOpenSession,
    Long openSessionId,
    boolean hasSessionHistory,
    boolean canDeactivate,
    boolean deletable
) {}

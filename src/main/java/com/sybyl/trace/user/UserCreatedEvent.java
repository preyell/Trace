package com.sybyl.trace.user;

public record UserCreatedEvent(
    Long userId, String email, String username, String firstName
    ) {}

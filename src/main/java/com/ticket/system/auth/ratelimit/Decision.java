package com.ticket.system.auth.ratelimit;

public record Decision(boolean allowed, long retryAfterSeconds) {}


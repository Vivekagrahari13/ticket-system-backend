package com.ticket.system.auth.dto;

import com.ticket.system.user.enums.Role;


public record UserResponse(Long id,
                           String email,
                           Role role)
{}

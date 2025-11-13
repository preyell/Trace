package com.sybyl.trace.user;

import java.util.Set;

import com.sybyl.trace.location.Location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    
    @NotEmpty Set<Location> locations,
    @NotEmpty Set<AppRole> roles,
    Set<Long> verticalIds
) {}

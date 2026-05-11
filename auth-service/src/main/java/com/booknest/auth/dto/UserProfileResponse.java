package com.booknest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private int userId;
    private String fullName;
    private String email;
    private String role;
    private String provider;
    private Long mobile;
}
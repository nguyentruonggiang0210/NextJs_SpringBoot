package com.example.demo.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private long id;       // có thể là username, email, hoặc studentId...
    private String password;
}
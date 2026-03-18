package com.personal.happygallery.app.web.customer.dto;

public record CustomerUserResponse(Long id, String email, String name, String phone, boolean phoneVerified) {}

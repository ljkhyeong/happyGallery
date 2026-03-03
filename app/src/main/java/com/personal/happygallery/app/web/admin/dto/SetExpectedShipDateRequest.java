package com.personal.happygallery.app.web.admin.dto;

import java.time.LocalDate;

/** PATCH /admin/orders/{id}/expected-ship-date 요청 바디 */
public record SetExpectedShipDateRequest(LocalDate expectedShipDate) {}

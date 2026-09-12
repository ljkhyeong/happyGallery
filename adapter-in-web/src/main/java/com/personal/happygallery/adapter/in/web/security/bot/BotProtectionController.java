package com.personal.happygallery.adapter.in.web.security.bot;

import com.personal.happygallery.adapter.in.web.security.bot.dto.BotProtectionResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes;
import com.personal.happygallery.application.security.port.in.BotProtectionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotProtectionController {
    private final BotProtectionUseCase protection;

    public BotProtectionController(BotProtectionUseCase protection) {
        this.protection = protection;
    }

    @GetMapping(CustomerSecurityRoutes.BOT_PROTECTION_API)
    @Operation(operationId = "getBotProtection")
    public BotProtectionResponse configuration() {
        return new BotProtectionResponse(protection.siteKey());
    }
}

package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.FavoritePageResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.FavoriteStatusResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.FavoriteUseCase;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/favorites")
public class MeFavoriteController {
    private final FavoriteUseCase favorites;
    public MeFavoriteController(FavoriteUseCase favorites) { this.favorites = favorites; }
    @GetMapping
    @Operation(operationId = "listMyFavorites")
    public FavoritePageResponse list(@AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) FavoriteTargetType type, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return FavoritePageResponse.from(favorites.list(customer.userId(), type, cursor, size));
    }
    @GetMapping("/{type}/{targetId}")
    @Operation(operationId = "getMyFavoriteStatus")
    public FavoriteStatusResponse status(@AuthenticationPrincipal CustomerPrincipal customer,
            @PathVariable FavoriteTargetType type, @PathVariable @Positive Long targetId) {
        return new FavoriteStatusResponse(favorites.isSaved(customer.userId(), type, targetId));
    }
    @PutMapping("/{type}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "saveMyFavorite")
    public void save(@AuthenticationPrincipal CustomerPrincipal customer,
            @PathVariable FavoriteTargetType type, @PathVariable @Positive Long targetId) {
        favorites.save(customer.userId(), type, targetId);
    }
    @DeleteMapping("/{type}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "removeMyFavorite")
    public void remove(@AuthenticationPrincipal CustomerPrincipal customer,
            @PathVariable FavoriteTargetType type, @PathVariable @Positive Long targetId) {
        favorites.remove(customer.userId(), type, targetId);
    }
}

package com.personal.happygallery.app.web.pass;

import com.personal.happygallery.app.pass.PassPurchaseService;
import com.personal.happygallery.app.web.pass.dto.PurchasePassRequest;
import com.personal.happygallery.app.web.pass.dto.PurchasePassResponse;
import com.personal.happygallery.domain.pass.PassPurchase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passes")
public class PassController {

    private final PassPurchaseService passPurchaseService;

    public PassController(PassPurchaseService passPurchaseService) {
        this.passPurchaseService = passPurchaseService;
    }

    /** 게스트 8회권 구매 */
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchasePassResponse purchaseForGuest(@RequestBody @Valid PurchasePassRequest request) {
        PassPurchase purchase = passPurchaseService.purchaseForGuest(request.guestId());
        return PurchasePassResponse.from(purchase);
    }
}

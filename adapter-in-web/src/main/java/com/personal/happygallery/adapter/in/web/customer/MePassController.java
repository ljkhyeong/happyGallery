package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.MyPassSummary;
import com.personal.happygallery.adapter.in.web.customer.dto.PurchaseMemberPassRequest;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import com.personal.happygallery.domain.pass.PassPurchase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/passes")
public class MePassController {

    private final PassQueryUseCase passQueryUseCase;
    private final PassPurchaseUseCase passPurchaseUseCase;

    public MePassController(PassQueryUseCase passQueryUseCase,
                             PassPurchaseUseCase passPurchaseUseCase) {
        this.passQueryUseCase = passQueryUseCase;
        this.passPurchaseUseCase = passPurchaseUseCase;
    }

    @GetMapping
    public List<MyPassSummary> myPasses(@CustomerUserId Long userId) {
        return passQueryUseCase.listMyPasses(userId).stream()
                .map(MyPassSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MyPassSummary myPass(@PathVariable Long id, @CustomerUserId Long userId) {
        PassPurchase pass = passQueryUseCase.findMyPass(id, userId);
        return MyPassSummary.from(pass);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyPassSummary purchasePass(@RequestBody @Valid PurchaseMemberPassRequest req,
                                      @CustomerUserId Long userId) {
        PassPurchase pass = passPurchaseUseCase.purchaseForMember(userId, req.totalPrice());
        return MyPassSummary.from(pass);
    }
}

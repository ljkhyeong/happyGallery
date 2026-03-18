package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.app.pass.PassQueryService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.MyPassSummary;
import com.personal.happygallery.app.web.customer.dto.PurchaseMemberPassRequest;
import com.personal.happygallery.domain.pass.PassPurchase;
import jakarta.servlet.http.HttpServletRequest;
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

    private final PassQueryService passQueryService;
    private final PassPurchaseUseCase passPurchaseService;

    public MePassController(PassQueryService passQueryService,
                             PassPurchaseUseCase passPurchaseService) {
        this.passQueryService = passQueryService;
        this.passPurchaseService = passPurchaseService;
    }

    @GetMapping
    public List<MyPassSummary> myPasses(HttpServletRequest request) {
        Long userId = getUserId(request);
        return passQueryService.listMyPasses(userId).stream()
                .map(MyPassSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MyPassSummary myPass(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        PassPurchase pass = passQueryService.findMyPass(id, userId);
        return MyPassSummary.from(pass);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyPassSummary purchasePass(@RequestBody @Valid PurchaseMemberPassRequest req,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        PassPurchase pass = passPurchaseService.purchaseForMember(userId, req.totalPrice());
        return MyPassSummary.from(pass);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}

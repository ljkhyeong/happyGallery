package com.personal.happygallery.app.payment;

import com.personal.happygallery.app.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.app.payment.port.out.RefundFailureScriptPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("local")
@Service
public class DefaultDevRefundFailureService implements DevRefundFailureUseCase {

    private final RefundFailureScriptPort refundFailureScript;

    public DefaultDevRefundFailureService(RefundFailureScriptPort refundFailureScript) {
        this.refundFailureScript = refundFailureScript;
    }

    @Override
    public void armNextFailure(String reason) {
        refundFailureScript.armNextFailure(reason);
    }

    @Override
    public void clear() {
        refundFailureScript.clear();
    }
}

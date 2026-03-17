package com.personal.happygallery.app.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;

public interface InquiryStorePort {

    Inquiry save(Inquiry inquiry);
}

package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;

public interface InquiryStorePort {

    Inquiry save(Inquiry inquiry);
}

package com.personal.happygallery.application.inquiry.port.out;

import com.personal.happygallery.domain.inquiry.Inquiry;

public interface InquiryStorePort {

    <S extends Inquiry> S save(S inquiry);
}

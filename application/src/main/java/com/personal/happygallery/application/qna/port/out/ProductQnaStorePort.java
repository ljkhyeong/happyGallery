package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;

public interface ProductQnaStorePort {

    ProductQna save(ProductQna qna);
}

package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;

public interface ProductQnaStorePort {

    <S extends ProductQna> S save(S qna);
}

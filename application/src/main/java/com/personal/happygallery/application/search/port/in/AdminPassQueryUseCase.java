package com.personal.happygallery.application.search.port.in;

import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.shared.page.OffsetPage;

public interface AdminPassQueryUseCase {

    OffsetPage<AdminPassView> search(String keyword, int page, int size);

    AdminPassView get(Long passId);
}

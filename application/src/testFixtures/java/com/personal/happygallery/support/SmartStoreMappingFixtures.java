package com.personal.happygallery.support;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.shared.AdminActor;
import java.util.List;
import java.util.Optional;

/** 스마트스토어 재고 매핑 테스트의 명령·처리자. 운영 port는 감사 처리자를 항상 요구한다. */
public final class SmartStoreMappingFixtures {

    /** 특정 관리자 없이 저장하는 테스트의 처리자. 이름이 비어 있어 "시스템"으로 기록된다. */
    public static final AdminActor SYSTEM_ACTOR = new AdminActor(null, null);

    private SmartStoreMappingFixtures() {}

    /** 기존 매핑 개정 확인이 없는 첫 저장 명령. */
    public static SaveMappingCommand newMapping(Long originProductNo, boolean enabled, List<VariantMapping> variants) {
        return new SaveMappingCommand(originProductNo, enabled, variants, null, false);
    }

    /** 현재 매핑의 개정 번호를 넣고 원상품이 바뀌면 확인 플래그를 켠 저장 명령. 매핑이 없으면 첫 저장과 같다. */
    public static SaveMappingCommand revisedMapping(
            Optional<MappingResult> current, Long originProductNo, boolean enabled, List<VariantMapping> variants) {
        boolean originChanged = current.isPresent()
                && !current.orElseThrow().originProductNo().equals(originProductNo);
        return new SaveMappingCommand(
                originProductNo, enabled, variants,
                current.map(MappingResult::mappingVersion).orElse(null), originChanged);
    }
}

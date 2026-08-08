package com.personal.happygallery.application.coupon;

import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponDefinitionCommand;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionStorePort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponReaderPort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCouponAdminService implements CouponAdminUseCase {

    private final CouponDefinitionReaderPort definitionReader;
    private final CouponDefinitionStorePort definitionStore;
    private final IssuedCouponReaderPort issuedCouponReader;

    public DefaultCouponAdminService(CouponDefinitionReaderPort definitionReader,
                                     CouponDefinitionStorePort definitionStore,
                                     IssuedCouponReaderPort issuedCouponReader) {
        this.definitionReader = definitionReader;
        this.definitionStore = definitionStore;
        this.issuedCouponReader = issuedCouponReader;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDefinition> list() {
        return definitionReader.findAllByOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDefinition get(Long definitionId) {
        return findDefinition(definitionId);
    }

    @Override
    @Transactional
    public CouponDefinition create(CouponDefinitionCommand command) {
        requireCommand(command);
        return definitionStore.save(new CouponDefinition(
                command.name(),
                command.discountType(),
                command.discountValue(),
                command.minOrderAmount(),
                command.maxDiscountAmount(),
                command.validFrom(),
                command.validUntil(),
                command.active(),
                command.publiclyClaimable()));
    }

    @Override
    @Transactional
    public CouponDefinition update(Long definitionId,
                                   long expectedVersion,
                                   CouponDefinitionCommand command) {
        requireCommand(command);
        CouponDefinition definition = findDefinitionForUpdate(definitionId);
        requireExpectedVersion(definition, expectedVersion);
        if (issuedCouponReader.existsByDefinitionId(definitionId)
                && changesIssuedCouponTerms(definition, command)) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "이미 발급된 쿠폰은 이름·할인 조건·유효기간을 변경할 수 없습니다.");
        }
        definition.update(
                command.name(),
                command.discountType(),
                command.discountValue(),
                command.minOrderAmount(),
                command.maxDiscountAmount(),
                command.validFrom(),
                command.validUntil(),
                command.active(),
                command.publiclyClaimable());
        return definitionStore.save(definition);
    }

    @Override
    @Transactional
    public void delete(Long definitionId, long expectedVersion) {
        CouponDefinition definition = findDefinitionForUpdate(definitionId);
        requireExpectedVersion(definition, expectedVersion);
        definition.deactivate();
        definitionStore.save(definition);
    }

    private CouponDefinition findDefinition(Long definitionId) {
        return definitionReader.findById(definitionId)
                .orElseThrow(NotFoundException.supplier("쿠폰 정의"));
    }

    private CouponDefinition findDefinitionForUpdate(Long definitionId) {
        return definitionReader.findByIdForUpdate(definitionId)
                .orElseThrow(NotFoundException.supplier("쿠폰 정의"));
    }

    private static boolean changesIssuedCouponTerms(
            CouponDefinition definition, CouponDefinitionCommand command) {
        String requestedName = command.name() == null ? null : command.name().strip();
        return !Objects.equals(definition.getName(), requestedName)
                || definition.getDiscountType() != command.discountType()
                || definition.getDiscountValue() != command.discountValue()
                || definition.getMinOrderAmount() != command.minOrderAmount()
                || !Objects.equals(definition.getMaxDiscountAmount(), command.maxDiscountAmount())
                || !Objects.equals(definition.getValidFrom(), command.validFrom())
                || !Objects.equals(definition.getValidUntil(), command.validUntil());
    }

    private static void requireCommand(CouponDefinitionCommand command) {
        if (command == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "쿠폰 정의가 누락되었습니다.");
        }
    }

    private static void requireExpectedVersion(CouponDefinition definition, long expectedVersion) {
        if (expectedVersion < 0L || definition.getVersion() != expectedVersion) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "다른 관리자가 쿠폰을 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 처리해주세요.");
        }
    }
}

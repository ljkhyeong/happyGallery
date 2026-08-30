package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 합계를 보존하는 최대 나머지 방식 원 단위 배분기. */
public final class ProportionalAmountAllocator {

    private ProportionalAmountAllocator() {}

    public static List<Long> allocate(long amount, List<Long> bases) {
        if (amount < 0L || bases == null || bases.isEmpty()
                || bases.stream().anyMatch(base -> base == null || base < 0L)) {
            throw invalid();
        }
        long baseSum;
        try {
            baseSum = bases.stream().reduce(0L, Math::addExact);
        } catch (ArithmeticException exception) {
            throw invalid();
        }
        if (amount > baseSum || (baseSum == 0L && amount != 0L)) {
            throw invalid();
        }
        if (amount == 0L) {
            return Collections.nCopies(bases.size(), 0L);
        }

        BigInteger total = BigInteger.valueOf(amount);
        BigInteger denominator = BigInteger.valueOf(baseSum);
        List<Share> shares = new ArrayList<>(bases.size());
        long allocated = 0L;
        for (int index = 0; index < bases.size(); index++) {
            BigInteger numerator = total.multiply(BigInteger.valueOf(bases.get(index)));
            BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(denominator);
            long floor = quotientAndRemainder[0].longValueExact();
            shares.add(new Share(index, floor, quotientAndRemainder[1]));
            allocated += floor;
        }
        long remainderUnits = amount - allocated;
        shares.stream()
                .sorted(Comparator.comparing(Share::remainder).reversed()
                        .thenComparingInt(Share::index))
                .limit(remainderUnits)
                .forEach(Share::addOne);
        shares.sort(Comparator.comparingInt(Share::index));
        return shares.stream().map(Share::amount).toList();
    }

    private static HappyGalleryException invalid() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, "혜택 금액 배분 기준이 올바르지 않습니다.");
    }

    private static final class Share {
        private final int index;
        private long amount;
        private final BigInteger remainder;

        private Share(int index, long amount, BigInteger remainder) {
            this.index = index;
            this.amount = amount;
            this.remainder = remainder;
        }

        private int index() { return index; }
        private long amount() { return amount; }
        private BigInteger remainder() { return remainder; }
        private void addOne() { amount += 1L; }
    }
}

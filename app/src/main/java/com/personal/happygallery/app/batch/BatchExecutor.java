package com.personal.happygallery.app.batch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 배치 공통 실행기.
 *
 * <p>조회 → 건별 처리 → 실패 격리 → 집계 패턴을 통일한다.
 * 낙관적 락 충돌은 info 레벨로, 그 외 실패는 warn 레벨로 기록한다.
 */
public final class BatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(BatchExecutor.class);

    private BatchExecutor() {}

    /**
     * 후보 목록을 건별 처리하고 결과를 집계한다.
     *
     * @param candidates  처리 대상 목록
     * @param idExtractor 로그용 ID 추출 함수
     * @param processor   건별 처리 함수 (true=성공, false=스킵)
     * @param label       로그 라벨 (예: "주문 자동환불")
     */
    public static <T> BatchResult execute(List<T> candidates,
                                          Function<T, Object> idExtractor,
                                          Function<T, Boolean> processor,
                                          String label) {
        int processed = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (T candidate : candidates) {
            Object id = idExtractor.apply(candidate);
            try {
                if (processor.apply(candidate)) {
                    log.info("{} 처리 [id={}]", label, id);
                    processed++;
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("{} 충돌로 스킵 [id={}]", label, id);
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            } catch (Exception e) {
                log.warn("{} 실패 [id={}]", label, id, e);
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            }
        }

        return BatchResult.of(processed, failureReasons);
    }
}

package com.personal.happygallery.application.batch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
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
                                          Predicate<T> processor,
                                          String label) {
        int processed = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (T candidate : candidates) {
            Object id = idExtractor.apply(candidate);
            try {
                if (processor.test(candidate)) {
                    log.info("{} 처리 [id={}]", label, id);
                    processed++;
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("{} 충돌로 스킵 [id={}]", label, id, e);
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            } catch (Exception e) {
                log.warn("{} 실패 [id={} type={}]",
                        label, id, e.getClass().getSimpleName(), e);
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            }
        }

        return BatchResult.of(processed, failureReasons);
    }

    /**
     * ID 키셋 기반 배치 실행기.
     *
     * <p>성공 여부와 무관하게 마지막 조회 ID 이후로 이동한다. 앞 페이지의 실패 항목이
     * 조회 결과에 계속 남더라도 뒤쪽 후보가 굶지 않으며, 다음 스케줄 실행에서 실패 항목을 재시도한다.
     * 조회 함수는 반드시 ID 오름차순으로 결과를 반환해야 한다.
     *
     * @param pageFetcher 마지막 조회 ID 이후의 다음 페이지 조회 함수
     * @param idExtractor 커서와 로그에 사용할 ID 추출 함수
     * @param processor   건별 처리 함수 (true=성공, false=스킵)
     * @param label       로그 라벨
     */
    public static <T> BatchResult executeByIdCursor(Function<Long, List<T>> pageFetcher,
                                                     Function<T, Long> idExtractor,
                                                     Predicate<T> processor,
                                                     String label) {
        BatchResult total = BatchResult.successOnly(0);
        long afterId = 0L;
        while (true) {
            List<T> page = pageFetcher.apply(afterId);
            if (page.isEmpty()) {
                return total;
            }

            BatchResult pageResult = execute(page, idExtractor::apply, processor, label);
            total = total.merge(pageResult);
            afterId = idExtractor.apply(page.getLast());
        }
    }
}

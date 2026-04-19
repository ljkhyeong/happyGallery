package com.personal.happygallery.application.batch;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
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

    /**
     * 페이지네이션 배치 실행기.
     *
     * <p>처리 후 상태가 변경되어 결과셋에서 빠지는 mutation 배치용이다.
     * 매 반복마다 첫 페이지를 조회하여 빈 페이지가 나올 때까지 처리한다.
     * 한 페이지에서 성공 건수가 0이면 무한 루프 방지를 위해 종료한다.
     *
     * @param pageFetcher 첫 페이지 조회 함수 (항상 page 0, 고정 크기)
     * @param idExtractor 로그용 ID 추출 함수
     * @param processor   건별 처리 함수 (true=성공, false=스킵)
     * @param label       로그 라벨
     */
    public static <T> BatchResult executePaginated(Supplier<List<T>> pageFetcher,
                                                    Function<T, Object> idExtractor,
                                                    Function<T, Boolean> processor,
                                                    String label) {
        BatchResult total = BatchResult.successOnly(0);
        Set<Object> seenIds = new HashSet<>();
        List<T> page;
        while (!(page = pageFetcher.get()).isEmpty()) {
            List<T> fresh = page.stream()
                    .filter(item -> seenIds.add(idExtractor.apply(item)))
                    .toList();

            if (fresh.isEmpty()) {
                log.warn("{} 남은 항목이 모두 처리 완료 — 루프 종료", label);
                break;
            }
            BatchResult pageResult = execute(fresh, idExtractor, processor, label);
            total = total.merge(pageResult);
            if (pageResult.successCount() == 0) {
                log.warn("{} 페이지 진행 없음 — 루프 종료 (failureCount={})", label, pageResult.failureCount());
                break;
            }
        }
        return total;
    }
}

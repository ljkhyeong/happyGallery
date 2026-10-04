---
name: happygallery-smartstore-flows
description: happyGallery의 네이버 스마트스토어 재고·가격·판매 상태 동기화, 채널 주문 원장·발주·발송·클레임, 문의 답변, 정산 대사·회계 조회를 변경할 때 사용한다. 자사 재고 차감·복원은 product, 자사 주문·클레임은 order, 공통 scheduler는 batch 스킬을 사용한다.
---

# happyGallery 스마트스토어 연동

## 규칙

- `adapter-out-external`의 `external/smartstore/`, application `product/`·`order/`·`qna/`의 `*SmartStore*`, ADR-0047·0048, PRD-0001 스마트스토어 절을 확인한다.
- 내부 재고가 원장이다. 주문 수집(매분 50초)이 내부 재고를 먼저 반영하고 재고 전송(55초)이 절대 수량을 보낸다. 자동·수동 전송 전 `synchronizeBeforeStock()`이 현재 시점까지 끝났는지 확인하고, `MAPPING_REQUIRED`·`STOCK_SHORTAGE`·`STATUS_REVIEW` 주문이 남은 상품은 보류한다.
- 재고를 바꾸는 업무 트랜잭션은 `SmartStoreStockSyncQueuePort.requestIfMapped`로 요청 버전만 올린다. 전송 결과는 선점한 `generation`·요청 버전이 현재와 같을 때만 반영하고, 늦은 응답은 보정 요청으로 남긴다.
- 옵션 재고 전송 직전 원상품을 조회해 현재 옵션가·사용 여부를 함께 보낸다. 생략하면 0원·사용 가능으로 바뀐다. 조회 실패·옵션 누락이면 전체 전송을 중단하고, `retired` 연결에는 항상 0개를 보낸다.
- 3xx는 `smartStoreRestClient`에서 실패로 처리한다. 가격·재고 반영이 실패하면 판매 상태 변경을 호출하지 않는다. 가격·판매 상태는 `previewVersion` 확인 뒤 수동 반영만 하고 자동 재시도하지 않는다. 수동 반영 뒤에는 결과와 관계없이 재고 재전송을 요청한다.
- 토큰은 `NaverCommerceAccessTokenProvider`만 발급·캐시한다. `401`+`GW.AUTHN`일 때만 재발급해 한 번 재전송하고, 최초 발급 실패는 재시도하지 않는다. client secret·access token은 DB·로그에 남기지 않는다.
- 채널 주문은 `smartstore_product_orders` 별도 원장에 두고 자사 주문 상태 머신에 합치지 않는다. 멱등 키는 `productOrderId`, 옵션 연결은 `itemNo`다. 매핑이 없으면 이름으로 추측하지 않고 `MAPPING_REQUIRED`로 남긴다.
- 재고는 `inventory_applied_quantity`와 목표 수량의 차이만 차감·복원한다. 채널 주문 차감은 단일 SKU `tryDeduct*`로 부족을 `STOCK_SHORTAGE`로 저장하고 주문 기록을 롤백하지 않는다. 반품은 관리자 검수 전 복원하지 않는다.
- 주문 명령은 트랜잭션 밖에서 호출하고 호출 전 `REQUESTED` 이력을 남긴다. 토큰 미준비는 `NOT_SENT`, 4xx·명시적 실패는 `REJECTED`, 나머지는 `RESULT_UNKNOWN`이며 자동 재전송하지 않는다. 성공 응답 뒤에도 로컬 상태는 변경 피드가 확정한다. 일괄 요청은 30건 이하로 주문별 이력을 남긴다.
- 배송정보는 `SmartStoreDeliveryInfoProtector`로 `delivery_info_enc`에 암호화하고 관리자 단건 조회에서만 복호화하며 키 회전에 포함한다. 클레임 상세·문의·답변 템플릿·검수·반품 택배사 목록은 실시간 조회만 한다. 회계 응답에서 구매자명·예금주·계좌번호를 뺀다.
- 변경 피드·상품 문의 일시는 Spring URI 변수로 넘겨 `+09:00`을 한 번만 인코딩한다. 로컬 페이지는 0, 네이버 페이지는 1부터 시작한다.
- 주문·정산 선점 시각은 주입한 `Clock` 값을 마이크로초로 절삭한다. 정산 커서는 성공 뒤에만 전진하고 실행당 최대 31일이다. 정산 불일치·회계 조회는 주문·재고·결제 상태와 대사 커서를 바꾸지 않는다.

## 검증

- 재고 전송 버전·백오프는 `./gradlew :application:policyTest --tests "*SmartStoreStockSyncPolicyTest"`를 실행한다.
- HTTP client·설정은 `./gradlew :adapter-out-external:test --tests "*SmartStoreRestClientConfigTest" --tests "*SmartStorePropertiesTest"`를 실행한다.
- `NaverCommerce*ProviderTest`는 application 모듈에 있다. `./gradlew :application:test --tests "*NaverCommerceOrderProviderTest"`처럼 대상만 고른다. 주문 명령은 `DefaultSmartStoreChannelOrderServiceTest`, 정산은 `DefaultSmartStoreSettlementServiceTest`, 문의 기간은 `SmartStoreInquirySearchTest`를 함께 고른다.
- DB·트랜잭션은 `./gradlew --no-daemon :application:useCaseTest --tests "*X*"`로 상품 반영·수집 보류·원상품 변경 `SmartStoreProductSyncUseCaseIT`, 채널 주문 재고·반품 검수·대사 `SmartStoreOrderInventoryUseCaseIT`, 선점·커서 `SmartStoreSyncLeaseUseCaseIT`, 옵션 매핑 `ProductInventoryUseCaseIT`, 배송정보 키 회전 `KeyRotationUseCaseIT` 중 선택한다.
- 관리자 API는 `:adapter-in-web:restDocsTest`의 `AdminAuthCatalogApiRestDocsTest`(정산은 `AdminOperationsApiRestDocsTest`), 화면은 `happygallery-frontend-flows`와 `frontend/tests/e2e/smartstore-admin.spec.ts`, 경보는 `happygallery-observability-flows`를 함께 적용한다. 실제 네이버 쓰기 API는 호출하지 않는다.

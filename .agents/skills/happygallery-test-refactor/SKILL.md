---
name: happygallery-test-refactor
description: happyGallery 테스트의 중복·fixture·helper·가독성을 동작 변경 없이 정리할 때 사용한다. 운영 코드 수정이 주목적이면 해당 도메인 스킬을 사용한다.
---

# happyGallery 테스트 정리

## 규칙

- ADR-0027의 테스트 전략을 따른다. 시나리오와 검증 의미는 보존하되 기존 assertion 형식을 모두 유지할 필요는 없다.
- 같은 의미의 설정·검증이 반복되어 시나리오를 가릴 때만 fixture·builder·helper로 추출한다. 한 facade에 repository 목록만 전달하는 static helper는 만들지 않는다.
- 같은 규칙의 여러 입력은 `@ParameterizedTest`, 다른 업무 시나리오는 별도 `@Test`로 작성한다. 핵심 업무 규칙을 helper 안에 숨기지 않는다.
- 삭제된 guard·도달 불가 분기·프레임워크 동작만 확인하는 테스트는 제거하고, 실제 입력·상태를 검증하는 가까운 테스트를 유지한다.
- DB 정리는 해당 도메인 범위에서 `@AfterEach` 한 번으로 처리한다. `@BeforeEach`는 fixture·mock·MockMvc 설정에 사용한다.
- 공용 fixture의 위치는 `AGENTS.md`를 따르고, 변경 전 `rg`로 모든 소비자를 확인한다.

## 검증

- 수정한 클래스만 `--tests`로 실행한다. `test-support` 변경은 `:test-support:compileTestFixturesJava` 후 실제 소비자의 test·useCaseTest·restDocsTest를 선택한다.
- 테스트 전략이나 남은 작업이 달라질 때만 관련 ADR·HANDOFF를 갱신한다.

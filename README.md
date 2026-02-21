## 문서 관리

docs 폴더 아래에 문서 성격별 카테고리와 주제 단위 폴더를 고정해 관리한다.

*   카테고리:
    *   `docs/Idea` - 아이디어 스케치, 문제 정의, 대략적인 방향
    *   `docs/1Pager` - 이해관계자용 한 장 요약 (목적/대상/핵심 기능/일정/리스크)
    *   `docs/PRD` - 제품 요구사항 상세 스펙 (기능/요구사항/API·화면·정책/비기능)
    *   `docs/POC` - 실험/검증 기록 (가설/방법/결과/결론)
    *   `docs/ADR` - 의사결정 기록 (왜 이 선택을 했는지)
*   주제 폴더 규칙: `docs/<Category>/0001_<topic>` 형태로 번호를 올리며 관리

## 빌드 및 실행

*   빌드: `./gradlew build`
*   실행: `./gradlew :app:bootRun`
*   Health 체크: `http://localhost:8080/actuator/health`
*   DB/환경 설정: `docker compose up -d`

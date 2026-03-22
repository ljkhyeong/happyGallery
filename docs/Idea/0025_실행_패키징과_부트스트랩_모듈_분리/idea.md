# Idea 0025: bootJar 패키징과 bootstrap 모듈 분리 검토

## 배경

현재 실행 진입점은 `app` 모듈의 `com.personal.happygallery.HappygalleryApplication`이다.
`app`은 `common`, `domain`에는 runtime 의존을 가지지만 `infra`에는 test 의존만 가진다.

이 상태에서 `:app:bootJar`를 만들면 실행형 jar는 생성되지만,
jar 내부에는 `app` 클래스와 `common`, `domain` jar만 들어가고 `infra` jar는 포함되지 않는다.

즉, 현재 bootJar는 애플리케이션 서비스와 도메인 모델은 담지만
Repository, 외부 연동, Port 구현체 같은 실제 런타임 어댑터는 담지 못할 수 있다.

## 현재 확인된 사실

- `settings.gradle` 기준 서브모듈은 `app`, `common`, `domain`, `infra` 4개다.
- `app/build.gradle`은 `implementation project(":common")`, `implementation project(":domain")`를 가진다.
- 같은 파일에서 `infra`는 `testImplementation project(":infra")`로만 연결되어 있다.
- `:app:bootJar` 결과물 내부 `BOOT-INF/lib`에는 `common-*.jar`, `domain-*.jar`가 보였고 `infra-*.jar`는 없었다.

## 문제

### 1. 실행용 산출물 책임이 모호하다

As-Is:
- `app` 모듈이 Spring Boot 진입점을 가지므로 실행 모듈처럼 보인다.
- 하지만 실제 런타임 구현체 다수는 `infra`에 있다.

To-Be:
- 최종 실행 산출물을 누가 책임지는가를 Gradle 구조와 의존 관계에서 분명히 드러낸다.

### 2. bootJar 하나만으로 배포 가능한지 판단이 어렵다

As-Is:
- `app` bootJar가 만들어지므로 겉보기에는 단독 배포가 가능해 보인다.
- 하지만 런타임에 필요한 Port 구현체가 classpath에 모두 포함되는지 보장되지 않는다.

To-Be:
- bootJar 하나만으로 배포 가능하다는 사실이 구조적으로 보장되도록 만든다.

### 3. 모듈 경계와 실행 조립 책임이 섞여 있다

As-Is:
- `app`은 유스케이스/컨트롤러/설정/실행 진입점을 함께 가진다.
- `infra`는 구현체를 제공하지만, 최종 조립 경계는 별도 모듈로 분리돼 있지 않다.

To-Be:
- 실행 조립은 별도 bootstrap 모듈이 담당하고, `app`과 `infra`는 각자의 책임에 집중한다.

## 제안

### 옵션 A. bootstrap 모듈 추가

- 새 실행 모듈(예: `bootstrap`)을 만들고 `@SpringBootApplication`을 그쪽으로 이동한다.
- `bootstrap`이 `app`, `infra`, `domain`, `common`에 의존하게 한다.
- 최종 `bootJar`는 bootstrap 모듈에서만 만든다.

장점:
- 실행 조립 책임이 분명하다.
- `app`은 유스케이스/웹 경계, `infra`는 구현체라는 역할이 명확해진다.
- 라이브러리 모듈과 실행 모듈의 구분이 Gradle 구조에 드러난다.

주의:
- 패키지 scan 범위와 설정 클래스 위치를 함께 점검해야 한다.
- 배포/로컬 실행 명령과 문서도 같이 맞춰야 한다.

### 옵션 B. app을 계속 실행 모듈로 유지

- `app`이 실제로 `infra`를 runtime classpath에 포함하도록 구조를 재조정한다.
- 다만 `infra -> app` 의존이 이미 있는 상태라면 순환 의존 위험을 먼저 해소해야 한다.

장점:
- 모듈 수가 늘지 않는다.

주의:
- 조립 책임이 계속 `app`에 남는다.
- 장기적으로 헥사고날 경계와 실행 조립이 다시 섞일 가능성이 있다.

## 현재 판단

현재 상태라면 bootstrap 모듈 분리가 더 안전한 방향이다.

이유:

- 실행 조립 책임과 유스케이스 책임을 분리할 수 있다.
- 최종 bootJar에 어떤 모듈이 반드시 포함돼야 하는지 구조적으로 설명 가능하다.
- 멀티 모듈에서 흔한 라이브러리 모듈 + 실행 모듈 구성을 따르기 쉽다.

다만 아직 구현된 결정은 아니므로 ADR이 아니라 `Idea`에 두는 것이 맞다.

## ADR 승격 조건

아래 조건을 만족하면 `Idea`가 아니라 `ADR`로 승격하는 편이 맞다.

1. 실행 전용 bootstrap 모듈 추가를 실제로 채택했을 때
2. `bootJar` 생성 책임을 특정 모듈 하나로 확정했을 때
3. 로컬 실행, 테스트, 배포 문서를 새 구조에 맞게 함께 정리했을 때
4. `app`/`infra`의 런타임 의존 관계를 팀 규칙으로 설명할 수 있게 됐을 때

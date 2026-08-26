# ─────────────────────────────────────────────────────────────
# HaruVerse 백엔드 (Spring Boot 3.4 / Java 21)
#
# 멀티스테이지 빌드:
#   ① build  — JDK + Gradle 로 jar 를 만든다 (무겁다)
#   ② run    — JRE 에 jar 만 복사한다 (가볍다)
# 최종 이미지에 JDK·Gradle·소스가 남지 않아 크기가 크게 줄어든다.
# ─────────────────────────────────────────────────────────────

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# ★의존성 파일을 먼저 복사하는 이유★
#   소스만 고쳤을 때 의존성 다운로드를 다시 하지 않게 하려고.
#   Docker 는 레이어 단위로 캐시하는데, 앞 레이어가 그대로면 뒤도 재사용한다.
#   src 를 먼저 복사하면 코드 한 줄만 고쳐도 의존성부터 전부 다시 받는다.
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

# Windows 에서 복사된 gradlew 는 실행 권한이 없다 → 붙여준다
RUN chmod +x gradlew

# 의존성만 미리 받아 캐시 레이어로 굳힌다.
# (실패해도 다음 단계에서 다시 받으므로 빌드를 멈추지 않는다)
RUN ./gradlew dependencies --no-daemon || true

COPY src src

# ★테스트를 건너뛰는 이유★
#   테스트는 CI 나 로컬에서 이미 돌린다. 이미지 빌드 때마다 돌리면
#   빌드가 몇 배로 느려지고, 외부 API 상태에 따라 이미지가 안 만들어질 수도 있다.
RUN ./gradlew bootJar --no-daemon -x test


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# H2 파일 DB 가 저장될 자리 (compose 에서 볼륨을 붙인다)
RUN mkdir -p /app/data

# 빌드 단계에서 만든 jar 만 가져온다.
# 이름을 고정하지 않고 와일드카드로 받아 version 이 바뀌어도 그대로 동작하게.
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 안에서는 8080 을 그대로 쓴다. 외부 포트 매핑은 compose 가 담당.
ENTRYPOINT ["java", "-jar", "app.jar"]

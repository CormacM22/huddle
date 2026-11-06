# ---- Build stage: full JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy build files first so the dependency layer is cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests already run in CI on three OSes; the image build only packages.
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system huddle && useradd --system --gid huddle huddle
COPY --from=build /app/target/huddle-*.jar app.jar
USER huddle

EXPOSE 8080
# Respect container memory limits rather than host RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

# ---- Build stage: full JDK + Maven, produces the fat jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Copy the wrapper + pom first so dependency resolution is cached unless the
# pom changes (Docker layer caching).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as an unprivileged user — a container compromise shouldn't be root.
RUN groupadd --system roost && useradd --system --gid roost roost
USER roost

COPY --from=build /build/target/roost-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

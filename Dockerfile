# Render has no native Java runtime (only Node/Bun, Python, Ruby, Go, Rust, Elixir) - Java/Kotlin/Scala go
# through a Docker image instead. This builds both deployable jars (api, db-migration) in one multi-stage
# build and ships only the runtime JRE + the two jars, not the JDK/Maven toolchain, into the final image.

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy just the reactor's pom.xml files first so the dependency-resolution layer only invalidates when a
# pom actually changes, not on every source edit - keeps Render's Docker layer cache useful across deploys.
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY shared/pom.xml shared/pom.xml
COPY db-migration/pom.xml db-migration/pom.xml
COPY api/pom.xml api/pom.xml
RUN chmod +x mvnw && ./mvnw -q -B -pl shared,db-migration,api dependency:go-offline || true

COPY shared shared
COPY db-migration db-migration
COPY api api
RUN ./mvnw -q -B -pl shared,db-migration,api -am package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/db-migration/target/pavitra-db-migration-*-exec.jar db-migration.jar
COPY --from=build /workspace/api/target/pavitra-api-*.jar api.jar
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["./docker-entrypoint.sh"]

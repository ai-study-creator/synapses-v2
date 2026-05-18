FROM gradle:9.5.1-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew clean :services:mcp-devto:installDist :services:orchestrator:installDist --no-daemon

FROM eclipse-temurin:21.0.2_13-jre
WORKDIR /app
COPY --from=build /workspace/services/orchestrator/build/install/orchestrator ./services/orchestrator
COPY --from=build /workspace/services/mcp-devto/build/install/mcp-devto ./services/mcp-devto
EXPOSE 8000
ENV PORT=8000
CMD ["./services/orchestrator/bin/orchestrator"]

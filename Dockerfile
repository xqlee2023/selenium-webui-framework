# =====================================================================
# Docker image for running Selenium WebUI tests
# =====================================================================
FROM maven:3.9-eclipse-temurin-17-alpine

WORKDIR /app

# Install Chrome + dependencies
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    fontconfig \
    freetype \
    ttf-freefont \
    ttf-dejavu

# Copy project files
COPY pom.xml .
COPY src ./src

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B

# Default command: run tests
CMD ["mvn", "clean", "test", "-Pdev", "-Dbrowser=chrome", "-Dheadless=true"]

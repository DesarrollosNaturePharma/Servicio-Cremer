# ==========================================
# STAGE 1: Build
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven (para cachear dependencias)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Dar permisos de ejecución al Maven Wrapper (soluciona Permission denied)
RUN chmod +x mvnw

# Descargar dependencias (capa cacheada)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Construir la aplicación
RUN ./mvnw clean package -DskipTests -B


# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Necesario para HEALTHCHECK (wget no viene siempre por defecto en alpine)
RUN apk add --no-cache wget

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR desde el builder
COPY --from=builder /app/target/*.jar app.jar

# Cambiar propietario
RUN chown spring:spring app.jar

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8600

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8600/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

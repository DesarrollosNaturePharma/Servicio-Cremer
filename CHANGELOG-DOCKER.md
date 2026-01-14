# Changelog - Dockerización y Pipeline CI/CD

## Fecha: 2024-12-24

### Resumen

Se ha dockerizado completamente la aplicación Spring Boot y se ha creado un pipeline de Jenkins para despliegue automático en el servidor Ubuntu de producción. Además, se migró la configuración de MySQL a PostgreSQL.

---

## Archivos Creados

### 1. Dockerfile
- **Ubicación**: `/Dockerfile`
- **Descripción**: Multi-stage build con Java 17 (eclipse-temurin)
- **Características**:
  - Stage 1: Build usando Maven wrapper
  - Stage 2: Runtime ligero con JRE Alpine
  - Usuario no-root para seguridad
  - Health check integrado
  - Expone puerto 8600
  - Perfil de producción activado por defecto

### 2. docker-compose.yml
- **Ubicación**: `/docker-compose.yml`
- **Descripción**: Configuración de producción
- **Características**:
  - Solo incluye la aplicación (PostgreSQL ya existe en servidor)
  - Conecta a red externa `cremer-network`
  - Variables de entorno configurables
  - Health check
  - Restart policy: `unless-stopped`
  - Logging con rotación

### 3. docker-compose.dev.yml
- **Ubicación**: `/docker-compose.dev.yml`
- **Descripción**: Entorno completo de desarrollo local
- **Características**:
  - Incluye PostgreSQL 15
  - Incluye pgAdmin para administración
  - Puerto de debug remoto (5005)
  - Volúmenes persistentes para datos
  - Red interna aislada

### 4. Jenkinsfile
- **Ubicación**: `/Jenkinsfile`
- **Descripción**: Pipeline declarativo para CI/CD
- **Stages**:
  1. Checkout del código
  2. Preparación del directorio de deploy
  3. Transferencia de archivos via SCP
  4. Build de imagen Docker EN EL SERVIDOR (importante para ARM Jenkins)
  5. Deploy con docker-compose
  6. Health check con reintentos
- **Características**:
  - Manejo de errores robusto
  - Logs detallados en caso de fallo
  - Cleanup automático del workspace
  - Variables de entorno configurables

### 5. .dockerignore
- **Ubicación**: `/.dockerignore`
- **Descripción**: Optimización del contexto de build
- **Excluye**:
  - Archivos de IDEs (.idea, .vscode)
  - Git (.git)
  - Target y builds previos
  - Tests
  - Documentación
  - Archivos temporales

### 6. DEPLOY-README.md
- **Ubicación**: `/DEPLOY-README.md`
- **Descripción**: Documentación completa de despliegue
- **Contenido**:
  - Guía de configuración del servidor
  - Configuración de Jenkins
  - Arquitectura del pipeline
  - Variables de entorno
  - Comandos útiles
  - Troubleshooting completo
  - Guía de migración MySQL → PostgreSQL

### 7. QUICK-START.md
- **Ubicación**: `/QUICK-START.md`
- **Descripción**: Guía rápida de comandos esenciales
- **Contenido**:
  - Configuración inicial
  - Comandos comunes
  - Troubleshooting rápido
  - URLs importantes
  - Checklist de despliegue

### 8. setup-server.sh
- **Ubicación**: `/setup-server.sh`
- **Descripción**: Script de configuración automática del servidor
- **Funciones**:
  - Verifica instalación de Docker
  - Crea red Docker `cremer-network`
  - Conecta PostgreSQL a la red
  - Crea base de datos y usuario
  - Crea estructura de directorios
  - Configura firewall (opcional)

### 9. test-local-build.sh
- **Ubicación**: `/test-local-build.sh`
- **Descripción**: Script para probar el build localmente
- **Funciones**:
  - Construye la imagen Docker
  - Verifica el tamaño de la imagen
  - Inspecciona capas
  - Verifica que el JAR sea ejecutable
  - Prueba de inicio sin BD

---

## Archivos Modificados

### 1. pom.xml
**Cambios**:
- ✅ Agregada dependencia `postgresql` driver
- ✅ Agregada dependencia `spring-boot-starter-actuator` para health checks
- ✅ Mantenida dependencia `mysql-connector-j` para desarrollo

**Líneas agregadas**:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. src/main/resources/application-prod.properties
**Cambios**:
- ✅ Migrado de MySQL a PostgreSQL
- ✅ Configuración con variables de entorno
- ✅ Cambiado dialecto a `PostgreSQLDialect`
- ✅ Agregada configuración de Actuator para health checks

**Configuración PostgreSQL**:
```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://postgres-cremer:5432/cremer_db}
spring.datasource.username=${DB_USERNAME:cremer_user}
spring.datasource.password=${DB_PASSWORD:CremerP@ssw0rd2024!}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Configuración Actuator**:
```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

---

## Configuración de Jenkins

### Credenciales Necesarias
- **ID**: `ubuntu-server-ssh`
- **Tipo**: SSH Username with private key
- **Username**: `administrador`
- **Private Key**: Clave privada SSH del servidor Ubuntu

### Job Configuration
- **Tipo**: Pipeline Job
- **Nombre sugerido**: `cremer-service-deploy`
- **Source**: Pipeline script from SCM
- **SCM**: Git
- **Branch**: `*/main`
- **Script Path**: `Jenkinsfile`
- **Trigger**: Manual (Build Now)

---

## Configuración del Servidor Ubuntu

### Información del Servidor
- **IP**: 192.168.10.135
- **Usuario SSH**: administrador
- **Directorio deploy**: `/home/administrador/cremer-service`
- **Red Docker**: `cremer-network`

### Requisitos
1. Docker instalado
2. docker-compose instalado
3. PostgreSQL corriendo en contenedor
4. Red Docker `cremer-network` creada
5. Base de datos `cremer_db` creada
6. Usuario `cremer_user` con permisos

### Base de Datos PostgreSQL
- **Host**: `postgres-cremer` (nombre del contenedor)
- **Puerto**: 5432
- **Base de datos**: `cremer_db`
- **Usuario**: `cremer_user`
- **Password**: `CremerP@ssw0rd2024!`

---

## Flujo de Despliegue

```
1. Desarrollador hace commit y push
          ↓
2. Ejecuta "Build Now" en Jenkins
          ↓
3. Jenkins: Checkout del código
          ↓
4. Jenkins: Transfiere archivos al servidor Ubuntu via SCP
          ↓
5. Jenkins: SSH al servidor y construye imagen Docker
          ↓
6. Jenkins: Detiene contenedor anterior
          ↓
7. Jenkins: Inicia nuevo contenedor con docker-compose
          ↓
8. Jenkins: Verifica health check
          ↓
9. Aplicación desplegada y funcionando
```

---

## Migración MySQL → PostgreSQL

### Cambios de Base de Datos

| Aspecto | MySQL | PostgreSQL |
|---------|-------|------------|
| **Driver** | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` |
| **Dialecto** | `MySQL8Dialect` | `PostgreSQLDialect` |
| **URL** | `jdbc:mysql://...` | `jdbc:postgresql://...` |
| **Puerto** | 3309 | 5432 |

### Herramientas de Migración
- **pgLoader**: Migración automática (recomendado)
- **mysqldump + psql**: Migración manual

---

## Variables de Entorno Configurables

| Variable | Valor por Defecto | Descripción |
|----------|-------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Perfil de Spring Boot |
| `DB_URL` | `jdbc:postgresql://postgres-cremer:5432/cremer_db` | URL de PostgreSQL |
| `DB_USERNAME` | `cremer_user` | Usuario de BD |
| `DB_PASSWORD` | `CremerP@ssw0rd2024!` | Contraseña de BD |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | Opciones JVM |
| `TZ` | `Europe/Madrid` | Zona horaria |

---

## Endpoints Disponibles

| Endpoint | URL | Descripción |
|----------|-----|-------------|
| **API Base** | http://192.168.10.135:8600 | API REST |
| **Health** | http://192.168.10.135:8600/actuator/health | Estado de salud |
| **Swagger** | http://192.168.10.135:8600/swagger-ui.html | Documentación API |
| **Metrics** | http://192.168.10.135:8600/actuator/metrics | Métricas |
| **Info** | http://192.168.10.135:8600/actuator/info | Información |

---

## Seguridad

### Mejoras Implementadas
- ✅ Usuario no-root en contenedor Docker
- ✅ Multi-stage build (reduce superficie de ataque)
- ✅ Variables de entorno para secretos
- ✅ Health checks para monitoreo
- ✅ Logging con rotación

### Recomendaciones Pendientes
- ⚠️ Cambiar contraseñas por defecto
- ⚠️ Usar Docker Secrets en lugar de variables de entorno
- ⚠️ Configurar HTTPS/TLS
- ⚠️ Implementar rate limiting
- ⚠️ Revisar logs periódicamente

---

## Testing

### Desarrollo Local
```bash
# Probar build localmente
./test-local-build.sh

# Levantar entorno completo de desarrollo
docker-compose -f docker-compose.dev.yml up -d

# Ver logs
docker-compose -f docker-compose.dev.yml logs -f
```

### Producción
```bash
# Verificar salud de la aplicación
curl http://192.168.10.135:8600/actuator/health

# Ver logs
ssh administrador@192.168.10.135
cd /home/administrador/cremer-service
docker-compose logs -f
```

---

## Rollback

### Proceso de Rollback
1. **Opción 1**: Re-ejecutar build anterior exitoso en Jenkins
2. **Opción 2**: Usar imagen Docker anterior con tags
3. **Opción 3**: Revertir commit en Git y re-desplegar

---

## Próximos Pasos Sugeridos

1. ✅ **Completado**: Dockerización de la aplicación
2. ✅ **Completado**: Pipeline de Jenkins
3. ✅ **Completado**: Migración a PostgreSQL
4. ⏳ **Pendiente**: Ejecutar migración de datos MySQL → PostgreSQL
5. ⏳ **Pendiente**: Configurar backups automáticos de PostgreSQL
6. ⏳ **Pendiente**: Implementar monitoring (Prometheus/Grafana)
7. ⏳ **Pendiente**: Configurar alertas
8. ⏳ **Pendiente**: Implementar HTTPS
9. ⏳ **Pendiente**: Crear tests de integración

---

## Notas Importantes

### Jenkins ARM → Ubuntu x86_64
El servidor Jenkins corre en un NAS Synology ARM, pero el servidor de producción es x86_64. Por esto, el build de la imagen Docker se realiza **EN EL SERVIDOR UBUNTU**, no en Jenkins. Esto evita problemas de incompatibilidad de arquitectura.

### Network Docker
La aplicación y PostgreSQL deben estar en la misma red Docker (`cremer-network`) para comunicarse. Si PostgreSQL tiene otro nombre de contenedor, actualizar la variable `DB_URL` en `docker-compose.yml`.

### Variables de Entorno
Las contraseñas están en texto plano en `docker-compose.yml`. Para producción, se recomienda usar Docker Secrets o un gestor de secretos externo (Vault, etc.).

---

## Contacto y Soporte

Para problemas relacionados con:
- **Dockerización**: Consultar `DEPLOY-README.md`
- **Inicio rápido**: Consultar `QUICK-START.md`
- **Problemas**: Sección Troubleshooting en `DEPLOY-README.md`

---

## Changelog de Versiones

### v1.0.0 - 2024-12-24
- ✅ Dockerización completa
- ✅ Pipeline de Jenkins
- ✅ Migración a PostgreSQL
- ✅ Documentación completa
- ✅ Scripts de automatización

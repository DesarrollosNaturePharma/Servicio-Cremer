# Cremer Service - API REST con Spring Boot

Aplicación Spring Boot 3.3.5 con Java 17 para gestión de datos, dockerizada y con pipeline CI/CD automatizado.

## Información del Proyecto

- **Versión**: 0.0.1-SNAPSHOT
- **Framework**: Spring Boot 3.3.5
- **Java**: 17 (Eclipse Temurin)
- **Base de Datos**: PostgreSQL 15
- **Puerto**: 8600
- **Servidor Producción**: Ubuntu 192.168.10.135

## Estado del Proyecto

- ✅ Aplicación Spring Boot funcional
- ✅ Dockerización completa (multi-stage build)
- ✅ Pipeline de Jenkins configurado
- ✅ Migración a PostgreSQL
- ✅ Documentación completa

## Inicio Rápido

### Para Usuarios Nuevos

Si es tu primera vez configurando este proyecto, sigue esta guía:

1. **[FIRST-TIME-SETUP.md](FIRST-TIME-SETUP.md)** - Configuración inicial paso a paso

### Para Desarrolladores

```bash
# Clonar el repositorio
git clone [URL-REPOSITORIO]
cd Cremer-Service

# Levantar entorno de desarrollo local (incluye PostgreSQL)
docker-compose -f docker-compose.dev.yml up -d

# Ver logs
docker-compose -f docker-compose.dev.yml logs -f

# Acceder a la aplicación
open http://localhost:8600/swagger-ui.html
```

### Para Administradores del Servidor

```bash
# Configurar servidor (primera vez)
./setup-server.sh

# Desplegar/Actualizar aplicación
# Ir a Jenkins → cremer-service-deploy → Build Now

# Ver estado en producción
ssh administrador@192.168.10.135
cd /home/administrador/cremer-service
docker-compose ps
docker-compose logs -f
```

## Documentación

### Guías de Instalación y Configuración

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| **[FIRST-TIME-SETUP.md](FIRST-TIME-SETUP.md)** | Configuración inicial completa paso a paso | Todos (primera vez) |
| **[QUICK-START.md](QUICK-START.md)** | Comandos rápidos y uso diario | Todos |
| **[DEPLOY-README.md](DEPLOY-README.md)** | Documentación completa de despliegue | DevOps/Sysadmin |

### Documentación Técnica

| Documento | Descripción |
|-----------|-------------|
| **[CHANGELOG-DOCKER.md](CHANGELOG-DOCKER.md)** | Detalles de dockerización y cambios realizados |
| **[Dockerfile](Dockerfile)** | Multi-stage build con Java 17 |
| **[Jenkinsfile](Jenkinsfile)** | Pipeline declarativo CI/CD |
| **[docker-compose.yml](docker-compose.yml)** | Configuración de producción |
| **[docker-compose.dev.yml](docker-compose.dev.yml)** | Entorno de desarrollo completo |

### Scripts Útiles

| Script | Descripción | Uso |
|--------|-------------|-----|
| **[setup-server.sh](setup-server.sh)** | Configuración automática del servidor | Ejecutar en servidor Ubuntu |
| **[test-local-build.sh](test-local-build.sh)** | Prueba de build Docker localmente | Ejecutar antes de commit |

## Arquitectura

### Tecnologías Principales

- **Backend**: Spring Boot 3.3.5
- **Java**: 17 (Eclipse Temurin JRE/JDK)
- **Base de Datos**: PostgreSQL 15
- **ORM**: Hibernate/JPA
- **Seguridad**: Spring Security
- **WebSocket**: Spring WebSocket + STOMP
- **Documentación API**: SpringDoc OpenAPI (Swagger)
- **Build**: Maven
- **Containerización**: Docker (multi-stage build)
- **CI/CD**: Jenkins (pipeline declarativo)

### Componentes

```
┌─────────────────────────────────────────────────────┐
│                  Jenkins (NAS ARM)                   │
│  - Checkout código                                   │
│  - Transferir archivos via SCP                       │
│  - SSH al servidor para build y deploy               │
└──────────────────────┬──────────────────────────────┘
                       │
                       │ SSH/SCP
                       ▼
┌─────────────────────────────────────────────────────┐
│         Servidor Ubuntu (192.168.10.135)             │
│  ┌─────────────────────────────────────────────┐   │
│  │  Docker Network: cremer-network              │   │
│  │  ┌─────────────────┐  ┌──────────────────┐ │   │
│  │  │ PostgreSQL      │  │ Cremer Service   │ │   │
│  │  │ postgres-cremer │  │ cremer-service   │ │   │
│  │  │ Port: 5432      │  │ Port: 8600       │ │   │
│  │  └─────────────────┘  └──────────────────┘ │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Endpoints

### API REST

- **Base URL**: http://192.168.10.135:8600
- **Swagger UI**: http://192.168.10.135:8600/swagger-ui.html
- **API Docs**: http://192.168.10.135:8600/v3/api-docs

### Actuator (Monitoreo)

- **Health**: http://192.168.10.135:8600/actuator/health
- **Info**: http://192.168.10.135:8600/actuator/info
- **Metrics**: http://192.168.10.135:8600/actuator/metrics

### WebSocket

- **Endpoint**: ws://192.168.10.135:8600/ws
- **Topics**: `/topic/*`, `/queue/*`
- **App Prefix**: `/app`

## Desarrollo

### Requisitos de Desarrollo

- Java 17+
- Maven 3.6+
- Docker y docker-compose (para desarrollo local)
- IDE con soporte para Spring Boot (IntelliJ IDEA, Eclipse, VS Code)

### Configuración Local

```bash
# Clonar repositorio
git clone [URL-REPOSITORIO]
cd Cremer-Service

# Levantar PostgreSQL y aplicación
docker-compose -f docker-compose.dev.yml up -d

# O ejecutar directamente con Maven (requiere PostgreSQL instalado)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Probar Build Docker

```bash
# Ejecutar script de prueba
chmod +x test-local-build.sh
./test-local-build.sh
```

### Estructura del Proyecto

```
Cremer-Service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/rnp/cremer/
│   │   │       ├── config/          # Configuraciones
│   │   │       ├── controller/      # Controllers REST
│   │   │       ├── model/           # Entidades JPA
│   │   │       ├── repository/      # Repositorios
│   │   │       └── service/         # Lógica de negocio
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
├── Dockerfile                        # Multi-stage build
├── docker-compose.yml                # Producción
├── docker-compose.dev.yml            # Desarrollo
├── Jenkinsfile                       # Pipeline CI/CD
├── pom.xml                           # Dependencias Maven
└── [Documentación]/
```

## Despliegue

### Pipeline Automático (Recomendado)

1. Hacer commit y push de cambios
2. Ir a Jenkins
3. Ejecutar "Build Now" en job `cremer-service-deploy`
4. Monitorear el progreso
5. Verificar que el despliegue fue exitoso

### Manual (Solo si es necesario)

```bash
# SSH al servidor
ssh administrador@192.168.10.135

# Ir al directorio del proyecto
cd /home/administrador/cremer-service

# Detener contenedor
docker-compose down

# Reconstruir imagen
docker build -t cremer-service:latest .

# Iniciar contenedor
docker-compose up -d

# Verificar logs
docker-compose logs -f
```

## Monitoreo

### Ver Estado

```bash
# En el servidor
docker-compose ps
docker-compose logs -f
docker stats cremer-service
```

### Health Check

```bash
curl http://192.168.10.135:8600/actuator/health
```

Respuesta esperada:
```json
{
  "status": "UP"
}
```

## Troubleshooting

### La aplicación no inicia

```bash
# Ver logs completos
docker-compose logs --tail=200 cremer-app

# Verificar conexión a PostgreSQL
docker exec cremer-service ping postgres-cremer

# Verificar variables de entorno
docker exec cremer-service env | grep DB
```

### Error de conexión a base de datos

```bash
# Verificar que PostgreSQL está corriendo
docker ps | grep postgres

# Verificar red Docker
docker network inspect cremer-network

# Ver logs de PostgreSQL
docker logs postgres-cremer
```

### Pipeline de Jenkins falla

1. Ver Console Output del build en Jenkins
2. Verificar credenciales SSH
3. Verificar conectividad de red
4. Revisar logs en el servidor

Para más detalles: [DEPLOY-README.md](DEPLOY-README.md) - Sección Troubleshooting

## Seguridad

### Recomendaciones

- ✅ Usuario no-root en contenedor
- ✅ Multi-stage build
- ✅ Health checks activos
- ⚠️ Cambiar contraseñas por defecto
- ⚠️ Configurar HTTPS/TLS
- ⚠️ Implementar rate limiting
- ⚠️ Revisar logs regularmente

### Contraseñas

Las contraseñas están en `docker-compose.yml`. Para producción:

1. Cambiar contraseñas por defecto
2. Usar Docker Secrets o gestor de secretos externo
3. No hacer commit de archivos con credenciales

## Migración desde MySQL

Si tienes datos en MySQL:

```bash
# Instalar pgLoader
sudo apt-get install pgloader

# Ejecutar migración
pgloader mysql://rnp_prod:PASSWORD@localhost:3309/cremer_db \
          postgresql://cremer_user:PASSWORD@localhost:5432/cremer_db
```

Ver guía completa: [DEPLOY-README.md](DEPLOY-README.md) - Sección Migración

## Backups

### PostgreSQL

```bash
# Backup manual
docker exec postgres-cremer pg_dump -U cremer_user cremer_db | gzip > backup.sql.gz

# Restaurar
gunzip < backup.sql.gz | docker exec -i postgres-cremer psql -U cremer_user -d cremer_db
```

### Backups Automáticos

Ver configuración de cron en [FIRST-TIME-SETUP.md](FIRST-TIME-SETUP.md) - Sección 5.2

## Contacto y Soporte

- **Documentación Técnica**: Ver archivos *.md en el repositorio
- **Issues**: [URL del sistema de issues si aplica]
- **DevOps/Sysadmin**: Consultar DEPLOY-README.md

## Licencia

[Especificar licencia del proyecto]

---

**Última actualización**: 2024-12-24
**Versión**: 0.0.1-SNAPSHOT
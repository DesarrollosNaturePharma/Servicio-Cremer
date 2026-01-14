# Guía de Despliegue - Cremer Service

## Resumen de la Configuración

Esta aplicación Spring Boot ha sido dockerizada y configurada para despliegue automático mediante Jenkins.

### Información General
- **Aplicación**: Spring Boot 3.3.5 con Java 17
- **Puerto**: 8600
- **Base de datos**: PostgreSQL (migrado desde MySQL)
- **Servidor de producción**: Ubuntu 192.168.10.135
- **Usuario SSH**: administrador
- **Directorio de despliegue**: /home/administrador

---

## Preparación del Servidor Ubuntu (192.168.10.135)

### 1. Configurar PostgreSQL

El contenedor PostgreSQL debe estar corriendo y conectado a la red `cremer-network`:

```bash
# Crear la red Docker si no existe
docker network create cremer-network

# Verificar que PostgreSQL está en la red correcta
# Si tienes PostgreSQL en un contenedor llamado 'postgres-cremer':
docker network connect cremer-network postgres-cremer

# O si PostgreSQL está en otro nombre, ajusta el comando
```

### 2. Crear la Base de Datos y Usuario

Conectarse a PostgreSQL y ejecutar:

```sql
-- Crear la base de datos
CREATE DATABASE cremer_db;

-- Crear el usuario
CREATE USER cremer_user WITH PASSWORD 'CremerP@ssw0rd2024!';

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE cremer_db TO cremer_user;

-- Conectarse a la base de datos
\c cremer_db

-- Otorgar privilegios en el schema public
GRANT ALL PRIVILEGES ON SCHEMA public TO cremer_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cremer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cremer_user;

-- Para PostgreSQL 15+, también necesitas:
ALTER DATABASE cremer_db OWNER TO cremer_user;
```

### 3. Verificar Configuración de Red

```bash
# Listar redes Docker
docker network ls

# Inspeccionar la red cremer-network
docker network inspect cremer-network

# Verificar que PostgreSQL está en la red
docker inspect postgres-cremer | grep NetworkMode
```

### 4. Ajustar docker-compose.yml (Si es necesario)

Si PostgreSQL NO está en la red `cremer-network`, puedes usar una de estas alternativas:

**Opción A: Usar host network**

Editar `docker-compose.yml`:

```yaml
services:
  cremer-app:
    # ... otras configuraciones ...
    network_mode: host
    environment:
      - DB_URL=jdbc:postgresql://localhost:5432/cremer_db
    # Eliminar la sección 'networks'
```

**Opción B: Usar IP del servidor**

```yaml
environment:
  - DB_URL=jdbc:postgresql://192.168.10.135:5432/cremer_db
```

---

## Configuración de Jenkins

### 1. Crear las Credenciales SSH

En Jenkins, ir a: **Manage Jenkins > Credentials > System > Global credentials**

1. Click en "Add Credentials"
2. Kind: **SSH Username with private key**
3. ID: `ubuntu-server-ssh`
4. Username: `administrador`
5. Private Key: Agregar la clave privada SSH para acceder al servidor Ubuntu
6. Guardar

### 2. Crear el Pipeline Job

1. En Jenkins, crear un nuevo **Pipeline Job**
2. Nombre sugerido: `cremer-service-deploy`
3. En la sección **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: [URL de tu repositorio]
   - Branch: `*/main` (o la rama que uses)
   - Script Path: `Jenkinsfile`

### 3. Configurar Trigger Manual

El pipeline está configurado para ejecución manual (Build Now). No requiere configuración adicional de triggers.

---

## Arquitectura del Pipeline de Jenkins

El Jenkinsfile implementa el siguiente flujo:

### Stages:

1. **Checkout**: Descarga el código fuente del repositorio
2. **Prepare Deploy Directory**: Crea directorios y verifica la red Docker en el servidor
3. **Transfer Files**: Transfiere los archivos necesarios al servidor Ubuntu
4. **Build Docker Image**: Construye la imagen Docker EN EL SERVIDOR (importante porque Jenkins está en ARM)
5. **Deploy**: Detiene el contenedor anterior y despliega el nuevo
6. **Health Check**: Verifica que la aplicación esté funcionando correctamente

### Post-Actions:
- **Success**: Muestra URLs de acceso
- **Failure**: Muestra logs de error
- **Always**: Limpia el workspace de Jenkins

---

## Estructura de Archivos Creados

```
Cremer-Service/
├── Dockerfile                      # Multi-stage build con Java 17
├── docker-compose.yml              # Configuración de producción
├── Jenkinsfile                     # Pipeline declarativo
├── .dockerignore                   # Archivos excluidos del build
├── pom.xml                         # Actualizado con driver PostgreSQL
└── src/main/resources/
    └── application-prod.properties # Configuración PostgreSQL
```

---

## Variables de Entorno

Las siguientes variables están configuradas en `docker-compose.yml`:

| Variable | Valor por Defecto | Descripción |
|----------|-------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Perfil de Spring Boot |
| `DB_URL` | `jdbc:postgresql://postgres-cremer:5432/cremer_db` | URL de PostgreSQL |
| `DB_USERNAME` | `cremer_user` | Usuario de base de datos |
| `DB_PASSWORD` | `CremerP@ssw0rd2024!` | Contraseña de base de datos |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | Opciones de JVM |
| `TZ` | `Europe/Madrid` | Zona horaria |

### Modificar Variables

Editar el archivo `docker-compose.yml` en el servidor:

```bash
cd /home/administrador/cremer-service
nano docker-compose.yml
```

Luego reiniciar el servicio:

```bash
docker-compose down
docker-compose up -d
```

---

## Comandos Útiles en el Servidor

### Verificar Estado del Contenedor

```bash
cd /home/administrador/cremer-service
docker-compose ps
```

### Ver Logs

```bash
# Logs en tiempo real
docker-compose logs -f

# Últimas 100 líneas
docker-compose logs --tail=100

# Logs de un servicio específico
docker-compose logs -f cremer-app
```

### Reiniciar la Aplicación

```bash
docker-compose restart
```

### Detener la Aplicación

```bash
docker-compose down
```

### Iniciar la Aplicación

```bash
docker-compose up -d
```

### Reconstruir y Desplegar

```bash
# Detener contenedor
docker-compose down

# Reconstruir imagen (si se hacen cambios locales)
docker build -t cremer-service:latest .

# Iniciar
docker-compose up -d
```

### Ver Recursos Utilizados

```bash
docker stats cremer-service
```

### Acceder al Contenedor

```bash
docker exec -it cremer-service sh
```

---

## Migración de Datos MySQL a PostgreSQL

### Opción 1: Usar pgLoader (Recomendado)

```bash
# Instalar pgLoader en el servidor
sudo apt-get install pgloader

# Crear archivo de configuración
nano mysql-to-postgres.load
```

Contenido del archivo:

```
LOAD DATABASE
     FROM mysql://rnp_prod:RnpPr0dP@ssw0rd2024!@localhost:3309/cremer_db
     INTO postgresql://cremer_user:CremerP@ssw0rd2024!@localhost:5432/cremer_db

WITH include drop, create tables, create indexes, reset sequences

SET work_mem to '256MB',
    maintenance_work_mem to '512 MB';
```

Ejecutar:

```bash
pgloader mysql-to-postgres.load
```

### Opción 2: Export/Import Manual

**Exportar desde MySQL:**

```bash
mysqldump -h localhost -P 3309 -u rnp_prod -p cremer_db > cremer_backup.sql
```

**Convertir sintaxis MySQL a PostgreSQL** (ajustar manualmente o usar herramientas)

**Importar a PostgreSQL:**

```bash
psql -h localhost -U cremer_user -d cremer_db < cremer_backup_converted.sql
```

---

## Troubleshooting

### La aplicación no puede conectarse a PostgreSQL

**Síntoma**: Logs muestran errores de conexión a la base de datos

**Solución**:

1. Verificar que PostgreSQL está corriendo:
   ```bash
   docker ps | grep postgres
   ```

2. Verificar la red Docker:
   ```bash
   docker network inspect cremer-network
   ```

3. Verificar que ambos contenedores están en la misma red:
   ```bash
   docker inspect cremer-service | grep -A 10 Networks
   docker inspect postgres-cremer | grep -A 10 Networks
   ```

4. Probar conexión manual:
   ```bash
   docker exec -it cremer-service sh
   wget -O- http://localhost:8600/actuator/health
   ```

### El health check falla

**Síntoma**: Jenkins reporta que el health check falló

**Verificar**:

1. Que Spring Boot Actuator esté habilitado en `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```

2. Que el endpoint esté expuesto en `application-prod.properties`:
   ```properties
   management.endpoints.web.exposure.include=health,info
   management.endpoint.health.show-details=always
   ```

### Error de permisos en Jenkins

**Síntoma**: Jenkins no puede conectarse al servidor via SSH

**Solución**:

1. Verificar que la credencial `ubuntu-server-ssh` existe
2. Verificar que la clave privada es correcta
3. Probar conexión SSH manualmente desde el servidor Jenkins:
   ```bash
   ssh administrador@192.168.10.135
   ```

### Contenedor se reinicia constantemente

**Síntoma**: `docker-compose ps` muestra el contenedor reiniciándose

**Solución**:

1. Ver logs:
   ```bash
   docker-compose logs --tail=200
   ```

2. Verificar que el JAR se construyó correctamente:
   ```bash
   docker exec cremer-service ls -lh /app/
   ```

3. Verificar variables de entorno:
   ```bash
   docker exec cremer-service env | grep DB
   ```

---

## URLs de Acceso

Una vez desplegada la aplicación:

- **API Base**: http://192.168.10.135:8600
- **Health Check**: http://192.168.10.135:8600/actuator/health
- **Swagger UI**: http://192.168.10.135:8600/swagger-ui.html
- **API Docs**: http://192.168.10.135:8600/v3/api-docs

---

## Actualización de la Aplicación

Para actualizar la aplicación después de hacer cambios en el código:

1. Hacer commit y push de los cambios al repositorio
2. En Jenkins, ir al job `cremer-service-deploy`
3. Click en **Build Now**
4. Monitorear el progreso del pipeline
5. Verificar que el despliegue fue exitoso

---

## Rollback

Si necesitas volver a una versión anterior:

### Opción 1: Re-deployar desde Jenkins

1. En Jenkins, ir al build exitoso anterior
2. Click en **Replay**

### Opción 2: Usar imagen Docker anterior

```bash
cd /home/administrador/cremer-service

# Ver imágenes disponibles
docker images | grep cremer-service

# Etiquetar imagen anterior como latest
docker tag cremer-service:<TAG_ANTERIOR> cremer-service:latest

# Reiniciar
docker-compose down
docker-compose up -d
```

---

## Monitoreo y Logs

### Logs Persistentes

Los logs están configurados con rotación en `docker-compose.yml`:

- Tamaño máximo por archivo: 10 MB
- Número máximo de archivos: 3

### Ver Logs del Sistema

```bash
# Logs de Docker del contenedor
docker logs cremer-service

# Logs de la aplicación (si están configurados en archivo)
docker exec cremer-service cat /app/logs/application.log
```

---

## Seguridad

### Recomendaciones:

1. **Cambiar las contraseñas por defecto** en `docker-compose.yml`
2. **Usar Docker Secrets** en lugar de variables de entorno para datos sensibles
3. **Configurar firewall** en el servidor Ubuntu:
   ```bash
   sudo ufw allow 8600/tcp
   sudo ufw enable
   ```
4. **Actualizar regularmente** las imágenes base de Docker
5. **Revisar logs** periódicamente en busca de actividad sospechosa

---

## Contacto y Soporte

Para problemas o consultas sobre el despliegue, contactar al equipo de DevOps.

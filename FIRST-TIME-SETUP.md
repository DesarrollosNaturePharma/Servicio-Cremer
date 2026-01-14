# Configuración Inicial - Primera Vez

Esta guía te ayudará a configurar todo desde cero para el primer despliegue de la aplicación dockerizada.

---

## Paso 1: Preparar el Servidor Ubuntu (192.168.10.135)

### 1.1. Conectarse al Servidor

```bash
ssh administrador@192.168.10.135
```

### 1.2. Verificar Docker

```bash
# Verificar que Docker está instalado
docker --version

# Si no está instalado:
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker administrador
# Cerrar sesión y volver a conectar
```

### 1.3. Verificar docker-compose

```bash
# Verificar que docker-compose está instalado
docker-compose --version

# Si no está instalado:
sudo apt-get update
sudo apt-get install -y docker-compose
```

### 1.4. Verificar PostgreSQL

```bash
# Listar contenedores
docker ps -a

# Buscar el contenedor de PostgreSQL
# Anotar el nombre exacto del contenedor (por ejemplo: postgres-cremer)
```

**IMPORTANTE**: Si el nombre del contenedor PostgreSQL NO es `postgres-cremer`, deberás actualizar el archivo `docker-compose.yml` más adelante.

### 1.5. Ejecutar Script de Configuración

Desde tu máquina local, transferir el script:

```bash
# Desde el directorio del proyecto
scp setup-server.sh administrador@192.168.10.135:/home/administrador/
```

En el servidor Ubuntu:

```bash
cd /home/administrador
chmod +x setup-server.sh
./setup-server.sh
```

Este script:
- ✅ Verifica Docker
- ✅ Crea la red `cremer-network`
- ✅ Conecta PostgreSQL a la red
- ✅ Crea la base de datos `cremer_db`
- ✅ Crea el usuario `cremer_user`
- ✅ Crea directorios necesarios

### 1.6. Verificar la Configuración

```bash
# Verificar red Docker
docker network ls | grep cremer-network

# Verificar que PostgreSQL está en la red
docker network inspect cremer-network

# Deberías ver el contenedor de PostgreSQL listado

# Verificar base de datos
docker exec -it <nombre-contenedor-postgres> psql -U postgres -c "\l"
# Deberías ver la base de datos cremer_db
```

### 1.7. Ajustar docker-compose.yml (Si es necesario)

Si el nombre del contenedor PostgreSQL NO es `postgres-cremer`:

```bash
# Desde tu máquina local, edita docker-compose.yml
# Cambia la línea:
# - DB_URL=jdbc:postgresql://postgres-cremer:5432/cremer_db
# Por:
# - DB_URL=jdbc:postgresql://<NOMBRE-REAL-CONTENEDOR>:5432/cremer_db
```

**Alternativa**: Usar host network o IP directa si tienes problemas con la red Docker.

---

## Paso 2: Configurar Jenkins

### 2.1. Crear Credenciales SSH

1. Abre Jenkins: http://[IP-JENKINS]:8080
2. Ve a: **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. Click en **Add Credentials**
4. Configurar:
   - **Kind**: SSH Username with private key
   - **ID**: `ubuntu-server-ssh`
   - **Description**: Credenciales para servidor Ubuntu 192.168.10.135
   - **Username**: `administrador`
   - **Private Key**:
     - Selecciona "Enter directly"
     - Pega la clave privada SSH completa (desde `-----BEGIN` hasta `-----END`)
   - Click en **Create**

### 2.2. Verificar Conexión SSH desde Jenkins

Si tienes acceso al servidor Jenkins:

```bash
# SSH al servidor Jenkins
# Probar conexión SSH
ssh -i /path/to/private/key administrador@192.168.10.135

# Si funciona, la credencial está correcta
```

### 2.3. Crear el Pipeline Job

1. En Jenkins, click en **New Item**
2. Nombre del job: `cremer-service-deploy`
3. Seleccionar: **Pipeline**
4. Click en **OK**

### 2.4. Configurar el Pipeline

En la configuración del job:

**General**:
- ✅ Marcar "Discard old builds"
  - Max # of builds to keep: `10`

**Pipeline**:
- **Definition**: Pipeline script from SCM
- **SCM**: Git
- **Repository URL**: [URL de tu repositorio Git]
  - Ejemplo: `https://github.com/usuario/cremer-service.git`
  - O: `git@github.com:usuario/cremer-service.git`
- **Credentials**: Selecciona las credenciales de Git si es repositorio privado
- **Branches to build**: `*/main` (o `*/master` según tu rama)
- **Script Path**: `Jenkinsfile`

Click en **Save**

### 2.5. Probar la Configuración

**NO ejecutar el pipeline todavía**. Primero verifica:

```bash
# En el servidor Ubuntu, verificar que todo está listo
docker network ls | grep cremer-network
docker ps | grep postgres
```

---

## Paso 3: Migrar Datos de MySQL a PostgreSQL (Opcional)

Si tienes datos en MySQL que necesitas migrar:

### Opción A: Usar pgLoader (Recomendado)

En el servidor Ubuntu:

```bash
# Instalar pgLoader
sudo apt-get update
sudo apt-get install -y pgloader

# Crear archivo de configuración
nano migrate.load
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
pgloader migrate.load
```

### Opción B: Migración Manual

```bash
# Exportar desde MySQL
mysqldump -h localhost -P 3309 -u rnp_prod -p cremer_db > backup.sql

# Convertir sintaxis (ajustar manualmente tipos de datos, auto_increment, etc.)
# O usar herramientas online de conversión

# Importar a PostgreSQL
psql -h localhost -U cremer_user -d cremer_db < backup_converted.sql
```

### Verificar Migración

```bash
# Conectar a PostgreSQL
docker exec -it postgres-cremer psql -U cremer_user -d cremer_db

# Verificar tablas
\dt

# Verificar datos
SELECT COUNT(*) FROM <nombre_tabla>;

# Salir
\q
```

---

## Paso 4: Primer Despliegue

### 4.1. Preparar el Código

En tu máquina local:

```bash
cd "D:\PROYECTOS\CAPTURA DATOS\RNP_CAPTURA\Cremer-Service"

# Verificar cambios
git status

# Agregar archivos nuevos
git add .

# Commit
git commit -m "feat: Dockerize application with Jenkins pipeline and PostgreSQL migration"

# Push
git push origin main
```

### 4.2. Ejecutar el Pipeline de Jenkins

1. Ve a Jenkins
2. Abre el job `cremer-service-deploy`
3. Click en **Build Now**
4. Monitorea la consola de output (click en el número de build → Console Output)

### 4.3. Monitorear el Despliegue

El pipeline ejecutará los siguientes stages:
1. ✅ Checkout - Descarga el código
2. ✅ Prepare Deploy Directory - Prepara directorios
3. ✅ Transfer Files - Transfiere archivos al servidor
4. ✅ Build Docker Image - Construye la imagen
5. ✅ Deploy - Despliega el contenedor
6. ✅ Health Check - Verifica que la app funciona

### 4.4. Verificar el Despliegue

```bash
# En el servidor Ubuntu
cd /home/administrador/cremer-service

# Ver estado
docker-compose ps

# Ver logs
docker-compose logs -f

# Verificar health
curl http://localhost:8600/actuator/health
```

Deberías ver:
```json
{
  "status": "UP"
}
```

### 4.5. Acceder a la Aplicación

Desde tu navegador:

- **API Base**: http://192.168.10.135:8600
- **Health Check**: http://192.168.10.135:8600/actuator/health
- **Swagger UI**: http://192.168.10.135:8600/swagger-ui.html

---

## Paso 5: Configuración Post-Despliegue

### 5.1. Configurar Firewall (Opcional)

En el servidor Ubuntu:

```bash
# Instalar UFW si no está
sudo apt-get install ufw

# Permitir SSH (IMPORTANTE - antes de habilitar UFW)
sudo ufw allow ssh

# Permitir puerto 8600
sudo ufw allow 8600/tcp

# Habilitar firewall
sudo ufw enable

# Verificar status
sudo ufw status
```

### 5.2. Configurar Backups de PostgreSQL

```bash
# Crear script de backup
sudo nano /home/administrador/backup-postgres.sh
```

Contenido:

```bash
#!/bin/bash
BACKUP_DIR="/home/administrador/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

docker exec postgres-cremer pg_dump -U cremer_user cremer_db | gzip > $BACKUP_DIR/cremer_db_$DATE.sql.gz

# Mantener solo últimos 7 backups
find $BACKUP_DIR -name "cremer_db_*.sql.gz" -mtime +7 -delete
```

Hacer ejecutable y agregar a cron:

```bash
chmod +x /home/administrador/backup-postgres.sh

# Agregar a cron (backup diario a las 2 AM)
crontab -e

# Agregar línea:
0 2 * * * /home/administrador/backup-postgres.sh
```

### 5.3. Configurar Monitoreo (Opcional)

Puedes agregar monitoring con Prometheus/Grafana o usar herramientas simples:

```bash
# Instalar herramientas de monitoreo
sudo apt-get install -y htop iotop nethogs

# Monitorear recursos del contenedor
docker stats cremer-service
```

---

## Troubleshooting Primera Instalación

### Problema: Pipeline falla en "Transfer Files"

**Causa**: Problemas de SSH o permisos

**Solución**:
```bash
# Verificar conexión SSH manual
ssh administrador@192.168.10.135

# Verificar permisos de directorio
ssh administrador@192.168.10.135 'ls -la /home/administrador'

# Verificar credencial en Jenkins
# Ir a Jenkins → Manage Jenkins → Credentials → ubuntu-server-ssh
```

### Problema: Pipeline falla en "Build Docker Image"

**Causa**: Maven no puede descargar dependencias

**Solución**:
```bash
# En servidor Ubuntu, verificar conectividad
ssh administrador@192.168.10.135
ping maven.apache.org
curl -I https://repo.maven.apache.org

# Verificar que mvnw tiene permisos de ejecución
cd /home/administrador/cremer-service
chmod +x mvnw
```

### Problema: Health check falla

**Causa**: Aplicación no puede conectarse a PostgreSQL

**Solución**:
```bash
# Verificar que PostgreSQL está corriendo
docker ps | grep postgres

# Verificar red Docker
docker network inspect cremer-network

# Verificar logs de la aplicación
docker-compose logs cremer-app

# Verificar variables de entorno
docker exec cremer-service env | grep DB

# Probar conexión manual a PostgreSQL
docker exec cremer-service ping postgres-cremer
```

### Problema: Contenedor se reinicia constantemente

**Causa**: Error en la aplicación o configuración incorrecta

**Solución**:
```bash
# Ver logs completos
docker-compose logs --tail=500 cremer-app

# Buscar errores comunes:
# - Connection refused a PostgreSQL
# - Credenciales incorrectas
# - Puerto ya en uso
# - JAR corrupto

# Verificar que el JAR existe
docker exec cremer-service ls -lh /app/

# Verificar que Java funciona
docker exec cremer-service java -version
```

---

## Checklist de Verificación

Antes de considerar completa la instalación, verifica:

- [ ] Docker instalado en servidor Ubuntu
- [ ] docker-compose instalado en servidor Ubuntu
- [ ] Red Docker `cremer-network` creada
- [ ] PostgreSQL corriendo y conectado a la red
- [ ] Base de datos `cremer_db` creada
- [ ] Usuario `cremer_user` con permisos correctos
- [ ] Datos migrados desde MySQL (si aplica)
- [ ] Jenkins configurado con credencial SSH
- [ ] Pipeline job creado en Jenkins
- [ ] Primer build exitoso
- [ ] Aplicación accesible en puerto 8600
- [ ] Health check retorna status UP
- [ ] Swagger UI accesible
- [ ] Logs de aplicación sin errores críticos
- [ ] Firewall configurado (opcional)
- [ ] Backups configurados (opcional)

---

## Próximos Pasos

Una vez completada la instalación inicial:

1. ✅ Configurar monitoreo y alertas
2. ✅ Documentar endpoints de API
3. ✅ Crear tests de integración
4. ✅ Configurar HTTPS/SSL
5. ✅ Implementar rate limiting
6. ✅ Revisar y mejorar seguridad

---

## Referencias

- **Documentación Completa**: `DEPLOY-README.md`
- **Guía Rápida**: `QUICK-START.md`
- **Changelog**: `CHANGELOG-DOCKER.md`

---

## Contacto

Para soporte técnico o problemas durante la instalación, consultar la documentación o contactar al equipo DevOps.

---

**Última actualización**: 2024-12-24

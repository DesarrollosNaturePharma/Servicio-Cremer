# Guía Rápida de Despliegue

## Para el Administrador del Servidor (Primera vez)

### 1. Configuración Inicial del Servidor Ubuntu (192.168.10.135)

```bash
# Transferir y ejecutar el script de configuración
scp setup-server.sh administrador@192.168.10.135:/home/administrador/
ssh administrador@192.168.10.135
chmod +x setup-server.sh
./setup-server.sh
```

### 2. Verificar que el nombre del contenedor PostgreSQL es correcto

```bash
# Ver contenedores corriendo
docker ps

# Si PostgreSQL tiene otro nombre, edita docker-compose.yml
# y cambia 'postgres-cremer' por el nombre correcto en DB_URL
```

---

## Para Desarrolladores

### Hacer Cambios y Desplegar

```bash
# 1. Hacer cambios en el código
# 2. Commit y push
git add .
git commit -m "Descripción de cambios"
git push

# 3. Ir a Jenkins y ejecutar "Build Now" en el job cremer-service-deploy
```

---

## Comandos Útiles en el Servidor

### Ver Estado

```bash
cd /home/administrador/cremer-service
docker-compose ps
docker-compose logs -f
```

### Reiniciar

```bash
docker-compose restart
```

### Ver Logs

```bash
docker-compose logs --tail=100
docker-compose logs -f  # Tiempo real
```

### Parar/Iniciar

```bash
docker-compose down
docker-compose up -d
```

### Verificar Salud de la Aplicación

```bash
curl http://localhost:8600/actuator/health
```

---

## Troubleshooting Rápido

### Error de Conexión a BD

```bash
# Verificar que PostgreSQL está corriendo
docker ps | grep postgres

# Verificar conectividad de red
docker network inspect cremer-network

# Ver logs de la aplicación
docker-compose logs cremer-app
```

### La aplicación no inicia

```bash
# Ver logs completos
docker-compose logs --tail=200

# Verificar variables de entorno
docker exec cremer-service env | grep DB

# Reiniciar desde cero
docker-compose down
docker-compose up -d
docker-compose logs -f
```

### Rollback a versión anterior

```bash
# Opción 1: Re-ejecutar build anterior en Jenkins
# Ve a Jenkins > cremer-service-deploy > Build anterior exitoso > Replay

# Opción 2: Usar docker-compose
docker-compose down
docker tag cremer-service:latest cremer-service:backup
# Construir versión anterior del código
docker build -t cremer-service:latest .
docker-compose up -d
```

---

## URLs Importantes

| Servicio | URL |
|----------|-----|
| API Base | http://192.168.10.135:8600 |
| Health Check | http://192.168.10.135:8600/actuator/health |
| Swagger UI | http://192.168.10.135:8600/swagger-ui.html |
| Métricas | http://192.168.10.135:8600/actuator/metrics |

---

## Configuración de PostgreSQL (Solo primera vez)

Si necesitas crear manualmente la BD y usuario:

```bash
# Conectar a PostgreSQL
docker exec -it postgres-cremer psql -U postgres

# Ejecutar en psql:
CREATE DATABASE cremer_db;
CREATE USER cremer_user WITH PASSWORD 'CremerP@ssw0rd2024!';
GRANT ALL PRIVILEGES ON DATABASE cremer_db TO cremer_user;
ALTER DATABASE cremer_db OWNER TO cremer_user;
\c cremer_db
GRANT ALL PRIVILEGES ON SCHEMA public TO cremer_user;
\q
```

---

## Migración desde MySQL

### Usando pgLoader

```bash
# Instalar pgLoader
sudo apt-get install pgloader

# Crear archivo de configuración
cat > migrate.load << 'EOF'
LOAD DATABASE
     FROM mysql://rnp_prod:RnpPr0dP@ssw0rd2024!@localhost:3309/cremer_db
     INTO postgresql://cremer_user:CremerP@ssw0rd2024!@localhost:5432/cremer_db
WITH include drop, create tables, create indexes, reset sequences
SET work_mem to '256MB';
EOF

# Ejecutar migración
pgloader migrate.load
```

---

## Checklist de Despliegue

- [ ] Servidor configurado con `setup-server.sh`
- [ ] Red Docker `cremer-network` creada
- [ ] PostgreSQL corriendo y accesible
- [ ] Base de datos `cremer_db` creada
- [ ] Usuario `cremer_user` creado
- [ ] Jenkins configurado con credencial SSH `ubuntu-server-ssh`
- [ ] Job de Jenkins creado apuntando al Jenkinsfile
- [ ] Puerto 8600 abierto en firewall (opcional)
- [ ] Datos migrados desde MySQL (si aplica)

---

## Contacto

Para más detalles, consulta `DEPLOY-README.md`



docker-compose up -d --build
#!/bin/bash

################################################################################
# Script de Configuración del Servidor Ubuntu para Cremer Service
################################################################################
# Este script debe ejecutarse en el servidor Ubuntu (192.168.10.135)
# como usuario 'administrador'
#
# Uso:
#   chmod +x setup-server.sh
#   ./setup-server.sh
################################################################################

set -e  # Salir si hay algún error

echo "=========================================="
echo "Configuración del Servidor para Cremer"
echo "=========================================="
echo ""

# Variables de configuración
DOCKER_NETWORK="cremer-network"
POSTGRES_CONTAINER="postgres-cremer"
DB_NAME="cremer_db"
DB_USER="cremer_user"
DB_PASSWORD="CremerP@ssw0rd2024!"

echo "1. Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker no está instalado"
    echo "Por favor, instala Docker primero: https://docs.docker.com/engine/install/ubuntu/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "WARNING: docker-compose no está instalado"
    echo "Instalando docker-compose..."
    sudo apt-get update
    sudo apt-get install -y docker-compose
fi

echo "✓ Docker está instalado"
echo ""

echo "2. Verificando red Docker '${DOCKER_NETWORK}'..."
if docker network ls | grep -q "${DOCKER_NETWORK}"; then
    echo "✓ La red ${DOCKER_NETWORK} ya existe"
else
    echo "Creando red Docker ${DOCKER_NETWORK}..."
    docker network create ${DOCKER_NETWORK}
    echo "✓ Red ${DOCKER_NETWORK} creada"
fi
echo ""

echo "3. Verificando PostgreSQL..."
if docker ps | grep -q "${POSTGRES_CONTAINER}"; then
    echo "✓ Contenedor PostgreSQL está corriendo"

    # Verificar si está en la red correcta
    if docker inspect ${POSTGRES_CONTAINER} | grep -q "${DOCKER_NETWORK}"; then
        echo "✓ PostgreSQL ya está en la red ${DOCKER_NETWORK}"
    else
        echo "Conectando PostgreSQL a la red ${DOCKER_NETWORK}..."
        docker network connect ${DOCKER_NETWORK} ${POSTGRES_CONTAINER} || true
        echo "✓ PostgreSQL conectado a la red"
    fi
else
    echo "WARNING: No se encontró el contenedor '${POSTGRES_CONTAINER}'"
    echo "Por favor, asegúrate de que PostgreSQL está corriendo"
    echo ""
    echo "Comandos sugeridos:"
    echo "  - Si PostgreSQL tiene otro nombre, actualiza la variable POSTGRES_CONTAINER"
    echo "  - Ver contenedores: docker ps -a"
    echo "  - Iniciar PostgreSQL: docker start <nombre_contenedor>"
fi
echo ""

echo "4. Configurando base de datos..."
echo "Intentando crear la base de datos y usuario..."

# Intentar conectar a PostgreSQL y crear DB/usuario
docker exec -it ${POSTGRES_CONTAINER} psql -U postgres <<EOF || true
-- Crear la base de datos si no existe
SELECT 'CREATE DATABASE ${DB_NAME}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}')\gexec

-- Crear el usuario si no existe
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = '${DB_USER}') THEN
    CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';
  END IF;
END
\$\$;

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
ALTER DATABASE ${DB_NAME} OWNER TO ${DB_USER};

\c ${DB_NAME}

-- Otorgar privilegios en el schema public
GRANT ALL PRIVILEGES ON SCHEMA public TO ${DB_USER};
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${DB_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${DB_USER};

-- Privilegios por defecto para objetos futuros
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${DB_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${DB_USER};

\q
EOF

if [ $? -eq 0 ]; then
    echo "✓ Base de datos configurada correctamente"
else
    echo "⚠ Hubo un problema configurando la base de datos"
    echo "Por favor, configura manualmente siguiendo las instrucciones en DEPLOY-README.md"
fi
echo ""

echo "5. Creando estructura de directorios..."
DEPLOY_DIR="/home/administrador/cremer-service"
mkdir -p ${DEPLOY_DIR}
echo "✓ Directorio ${DEPLOY_DIR} creado"
echo ""

echo "6. Verificando puerto 8600..."
if lsof -Pi :8600 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠ WARNING: El puerto 8600 ya está en uso"
    echo "Procesos usando el puerto:"
    lsof -i :8600
else
    echo "✓ Puerto 8600 disponible"
fi
echo ""

echo "7. Configurando firewall (opcional)..."
if command -v ufw &> /dev/null; then
    echo "¿Deseas abrir el puerto 8600 en el firewall? (s/n)"
    read -r response
    if [[ "$response" =~ ^[Ss]$ ]]; then
        sudo ufw allow 8600/tcp
        echo "✓ Puerto 8600 abierto en firewall"
    fi
else
    echo "ℹ UFW no está instalado, omitiendo configuración de firewall"
fi
echo ""

echo "=========================================="
echo "Configuración completada!"
echo "=========================================="
echo ""
echo "Resumen:"
echo "  - Red Docker: ${DOCKER_NETWORK}"
echo "  - Base de datos: ${DB_NAME}"
echo "  - Usuario DB: ${DB_USER}"
echo "  - Directorio deploy: ${DEPLOY_DIR}"
echo ""
echo "Siguiente paso:"
echo "  Ejecuta el pipeline de Jenkins para desplegar la aplicación"
echo ""
echo "Para verificar la configuración:"
echo "  docker network inspect ${DOCKER_NETWORK}"
echo "  docker ps"
echo ""
echo "Documentación completa en: DEPLOY-README.md"
echo "=========================================="

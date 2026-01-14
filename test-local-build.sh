#!/bin/bash

################################################################################
# Script de Prueba Local para Cremer Service
################################################################################
# Este script prueba que el Dockerfile funciona correctamente
# antes de hacer el despliegue
#
# Uso:
#   chmod +x test-local-build.sh
#   ./test-local-build.sh
################################################################################

set -e  # Salir si hay algún error

echo "=========================================="
echo "Prueba Local de Build Docker"
echo "=========================================="
echo ""

# Variables
IMAGE_NAME="cremer-service"
IMAGE_TAG="test"
CONTAINER_NAME="cremer-test"

echo "1. Limpiando contenedores e imágenes de prueba anteriores..."
docker rm -f ${CONTAINER_NAME} 2>/dev/null || true
docker rmi ${IMAGE_NAME}:${IMAGE_TAG} 2>/dev/null || true
echo "✓ Limpieza completada"
echo ""

echo "2. Construyendo imagen Docker..."
echo "   Esto puede tomar varios minutos la primera vez..."
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
echo "✓ Imagen construida exitosamente"
echo ""

echo "3. Verificando la imagen..."
docker images | grep ${IMAGE_NAME}
IMAGE_SIZE=$(docker images ${IMAGE_NAME}:${IMAGE_TAG} --format "{{.Size}}")
echo "✓ Tamaño de la imagen: ${IMAGE_SIZE}"
echo ""

echo "4. Inspeccionando la imagen..."
echo "   Capas de la imagen:"
docker history ${IMAGE_NAME}:${IMAGE_TAG} --human --format "table {{.CreatedBy}}\t{{.Size}}" | head -10
echo ""

echo "5. Verificando el JAR dentro de la imagen..."
docker run --rm ${IMAGE_NAME}:${IMAGE_TAG} ls -lh /app/
echo "✓ Contenido de /app/ verificado"
echo ""

echo "6. Prueba de inicio (sin conexión a BD)..."
echo "   Iniciando contenedor de prueba..."
echo "   NOTA: Fallará la conexión a BD, pero verifica que el JAR es ejecutable"
echo ""

# Iniciar contenedor con un timeout
timeout 30s docker run --rm --name ${CONTAINER_NAME} \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_URL=jdbc:postgresql://localhost:5432/test_db \
    -e DB_USERNAME=test \
    -e DB_PASSWORD=test \
    ${IMAGE_NAME}:${IMAGE_TAG} || true

echo ""
echo "=========================================="
echo "Build Local Completado!"
echo "=========================================="
echo ""
echo "La imagen se construyó correctamente:"
echo "  - Nombre: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "  - Tamaño: ${IMAGE_SIZE}"
echo ""
echo "Siguiente paso:"
echo "  - Hacer commit de los cambios"
echo "  - Ejecutar el pipeline de Jenkins"
echo ""
echo "Para probar con una base de datos real:"
echo "  docker run -p 8600:8600 \\"
echo "    -e DB_URL=jdbc:postgresql://host.docker.internal:5432/cremer_db \\"
echo "    -e DB_USERNAME=cremer_user \\"
echo "    -e DB_PASSWORD=CremerP@ssw0rd2024! \\"
echo "    ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "=========================================="

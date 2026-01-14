pipeline {
    agent any

    environment {
        // Configuración del servidor destino
        DEPLOY_SERVER = '192.168.10.135'
        DEPLOY_USER = 'administrador'
        DEPLOY_DIR = '/home/administrador'
        SSH_CREDENTIALS = 'ubuntu-server-ssh'

        // Configuración de la aplicación
        APP_NAME = 'cremer-service'
        DOCKER_IMAGE = 'cremer-service:latest'

        // Red Docker existente (infraestructura de BD)
        DOCKER_NETWORK = 'db_infrastructure_network'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm

                script {
                    // Obtener información del commit
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    env.GIT_COMMIT_MSG = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()
                }

                echo "Building commit: ${env.GIT_COMMIT_SHORT}"
                echo "Commit message: ${env.GIT_COMMIT_MSG}"
            }
        }

        stage('Prepare Deploy Directory') {
            steps {
                echo 'Preparing deployment directory on target server...'

                sshagent(credentials: [SSH_CREDENTIALS]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_SERVER} '
                            # Crear directorio del proyecto si no existe
                            mkdir -p ${DEPLOY_DIR}/${APP_NAME}

                            # Verificar que existe la red Docker de infraestructura
                            if docker network ls | grep -q ${DOCKER_NETWORK}; then
                                echo "Red Docker ${DOCKER_NETWORK} encontrada OK"
                            else
                                echo "ERROR: Red ${DOCKER_NETWORK} no existe. Asegúrate de que la infraestructura de BD está corriendo."
                                exit 1
                            fi
                        '
                    """
                }
            }
        }

        stage('Transfer Files') {
            steps {
                echo 'Transferring files to target server...'

                sshagent(credentials: [SSH_CREDENTIALS]) {
                    sh """
                        # Transferir archivos necesarios al servidor
                        scp -o StrictHostKeyChecking=no -r \
                            Dockerfile \
                            docker-compose.yml \
                            pom.xml \
                            mvnw \
                            .mvn \
                            src \
                            ${DEPLOY_USER}@${DEPLOY_SERVER}:${DEPLOY_DIR}/${APP_NAME}/
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image on target server...'

                sshagent(credentials: [SSH_CREDENTIALS]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_SERVER} '
                            cd ${DEPLOY_DIR}/${APP_NAME}

                            # Construir la imagen Docker en el servidor (x86_64)
                            echo "Building Docker image..."
                            docker build -t ${DOCKER_IMAGE} .

                            # Limpiar imágenes dangling
                            echo "Cleaning up dangling images..."
                            docker image prune -f
                        '
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying application...'

                sshagent(credentials: [SSH_CREDENTIALS]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_SERVER} '
                            cd ${DEPLOY_DIR}/${APP_NAME}

                            # Detener y eliminar contenedor anterior si existe
                            echo "Stopping existing container..."
                            docker-compose down || true

                            # Iniciar el nuevo contenedor
                            echo "Starting new container..."
                            docker-compose up -d

                            # Esperar a que la aplicación inicie
                            echo "Waiting for application to start..."
                            sleep 10

                            # Verificar el estado del contenedor
                            echo "Checking container status..."
                            docker-compose ps

                            # Mostrar logs recientes
                            echo "Recent logs:"
                            docker-compose logs --tail=50
                        '
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                echo 'Performing health check...'

                sshagent(credentials: [SSH_CREDENTIALS]) {
                    script {
                        def maxRetries = 12
                        def retryCount = 0
                        def healthy = false

                        while (retryCount < maxRetries && !healthy) {
                            try {
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_SERVER} '
                                        # Verificar que el contenedor está corriendo
                                        if docker ps | grep -q ${APP_NAME}; then
                                            echo "Container is running"

                                            # Intentar acceder al health endpoint
                                            curl -f http://localhost:8600/actuator/health || exit 1
                                        else
                                            echo "Container is not running"
                                            exit 1
                                        fi
                                    '
                                """
                                healthy = true
                                echo "Application is healthy!"
                            } catch (Exception e) {
                                retryCount++
                                if (retryCount < maxRetries) {
                                    echo "Health check failed, retrying in 10 seconds... (${retryCount}/${maxRetries})"
                                    sleep 10
                                } else {
                                    error "Health check failed after ${maxRetries} attempts"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
            echo "Application URL: http://${DEPLOY_SERVER}:8600"
            echo "Swagger UI: http://${DEPLOY_SERVER}:8600/swagger-ui.html"
        }
        failure {
            echo 'Deployment failed!'

            sshagent(credentials: [SSH_CREDENTIALS]) {
                sh """
                    ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_SERVER} '
                        cd ${DEPLOY_DIR}/${APP_NAME}
                        echo "=== Container Status ==="
                        docker-compose ps
                        echo ""
                        echo "=== Application Logs ==="
                        docker-compose logs --tail=100
                    '
                """
            }
        }
        always {
            echo 'Cleaning up workspace...'
            cleanWs()
        }
    }
}

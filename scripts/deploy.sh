#!/bin/bash

# Selah API Server - Blue/Green 배포 스크립트
# 이 파일을 Lightsail의 /home/{user}/selah/deploy.sh 에 배치하세요.

# === 설정 변수 ===
SERVICE_NAME="selah"
BASE_DIR="/home/ec2-user"  # 실제 사용자에 맞게 수정
PORT_PREFIX=80
DOCKER_IMAGE="amazoncorretto:21-alpine"
JAR_FILE_NAME="selah-api-server.jar"

# === 자동 계산 ===
PROJECT_DIR="${BASE_DIR}/${SERVICE_NAME}"
SERVICE_URL_INC="/etc/nginx/conf.d/${SERVICE_NAME}-api-url.inc"
NGINX_VAR_NAME="api_url_${SERVICE_NAME//-/_}"

# === 환경 변수 로드 ===
if [ -f "${PROJECT_DIR}/.env" ]; then
    source "${PROJECT_DIR}/.env"
else
    echo "❌ .env file not found at ${PROJECT_DIR}/.env"
    exit 1
fi

# === Blue/Green 슬롯 결정 ===
EXIST_BLUE=$(docker ps -a | grep "${SERVICE_NAME}-api-blue")
if [ -z "$EXIST_BLUE" ]; then
    PORT="${PORT_PREFIX}90"
    CONTAINER_NAME="${SERVICE_NAME}-api-blue"
    REMOVE_TARGET="${SERVICE_NAME}-api-green"
    SLOT="blue"
    echo "🔵 Starting BLUE deployment (port: $PORT)"
else
    PORT="${PORT_PREFIX}91"
    CONTAINER_NAME="${SERVICE_NAME}-api-green"
    REMOVE_TARGET="${SERVICE_NAME}-api-blue"
    SLOT="green"
    echo "🟢 Starting GREEN deployment (port: $PORT)"
fi

# === JAR 파일 원자적 교체 ===
JAR_SLOT_DIR="${PROJECT_DIR}/backend/${SLOT}"
mkdir -p "$JAR_SLOT_DIR"
echo "📦 Copying JAR to ${SLOT} slot..."

# GitHub Actions에서 전송된 JAR 찾기
UPLOADED_JAR=$(ls -t ${PROJECT_DIR}/backend/*.jar 2>/dev/null | head -1)
if [ -z "$UPLOADED_JAR" ]; then
    echo "❌ JAR file not found!"
    exit 1
fi

cp "$UPLOADED_JAR" "${JAR_SLOT_DIR}/${JAR_FILE_NAME}.tmp"
mv "${JAR_SLOT_DIR}/${JAR_FILE_NAME}.tmp" "${JAR_SLOT_DIR}/${JAR_FILE_NAME}"
echo "✅ JAR copied to ${JAR_SLOT_DIR}/${JAR_FILE_NAME}"

# === Docker 컨테이너 실행 ===
docker run -d --name ${CONTAINER_NAME} \
    --network bridge \
    -p ${PORT}:8080 \
    -v ${JAR_SLOT_DIR}/${JAR_FILE_NAME}:/app.jar:ro \
    -v ${PROJECT_DIR}/logs:/logs \
    -e TZ=Asia/Seoul \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DATASOURCE_URL="${DATASOURCE_URL}" \
    -e DATASOURCE_USERNAME="${DATASOURCE_USERNAME}" \
    -e DATASOURCE_PASSWORD="${DATASOURCE_PASSWORD}" \
    -e ENCRYPTION_MASTER_KEY="${ENCRYPTION_MASTER_KEY}" \
    -e OAUTH_GOOGLE_CLIENT_ID="${OAUTH_GOOGLE_CLIENT_ID}" \
    -e OAUTH_GOOGLE_CLIENT_SECRET="${OAUTH_GOOGLE_CLIENT_SECRET}" \
    -e OAUTH_KAKAO_CLIENT_ID="${OAUTH_KAKAO_CLIENT_ID}" \
    -e OAUTH_KAKAO_CLIENT_SECRET="${OAUTH_KAKAO_CLIENT_SECRET}" \
    -e OAUTH_NAVER_CLIENT_ID="${OAUTH_NAVER_CLIENT_ID}" \
    -e OAUTH_NAVER_CLIENT_SECRET="${OAUTH_NAVER_CLIENT_SECRET}" \
    --restart unless-stopped \
    ${DOCKER_IMAGE} \
    java \
    -XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -Dfile.encoding=UTF-8 \
    -jar /app.jar

# === Health Check ===
echo "⏳ Waiting for health check (http://127.0.0.1:$PORT/actuator/health)"
for i in {1..30}; do
    HTTP_STATUS=$(curl -o /dev/null -s -w "%{http_code}" http://127.0.0.1:$PORT/actuator/health)
    if [ "$HTTP_STATUS" -eq 200 ]; then
        echo "✅ $CONTAINER_NAME is healthy!"
        break
    fi
    echo "   └── Attempt $i/30 - Status: $HTTP_STATUS, retrying in 5s..."
    sleep 5
done

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo "❌ Health check failed! Rolling back..."
    docker logs ${CONTAINER_NAME}
    docker rm -f ${CONTAINER_NAME}
    exit 1
fi

# === Nginx 설정 업데이트 ===
echo "🔄 Updating nginx to route traffic to port $PORT"
echo "set \$${NGINX_VAR_NAME} http://127.0.0.1:$PORT;" | sudo tee $SERVICE_URL_INC
sudo systemctl reload nginx

# === 기존 컨테이너 제거 ===
echo "⏰ Waiting 30s for graceful shutdown..."
sleep 30
docker rm -f $REMOVE_TARGET 2>/dev/null || true

# === 이전 JAR 정리 ===
rm -f ${PROJECT_DIR}/backend/*.jar

echo "🎉 Deployment completed successfully!"
exit 0

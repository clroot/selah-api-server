#!/bin/bash

# Selah API Server - Blue/Green 배포 스크립트

set -e  # 에러 발생 시 즉시 종료

# === 배포 시작 시간 기록 ===
DEPLOY_START_TIME=$(date +%s)

# === 설정 변수 ===
SERVICE_NAME="selah"
BASE_DIR="/home/ec2-user"  # 실제 사용자에 맞게 수정
PORT_PREFIX=80
DOCKER_IMAGE="amazoncorretto:21-alpine"
JAR_FILE_NAME="selah-api-server.jar"
HEALTH_CHECK_TIMEOUT=60  # 헬스체크 타임아웃 (초)
HEALTH_CHECK_INTERVAL=5  # 헬스체크 간격 (초)
GRACEFUL_SHUTDOWN_WAIT=30  # graceful shutdown 대기 시간 (초)

# === 자동 계산 ===
PROJECT_DIR="${BASE_DIR}/${SERVICE_NAME}"
SERVICE_URL_INC="/etc/nginx/conf.d/${SERVICE_NAME}-api-url.inc"
NGINX_VAR_NAME="api_url_${SERVICE_NAME//-/_}"
HEALTH_CHECK_MAX_ATTEMPTS=$((HEALTH_CHECK_TIMEOUT / HEALTH_CHECK_INTERVAL))

# === 유틸리티 함수 ===
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

cleanup_failed_deployment() {
    log "🧹 Cleaning up failed deployment..."
    docker rm -f ${CONTAINER_NAME} 2>/dev/null || true
}

# === 환경 변수 로드 ===
if [ -f "${PROJECT_DIR}/.env" ]; then
    source "${PROJECT_DIR}/.env"
else
    log "❌ .env file not found at ${PROJECT_DIR}/.env"
    exit 1
fi

# === 필수 환경 변수 검증 ===
REQUIRED_VARS=(
    "SPRING_DATASOURCE_URL"
    "SPRING_DATASOURCE_USERNAME"
    "SPRING_DATASOURCE_PASSWORD"
    "ENCRYPTION_MASTER_KEY"
)

MISSING_VARS=()
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -ne 0 ]; then
    log "❌ Missing required environment variables:"
    for var in "${MISSING_VARS[@]}"; do
        log "   └── $var"
    done
    exit 1
fi

log "✅ All required environment variables are set"

# === Blue/Green 슬롯 결정 ===
# 실행 중인 컨테이너만 체크 (docker ps, -a 제외)
EXIST_BLUE=$(docker ps --format '{{.Names}}' | grep "^${SERVICE_NAME}-api-blue$" || true)
if [ -z "$EXIST_BLUE" ]; then
    PORT="${PORT_PREFIX}90"
    CONTAINER_NAME="${SERVICE_NAME}-api-blue"
    REMOVE_TARGET="${SERVICE_NAME}-api-green"
    SLOT="blue"
    log "🔵 Starting BLUE deployment (port: $PORT)"
else
    PORT="${PORT_PREFIX}91"
    CONTAINER_NAME="${SERVICE_NAME}-api-green"
    REMOVE_TARGET="${SERVICE_NAME}-api-blue"
    SLOT="green"
    log "🟢 Starting GREEN deployment (port: $PORT)"
fi

# 기존 컨테이너 존재 여부 확인 (graceful shutdown 결정용)
EXIST_OLD_CONTAINER=$(docker ps --format '{{.Names}}' | grep "^${REMOVE_TARGET}$" || true)

# === JAR 파일 원자적 교체 ===
JAR_SLOT_DIR="${PROJECT_DIR}/backend/${SLOT}"
mkdir -p "$JAR_SLOT_DIR"
log "📦 Copying JAR to ${SLOT} slot..."

# GitHub Actions에서 전송된 JAR 찾기
UPLOADED_JAR=$(ls -t ${PROJECT_DIR}/backend/*.jar 2>/dev/null | head -1)
if [ -z "$UPLOADED_JAR" ]; then
    log "❌ JAR file not found!"
    exit 1
fi

cp "$UPLOADED_JAR" "${JAR_SLOT_DIR}/${JAR_FILE_NAME}.tmp"
mv "${JAR_SLOT_DIR}/${JAR_FILE_NAME}.tmp" "${JAR_SLOT_DIR}/${JAR_FILE_NAME}"
log "✅ JAR copied to ${JAR_SLOT_DIR}/${JAR_FILE_NAME}"

# === Docker 컨테이너 실행 ===
log "🐳 Starting Docker container..."
docker run -d --name ${CONTAINER_NAME} \
    --network bridge \
    -p ${PORT}:8080 \
    -v ${JAR_SLOT_DIR}/${JAR_FILE_NAME}:/app.jar:ro \
    -v ${PROJECT_DIR}/logs:/logs \
    -e TZ=Asia/Seoul \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}" \
    -e SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}" \
    -e SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" \
    -e ENCRYPTION_MASTER_KEY="${ENCRYPTION_MASTER_KEY}" \
    -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
    -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
    -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
    -e KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET}" \
    -e NAVER_CLIENT_ID="${NAVER_CLIENT_ID}" \
    -e NAVER_CLIENT_SECRET="${NAVER_CLIENT_SECRET}" \
    -e FRONTEND_URL="${FRONTEND_URL}" \
    -e BACKEND_URL="${BACKEND_URL}" \
    -e MAIL_HOST="${MAIL_HOST}" \
    -e MAIL_PORT="${MAIL_PORT}" \
    -e MAIL_USERNAME="${MAIL_USERNAME}" \
    -e MAIL_PASSWORD="${MAIL_PASSWORD}" \
    --restart unless-stopped \
    ${DOCKER_IMAGE} \
    java \
    -XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -Dfile.encoding=UTF-8 \
    -Dspring.profiles.active=prod \
    -jar /app.jar

# === Health Check ===
log "⏳ Waiting for health check (timeout: ${HEALTH_CHECK_TIMEOUT}s)"
log "   └── URL: http://127.0.0.1:$PORT/actuator/health"

HTTP_STATUS=0
for i in $(seq 1 $HEALTH_CHECK_MAX_ATTEMPTS); do
    HTTP_STATUS=$(curl -o /dev/null -s -w "%{http_code}" http://127.0.0.1:$PORT/actuator/health 2>/dev/null || echo "000")
    if [ "$HTTP_STATUS" -eq 200 ]; then
        log "✅ $CONTAINER_NAME is healthy!"
        break
    fi
    log "   └── Attempt $i/${HEALTH_CHECK_MAX_ATTEMPTS} - Status: $HTTP_STATUS, retrying in ${HEALTH_CHECK_INTERVAL}s..."
    sleep $HEALTH_CHECK_INTERVAL
done

if [ "$HTTP_STATUS" -ne 200 ]; then
    log "❌ Health check failed after ${HEALTH_CHECK_TIMEOUT}s! Rolling back..."
    log "📋 Container logs (last 50 lines):"
    docker logs --tail 50 ${CONTAINER_NAME}
    cleanup_failed_deployment
    exit 1
fi

# === Nginx 설정 업데이트 ===
log "🔄 Updating nginx to route traffic to port $PORT"
echo "set \$${NGINX_VAR_NAME} http://127.0.0.1:$PORT;" | sudo tee $SERVICE_URL_INC > /dev/null
sudo systemctl reload nginx

# === 기존 컨테이너 제거 ===
if [ -n "$EXIST_OLD_CONTAINER" ]; then
    log "⏰ Waiting ${GRACEFUL_SHUTDOWN_WAIT}s for graceful shutdown of ${REMOVE_TARGET}..."
    sleep $GRACEFUL_SHUTDOWN_WAIT
    docker rm -f $REMOVE_TARGET 2>/dev/null || true
    log "✅ Old container ${REMOVE_TARGET} removed"
else
    log "ℹ️  No existing container to remove, skipping graceful shutdown wait"
fi

# === 이전 JAR 정리 ===
rm -f ${PROJECT_DIR}/backend/*.jar

# === 배포 완료 ===
DEPLOY_END_TIME=$(date +%s)
DEPLOY_DURATION=$((DEPLOY_END_TIME - DEPLOY_START_TIME))

log "🎉 Deployment completed successfully!"
log "📊 Deployment summary:"
log "   └── Container: ${CONTAINER_NAME}"
log "   └── Port: ${PORT}"
log "   └── Duration: ${DEPLOY_DURATION}s"

exit 0

# GPU 사용 시 build-arg로 BASE_IMAGE를 적절한 CUDA 런타임 이미지로 변경하세요.
# 예: --build-arg BASE_IMAGE=nvidia/cuda:12.2.0-cudnn8-runtime-ubuntu22.04
ARG BASE_IMAGE=python:3.11-slim-bookworm
FROM ${BASE_IMAGE} AS base

# 메타데이터
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    POETRY_VIRTUALENVS_CREATE=false \
    MODEL_PATH=/app/best.pt \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    PYTHONOPTIMIZE=1

# 시스템 패키지 설치 (이미지에 따라 apt 사용)
# OpenCV/Pillow 관련 라이브러리와 빌드 도구 일부 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 생성 및 권한 설정
WORKDIR /app

# non-root 사용자 생성 (선택적 안전 조치)
ARG UID=1000
ARG GID=1000
RUN groupadd -g ${GID} appgroup || true && \
    useradd -u ${UID} -g appgroup -s /bin/bash -m appuser || true

# 의존성 먼저 복사 (빌드 캐시 활용)
COPY requirements.txt /app/requirements.txt

# pip 업그레이드 및 의존성 설치
# ultralytics와 torch 같은 대형 패키지 설치가 오래 걸릴 수 있습니다.
# --no-cache-dir로 캐시를 남기지 않습니다.
RUN python -m pip install --upgrade pip setuptools wheel && \
    pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch==2.3.1 torchvision==0.18.1 && \
    pip install --no-cache-dir -r /app/requirements.txt

# 애플리케이션 코드 복사
COPY api.py /app/api.py

# 모델 파일은 빌드 시 복사하거나, 아래 RUN 코드를 주석 처리하고 런타임에 마운트하세요.
# 빌드에 모델을 포함하려면 주석 해제 후 로컬의 best.pt를 Docker 빌드 컨텍스트에 넣으십시오.
COPY best.pt /app/best.pt

# 소유자 변경
RUN chown -R appuser:appgroup /app

# 포트 및 사용자 설정
EXPOSE 8001
USER appuser

# 환경변수(원하면 변경)
ENV UVICORN_WORKERS=1
ENV UVICORN_HOST=0.0.0.0
ENV UVICORN_PORT=8001

# 컨테이너 시작 명령 (개발/간단 운영용)
CMD ["sh", "-c", "uvicorn api:app --host ${UVICORN_HOST} --port ${UVICORN_PORT} --workers ${UVICORN_WORKERS}"]

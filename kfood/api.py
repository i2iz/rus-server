import base64
from io import BytesIO
from typing import List, Dict, Any

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from PIL import Image
from ultralytics import YOLO

# FastAPI 애플리케이션 생성
app = FastAPI(
    title="K-Food Object Detection API with YOLOv11",
    description="업로드된 한국 음식 이미지에서 객체를 감지하고 분석 결과를 반환하는 API입니다."
)

# 모델 로드
MODEL_PATH = "./best.pt"
try:
    model = YOLO(MODEL_PATH)
    print("🔥 YOLO is using device:", model.device)
except Exception as e:
    # 모델 로드 실패 시 서버 시작을 막고 에러 메시지 출력
    print(f"ERROR: Failed to load YOLO model from {MODEL_PATH}. Check your model path.")
    print(f"Details: {e}")

@app.get("/")
def read_root():
    """API 상태 확인을 위한 기본 경로"""
    return {"message": "YOLOv11 K-Food Object Detection API is running!"}

# 모델 추론 API 엔드포인트
@app.post("/predict")
async def predict_kfood(
    file: UploadFile = File(..., description="분석할 이미지 파일 (JPG, PNG 등)")
):
    """
    업로드된 이미지 파일에서 한국 음식을 감지하고 결과를 반환합니다.
    """
    if not file.content_type.startswith('image/'):
        raise HTTPException(
            status_code=400,
            detail="잘못된 파일 형식입니다. 이미지 파일을 업로드해 주세요."
        )

    try:
        # 1. 이미지 파일 읽기 및 PIL Image로 변환
        image_bytes = await file.read()
        image = Image.open(BytesIO(image_bytes)).convert("RGB")

        # 2. YOLO 모델 추론
        # imgsz는 학습 시 사용한 크기(640)를 사용합니다.
        results = model(image, imgsz=640, conf=0.5) # conf는 최소 신뢰도 설정 (예: 0.5)

        # 3. 바운딩 박스와 라벨이 그려진 이미지 준비
        # plot=True로 설정하여 결과를 이미지에 그립니다.
        plotted_image = results[0].plot(
            line_width=2,  # 바운딩 박스 두께
            # font_size=1,       # 라벨 폰트 크기
            conf=True,         # 확률(신뢰도) 표시
            labels=True        # 라벨 표시
        )
        
        # NumPy 배열 (plotted_image)을 PIL Image로 변환
        # OpenCV(BGR)로 변환된 경우를 고려하여 순서를 맞춥니다.
        try:
            # OpenCV를 사용하여 plot()이 BGR 포맷으로 출력했을 가능성을 고려하여 RGB로 변환합니다.
            import cv2
            plotted_image = cv2.cvtColor(plotted_image, cv2.COLOR_BGR2RGB)
            plotted_image = Image.fromarray(plotted_image)
        except ImportError:
            # OpenCV가 없거나 이미 RGB인 경우
            plotted_image = Image.fromarray(plotted_image)


        # 4. 바운딩 박스가 그려진 이미지를 Base64로 인코딩
        buffered = BytesIO()
        plotted_image.save(buffered, format="JPEG") # JPEG 또는 PNG 선택 가능
        encoded_image = base64.b64encode(buffered.getvalue()).decode("utf-8")

        # 5. 분석 결과 데이터 추출
        detected_foods: List[Dict[str, Any]] = []
        overall_confidence_sum = 0.0
        num_detections = 0
        
        # results[0].boxes는 감지된 모든 객체의 바운딩 박스 정보를 담고 있습니다.
        for box in results[0].boxes:
            # box.cls: 감지된 클래스 ID (tensor)
            # box.conf: 감지된 객체의 신뢰도 (tensor)
            # box.xyxy: 바운딩 박스 좌표 (x1, y1, x2, y2)
            
            conf = float(box.conf.cpu().numpy()[0])
            cls_id = int(box.cls.cpu().numpy()[0])
            label = model.names[cls_id] # 클래스 ID를 라벨 이름으로 변환
            
            overall_confidence_sum += conf
            num_detections += 1

            detected_foods.append({
                "label": label,
                "confidence": conf,  # 소수점 형태 (float)
                "bounding_box": box.xyxy.cpu().numpy().tolist()[0] # [x1, y1, x2, y2]
            })

        # 전체 신뢰도 계산 (감지된 객체가 있을 경우 평균 신뢰도, 없으면 0.0)
        overall_confidence = overall_confidence_sum / num_detections if num_detections > 0 else 0.0

        # 6. 최종 응답 데이터 구성
        response_data = {
            "image_with_boxes_base64": encoded_image,
            "detected_food_labels": [food["label"] for food in detected_foods],
            "analysis_results": detected_foods,
            "overall_average_confidence": overall_confidence
        }
        
        return JSONResponse(content=response_data)

    except Exception as e:
        print(f"Prediction Error: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"이미지 처리 중 서버 오류가 발생했습니다: {e}"
        )
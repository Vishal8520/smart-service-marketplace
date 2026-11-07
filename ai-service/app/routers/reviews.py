import time
from fastapi import APIRouter, Depends
from app.security import verify_internal_api_key
from app.schemas.reviews import (
    ReviewAnalyzeRequest,
    ReviewAnalyzeResponse,
    ServiceReviewSummaryRequest,
    ServiceReviewSummaryResponse
)
from app.services.review_service import review_service
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/reviews", tags=["Review Intelligence"])

@router.post("/analyze", response_model=ReviewAnalyzeResponse, dependencies=[Depends(verify_internal_api_key)])
def analyze_review_text(req: ReviewAnalyzeRequest):
    t0 = time.time()
    res = review_service.analyze_review(req)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/reviews/analyze",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"review_id": req.review_id, "sentiment": res.sentiment, "suspicious": res.is_suspicious_or_fake}
    )
    return res

@router.post("/summarize", response_model=ServiceReviewSummaryResponse, dependencies=[Depends(verify_internal_api_key)])
def summarize_service_reviews(req: ServiceReviewSummaryRequest):
    t0 = time.time()
    res = review_service.summarize_service_reviews(req)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/reviews/summarize",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"service_id": req.service_id, "review_count": req.reviews.__len__()}
    )
    return res

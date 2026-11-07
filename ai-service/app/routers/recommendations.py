import time
from fastapi import APIRouter, Depends
from app.security import verify_internal_api_key
from app.schemas.recommendations import RecommendationRequest, RecommendationResponse
from app.services.recommendation_service import recommendation_service
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/recommendations", tags=["Recommendations & Ranking"])

@router.post("/rank", response_model=RecommendationResponse, dependencies=[Depends(verify_internal_api_key)])
def rank_services(req: RecommendationRequest):
    t0 = time.time()
    res = recommendation_service.rank_candidates(
        candidates=req.candidates,
        query=req.query,
        user_city=req.user_city,
        limit=req.limit
    )
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/recommendations/rank",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"candidate_count": len(req.candidates), "user_city": req.user_city}
    )
    return res

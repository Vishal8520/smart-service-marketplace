import time
from fastapi import APIRouter, Depends
from app.security import verify_internal_api_key
from app.schemas.fraud import PaymentFeatures, FraudScoreResponse
from app.services.fraud_service import fraud_service
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/fraud", tags=["Payment Fraud Scoring"])

@router.post("/score", response_model=FraudScoreResponse, dependencies=[Depends(verify_internal_api_key)])
def score_payment_fraud(features: PaymentFeatures):
    t0 = time.time()
    res = fraud_service.score_payment(features)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/fraud/score",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={
            "payment_id": features.payment_id,
            "risk_score": res.risk_score,
            "risk_level": res.risk_level,
            "flagged": res.flagged_for_review
        }
    )
    return res

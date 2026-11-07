import time
from fastapi import APIRouter, Depends
from app.security import verify_internal_api_key
from app.schemas.listing import ListingAssistRequest, ListingAssistResponse
from app.services.listing_service import listing_service
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/listing", tags=["Provider Listing Assistant"])

@router.post("/assist", response_model=ListingAssistResponse, dependencies=[Depends(verify_internal_api_key)])
def assist_provider_listing(req: ListingAssistRequest):
    t0 = time.time()
    res = listing_service.assist_listing(req)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/listing/assist",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"category": req.category, "is_compliant": res.is_compliant}
    )
    return res

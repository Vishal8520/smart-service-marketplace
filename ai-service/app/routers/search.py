import time
from typing import Optional
from fastapi import APIRouter, Depends, Query
from app.security import verify_internal_api_key
from app.schemas.search import (
    IndexSyncRequest,
    SmartSearchRequest,
    SmartSearchResponse
)
from app.services.search_service import search_service
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/search", tags=["Semantic Search"])

@router.post("/sync", dependencies=[Depends(verify_internal_api_key)])
def sync_search_index(req: IndexSyncRequest):
    search_service.sync_index(req.services)
    return {"status": "SUCCESS", "synced_count": len(req.services)}

@router.post("/smart", response_model=SmartSearchResponse, dependencies=[Depends(verify_internal_api_key)])
def smart_search_post(req: SmartSearchRequest):
    t0 = time.time()
    res = search_service.search(query=req.query, city=req.city, limit=req.limit)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/search/smart",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"query": req.query, "city": req.city, "count": res.total}
    )
    return res

@router.get("/smart", response_model=SmartSearchResponse, dependencies=[Depends(verify_internal_api_key)])
def smart_search_get(
    q: str = Query(..., min_length=1, max_length=500),
    city: Optional[str] = Query(None),
    limit: int = Query(10, ge=1, le=50)
):
    t0 = time.time()
    res = search_service.search(query=q, city=city, limit=limit)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/search/smart",
        latency_ms=latency_ms,
        fallback_used=False,
        metadata={"query": q, "city": city, "count": res.total}
    )
    return res

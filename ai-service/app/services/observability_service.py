import datetime
import json
import logging
import time
from typing import Dict, Any, List
from prometheus_client import Counter, Histogram
from app.schemas.feedback import AdminFeedbackRequest, AdminFeedbackResponse

log = logging.getLogger("ai_observability")

# Prometheus Metrics
AI_REQUEST_COUNT = Counter(
    "ai_service_requests_total",
    "Total count of requests processed by AI service",
    ["endpoint", "status"]
)

AI_LATENCY_HISTOGRAM = Histogram(
    "ai_service_latency_seconds",
    "Latency of AI operations in seconds",
    ["endpoint"]
)

AI_FALLBACK_COUNT = Counter(
    "ai_service_fallback_total",
    "Total count of fallback activations",
    ["feature"]
)

class ObservabilityService:
    def __init__(self):
        self.feedback_store: List[Dict[str, Any]] = []

    def record_call(self, endpoint: str, latency_ms: float, fallback_used: bool, metadata: Dict[str, Any]):
        status = "fallback" if fallback_used else "success"
        AI_REQUEST_COUNT.labels(endpoint=endpoint, status=status).inc()
        AI_LATENCY_HISTOGRAM.labels(endpoint=endpoint).observe(latency_ms / 1000.0)

        if fallback_used:
            AI_FALLBACK_COUNT.labels(feature=endpoint).inc()

        # Sanitize metadata (strip any emails, phone numbers, or passwords)
        sanitized = self._sanitize_metadata(metadata)

        log.info(
            json.dumps({
                "timestamp": datetime.datetime.utcnow().isoformat(),
                "event": "ai_inference",
                "endpoint": endpoint,
                "latency_ms": round(latency_ms, 2),
                "fallback_used": fallback_used,
                "metadata": sanitized
            })
        )

    def _sanitize_metadata(self, meta: Dict[str, Any]) -> Dict[str, Any]:
        sanitized = {}
        for k, v in meta.items():
            if k in ["password", "email", "phone", "address", "secret", "token"]:
                sanitized[k] = "[REDACTED]"
            elif isinstance(v, dict):
                sanitized[k] = self._sanitize_metadata(v)
            else:
                sanitized[k] = v
        return sanitized

    def record_feedback(self, req: AdminFeedbackRequest) -> AdminFeedbackResponse:
        entry = {
            "feature": req.feature,
            "reference_id": req.reference_id,
            "is_correct": req.is_correct,
            "feedback_notes": req.feedback_notes,
            "timestamp": datetime.datetime.utcnow().isoformat()
        }
        self.feedback_store.append(entry)
        log.info("Admin feedback recorded: %s", json.dumps(entry))

        return AdminFeedbackResponse(
            status="SUCCESS",
            message=f"Feedback for {req.feature} (ref: {req.reference_id}) successfully logged.",
            recorded_at=entry["timestamp"]
        )

    def get_feedback_records(self) -> List[Dict[str, Any]]:
        return list(self.feedback_store)

observability_service = ObservabilityService()

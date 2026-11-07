from fastapi import APIRouter, Depends
from app.security import verify_internal_api_key
from app.schemas.feedback import AdminFeedbackRequest, AdminFeedbackResponse
from app.services.observability_service import observability_service

router = APIRouter(prefix="/api/v1/feedback", tags=["AI Feedback & Retraining"])

@router.post("", response_model=AdminFeedbackResponse, dependencies=[Depends(verify_internal_api_key)])
def submit_admin_feedback(req: AdminFeedbackRequest):
    return observability_service.record_feedback(req)

@router.get("", dependencies=[Depends(verify_internal_api_key)])
def get_admin_feedback():
    return {"feedback": observability_service.get_feedback_records()}

import time
from fastapi import APIRouter, Depends, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from app.security import verify_internal_api_key
from app.schemas.chatbot import ChatbotRequest, ChatbotResponse
from app.services.chatbot_service import chatbot_service
from app.services.observability_service import observability_service

limiter = Limiter(key_func=get_remote_address)
router = APIRouter(prefix="/api/v1/chatbot", tags=["Booking Chatbot"])

@router.post("/message", response_model=ChatbotResponse, dependencies=[Depends(verify_internal_api_key)])
@limiter.limit("30/minute")
def chat_message(req: ChatbotRequest, request: Request):
    t0 = time.time()
    res = chatbot_service.process_chat(req)
    latency_ms = (time.time() - t0) * 1000.0
    observability_service.record_call(
        endpoint="/api/v1/chatbot/message",
        latency_ms=latency_ms,
        fallback_used=chatbot_service.client is None,
        metadata={
            "user_id": req.context.user_id,
            "action_required": res.action_required,
            "tools_count": len(res.tools_called)
        }
    )
    return res

from typing import List, Optional, Any, Dict
from pydantic import BaseModel, Field

class ChatMessage(BaseModel):
    role: str  # user, assistant, system
    content: str = Field(..., max_length=1000)

class ChatbotContext(BaseModel):
    user_id: int
    user_name: str
    user_role: str
    user_email: str

class ChatbotRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=500)
    conversation_history: List[ChatMessage] = []
    context: ChatbotContext

class ToolCall(BaseModel):
    tool_name: str
    arguments: Dict[str, Any]
    result: Optional[Dict[str, Any]] = None

class ChatbotResponse(BaseModel):
    reply: str
    action_required: Optional[str] = None  # e.g., "CONFIRM_BOOKING"
    draft_booking: Optional[Dict[str, Any]] = None
    tools_called: List[ToolCall] = []

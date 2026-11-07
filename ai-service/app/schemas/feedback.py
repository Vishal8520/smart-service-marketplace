from typing import Optional
from pydantic import BaseModel, Field

class AdminFeedbackRequest(BaseModel):
    feature: str = Field(..., max_length=50)  # fraud, review, search, recommendation
    reference_id: str = Field(..., max_length=100)
    is_correct: bool
    feedback_notes: Optional[str] = Field(default=None, max_length=500)

class AdminFeedbackResponse(BaseModel):
    status: str
    message: str
    recorded_at: str

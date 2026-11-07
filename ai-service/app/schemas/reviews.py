from typing import List, Optional
from pydantic import BaseModel, Field

class ReviewAnalyzeRequest(BaseModel):
    review_id: int
    service_id: int
    rating: int = Field(..., ge=1, le=5)
    comment: str = Field(..., max_length=1000)

class ReviewAnalyzeResponse(BaseModel):
    review_id: int
    sentiment: str  # POSITIVE, NEUTRAL, NEGATIVE
    sentiment_score: float = Field(..., ge=-1.0, le=1.0)
    is_suspicious_or_fake: bool
    confidence: float = Field(..., ge=0.0, le=1.0)
    flags: List[str]

class ServiceReviewSummaryRequest(BaseModel):
    service_id: int
    reviews: List[ReviewAnalyzeRequest]

class ServiceReviewSummaryResponse(BaseModel):
    service_id: int
    total_reviews: int
    positive_percentage: float
    summary: str
    pros: List[str]
    cons: List[str]

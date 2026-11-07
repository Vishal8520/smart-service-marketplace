from typing import List, Optional
from pydantic import BaseModel, Field

class CandidateService(BaseModel):
    id: int
    title: str
    category: str
    price: float
    rating: float = 0.0
    city: str
    provider_response_rate: float = Field(default=0.95, ge=0.0, le=1.0)
    provider_id: int = 0

class RecommendationRequest(BaseModel):
    query: Optional[str] = None
    user_city: Optional[str] = None
    candidates: List[CandidateService]
    limit: int = Field(default=5, ge=1, le=20)

class RankedServiceItem(BaseModel):
    id: int
    title: str
    category: str
    price: float
    rating: float
    city: str
    final_score: float
    why_recommended: str

class RecommendationResponse(BaseModel):
    recommendations: List[RankedServiceItem]

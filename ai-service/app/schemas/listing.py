from typing import List, Optional
from pydantic import BaseModel, Field

class ListingAssistRequest(BaseModel):
    category: str
    title_keywords: str = Field(..., max_length=200)
    raw_notes: Optional[str] = Field(default="", max_length=1000)
    city: Optional[str] = None
    benchmark_prices: Optional[List[float]] = None

class ListingAssistResponse(BaseModel):
    suggested_title: str
    generated_description: str
    suggested_price_min: float
    suggested_price_max: float
    suggested_price_median: float
    policy_violations: List[str]
    is_compliant: bool

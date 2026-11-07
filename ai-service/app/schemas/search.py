from typing import List, Optional
from pydantic import BaseModel, Field

class ServiceDocument(BaseModel):
    id: int
    title: str
    category: str
    description: str
    city: str
    price: float
    rating: float = 0.0

class IndexSyncRequest(BaseModel):
    services: List[ServiceDocument]

class SmartSearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=500)
    city: Optional[str] = None
    limit: int = Field(default=10, ge=1, le=50)

class SearchResultItem(BaseModel):
    id: int
    title: str
    category: str
    description: str
    city: str
    price: float
    rating: float
    relevance_score: float

class SmartSearchResponse(BaseModel):
    query: str
    detected_category: Optional[str] = None
    results: List[SearchResultItem]
    total: int

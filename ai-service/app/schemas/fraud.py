from typing import List
from pydantic import BaseModel, Field

class PaymentFeatures(BaseModel):
    payment_id: int
    user_id: int
    amount: float = Field(..., gt=0)
    expected_amount: float = Field(..., gt=0)
    upi_reference_id: str
    velocity_last_10m: int = Field(default=0, ge=0)
    account_age_hours: float = Field(default=24.0, ge=0)
    consecutive_failed_payments: int = Field(default=0, ge=0)
    is_reference_reused: bool = False

class FraudScoreResponse(BaseModel):
    payment_id: int
    risk_score: float = Field(..., ge=0.0, le=1.0)
    risk_level: str  # LOW, MEDIUM, HIGH
    reasons: List[str]
    flagged_for_review: bool

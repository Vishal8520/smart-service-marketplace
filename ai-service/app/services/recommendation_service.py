from typing import List, Optional
from app.schemas.recommendations import CandidateService, RankedServiceItem, RecommendationResponse

class RecommendationService:
    def rank_candidates(
        self,
        candidates: List[CandidateService],
        query: Optional[str] = None,
        user_city: Optional[str] = None,
        limit: int = 5
    ) -> RecommendationResponse:
        if not candidates:
            return RecommendationResponse(recommendations=[])

        max_price = max((c.price for c in candidates), default=1.0)
        min_price = min((c.price for c in candidates), default=1.0)
        price_spread = max(1.0, max_price - min_price)

        ranked: List[RankedServiceItem] = []

        for c in candidates:
            # 1. Rating component (0.45 weight) - quality first
            rating_norm = (c.rating / 5.0) if c.rating > 0 else 0.5
            rating_score = rating_norm * 0.45

            # 2. Provider reliability / response rate (0.25 weight)
            resp_score = (c.provider_response_rate or 0.9) * 0.25

            # 3. City proximity / match (0.20 weight)
            city_match = False
            if user_city and user_city.strip():
                if user_city.strip().lower() == c.city.strip().lower():
                    city_score = 0.20
                    city_match = True
                else:
                    city_score = 0.05
            else:
                city_score = 0.15

            # 4. Price competitiveness (0.10 weight)
            price_norm = 1.0 - ((c.price - min_price) / price_spread)
            price_score = max(0.0, min(1.0, price_norm)) * 0.10

            final_score = rating_score + resp_score + city_score + price_score

            # Construct explainable "why recommended" rationale
            reasons = []
            if city_match:
                reasons.append(f"Available locally in {c.city}")
            if c.rating >= 4.5:
                reasons.append(f"exceptional {c.rating}★ customer rating")
            elif c.rating >= 4.0:
                reasons.append(f"solid {c.rating}★ rating")

            if c.price <= min_price + (price_spread * 0.33):
                reasons.append("budget-friendly pricing")
            elif c.price >= max_price - (price_spread * 0.2):
                reasons.append("premium service tier")

            if (c.provider_response_rate or 0.0) >= 0.95:
                reasons.append(f"{int(c.provider_response_rate * 100)}% rapid response rate")

            why_str = f"Recommended: {', '.join(reasons)}." if reasons else "Recommended based on overall service quality."

            ranked.append(RankedServiceItem(
                id=c.id,
                title=c.title,
                category=c.category,
                price=c.price,
                rating=c.rating,
                city=c.city,
                final_score=round(final_score, 3),
                why_recommended=why_str
            ))

        ranked.sort(key=lambda x: x.final_score, reverse=True)
        return RecommendationResponse(recommendations=ranked[:limit])

recommendation_service = RecommendationService()

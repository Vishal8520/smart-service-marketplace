import re
from typing import List, Optional
import numpy as np
from app.schemas.listing import ListingAssistRequest, ListingAssistResponse

CATEGORY_PRICE_BENCHMARKS = {
    "Plumbing": (299.0, 799.0, 499.0),
    "Electrical": (199.0, 699.0, 349.0),
    "Cleaning": (799.0, 2499.0, 1499.0),
    "Appliance Repair": (499.0, 1599.0, 899.0),
    "Painting": (2500.0, 8000.0, 4500.0),
    "Carpentry": (399.0, 1200.0, 650.0),
}

PROHIBITED_PATTERNS = [
    (r"\b\d{10}\b", "Contains personal direct telephone number (off-platform contact violation)"),
    (r"(whatsapp|call me on|direct pay|cash only|bypass marketplace)", "Solicits off-platform contact or unrecorded cash transactions"),
    (r"(guaranteed riches|bitcoin|lottery|crypto|porn|nude|casino)", "Violates content and commercial integrity policy")
]

class ListingService:
    def assist_listing(self, request: ListingAssistRequest) -> ListingAssistResponse:
        combined_text = f"{request.title_keywords} {request.raw_notes}"
        violations = []

        # 1. Policy screening
        for pattern, explanation in PROHIBITED_PATTERNS:
            if re.search(pattern, combined_text, re.IGNORECASE):
                violations.append(explanation)

        is_compliant = len(violations) == 0

        # 2. Suggested Title
        keywords_clean = [w.capitalize() for w in request.title_keywords.strip().split() if len(w) > 2]
        title_body = " ".join(keywords_clean) if keywords_clean else "Home Maintenance Service"
        suggested_title = f"Professional {title_body} ({request.category})"

        # 3. Clean Description Generation
        city_suffix = f" across {request.city} and neighboring areas" if request.city else ""
        generated_desc = (
            f"Certified and thoroughly verified {request.category.lower()} service{city_suffix}. "
            f"We specialize in {request.title_keywords.lower()}, adhering to standard safety, cleanliness, "
            f"and transparent pricing. All tools and inspection equipment provided. "
            f"Backed by the ServeNow Service Quality Guarantee with 100% verified customer ratings."
        )
        if request.raw_notes and request.raw_notes.strip():
            clean_notes = re.sub(r"\b\d{10}\b", "[REDACTED]", request.raw_notes.strip())
            generated_desc += f" Scope of work includes: {clean_notes}."

        # 4. Fair Price Range Calculation
        if request.benchmark_prices and len(request.benchmark_prices) >= 2:
            prices = np.array(request.benchmark_prices)
            p_min = float(np.percentile(prices, 20))
            p_max = float(np.percentile(prices, 80))
            p_median = float(np.median(prices))
        else:
            cat_defaults = CATEGORY_PRICE_BENCHMARKS.get(
                request.category, (350.0, 1200.0, 599.0)
            )
            p_min, p_max, p_median = cat_defaults

        return ListingAssistResponse(
            suggested_title=suggested_title,
            generated_description=generated_desc,
            suggested_price_min=round(p_min, 2),
            suggested_price_max=round(p_max, 2),
            suggested_price_median=round(p_median, 2),
            policy_violations=violations,
            is_compliant=is_compliant
        )

listing_service = ListingService()

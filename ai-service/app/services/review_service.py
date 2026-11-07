import re
from typing import List
from app.schemas.reviews import (
    ReviewAnalyzeRequest,
    ReviewAnalyzeResponse,
    ServiceReviewSummaryRequest,
    ServiceReviewSummaryResponse
)

POSITIVE_WORDS = {
    "excellent", "great", "amazing", "good", "punctual", "clean", "professional",
    "recommend", "polite", "quick", "fast", "skilled", "perfect", "satisfied", "helpful", "best"
}
NEGATIVE_WORDS = {
    "terrible", "worst", "bad", "late", "dirty", "rude", "unprofessional", "waste",
    "overcharged", "broken", "scam", "poor", "horrible", "delay", "careless", "damage"
}
SPAM_PATTERNS = [
    r"http[s]?://",
    r"www\.",
    r"\b\d{10}\b",  # phone numbers
    r"(buy|call|whatsapp|discount|promo code)\b",
    r"(.)\1{4,}"    # excessive repeated characters e.g. "aaaaaa"
]

class ReviewService:
    def analyze_review(self, request: ReviewAnalyzeRequest) -> ReviewAnalyzeResponse:
        text = request.comment.lower()
        words = set(re.findall(r"\b\w+\b", text))
        
        pos_count = sum(1 for w in words if w in POSITIVE_WORDS)
        neg_count = sum(1 for w in words if w in NEGATIVE_WORDS)

        # Baseline sentiment score (-1.0 to 1.0)
        total_sentiment_words = pos_count + neg_count
        if total_sentiment_words > 0:
            sentiment_score = (pos_count - neg_count) / total_sentiment_words
        else:
            # Fall back to rating alignment
            sentiment_score = (request.rating - 3.0) / 2.0

        sentiment_score = max(-1.0, min(1.0, sentiment_score))
        if sentiment_score >= 0.2:
            sentiment = "POSITIVE"
        elif sentiment_score <= -0.2:
            sentiment = "NEGATIVE"
        else:
            sentiment = "NEUTRAL"

        # Spam / Fake review detection
        flags = []
        is_suspicious = False

        for pattern in SPAM_PATTERNS:
            if re.search(pattern, text, re.IGNORECASE):
                flags.append("Contains suspicious link, contact information, or promotional spam pattern")
                is_suspicious = True
                break

        # Sentiment vs rating contradiction (e.g. 5 stars with "terrible worst late", or 1 star with "best amazing")
        if request.rating == 5 and neg_count >= 2 and pos_count == 0:
            flags.append("Severe sentiment-rating contradiction (5 stars but negative text)")
            is_suspicious = True
        elif request.rating == 1 and pos_count >= 2 and neg_count == 0:
            flags.append("Severe sentiment-rating contradiction (1 star but glowing text)")
            is_suspicious = True

        # Very low word count with extreme rating
        if len(text.strip().split()) <= 2 and request.rating in [1, 5] and len(text) < 10:
            flags.append("Extremely brief uninformative review text")

        confidence = 0.90 if is_suspicious or total_sentiment_words >= 3 else 0.70

        return ReviewAnalyzeResponse(
            review_id=request.review_id,
            sentiment=sentiment,
            sentiment_score=round(sentiment_score, 2),
            is_suspicious_or_fake=is_suspicious,
            confidence=round(confidence, 2),
            flags=flags
        )

    def summarize_service_reviews(self, request: ServiceReviewSummaryRequest) -> ServiceReviewSummaryResponse:
        reviews = request.reviews
        if not reviews:
            return ServiceReviewSummaryResponse(
                service_id=request.service_id,
                total_reviews=0,
                positive_percentage=0.0,
                summary="No customer reviews submitted yet for this service.",
                pros=[],
                cons=[]
            )

        analyzed = [self.analyze_review(r) for r in reviews]
        positive_count = sum(1 for a in analyzed if a.sentiment == "POSITIVE")
        pos_pct = round((positive_count / len(reviews)) * 100.0, 1)

        # Extract recurring highlights
        all_text = " ".join(r.comment.lower() for r in reviews)
        pros = []
        cons = []

        if any(w in all_text for w in ["punctual", "on time", "quick", "fast"]):
            pros.append("Punctual and prompt response time")
        if any(w in all_text for w in ["clean", "neat", "tidy"]):
            pros.append("Maintains cleanliness and neat work area")
        if any(w in all_text for w in ["polite", "friendly", "professional"]):
            pros.append("Courteous and professional provider demeanor")
        if any(w in all_text for w in ["skilled", "expert", "quality", "perfect"]):
            pros.append("High quality workmanship and diagnosis")

        if any(w in all_text for w in ["late", "delay"]):
            cons.append("Occasional scheduling or arrival delays reported")
        if any(w in all_text for w in ["expensive", "overcharge", "costly"]):
            cons.append("Some customers noted higher than expected costs for spare parts")
        if any(w in all_text for w in ["mess", "dirty"]):
            cons.append("Post-service clean-up could be improved")

        # Summary text
        if pos_pct >= 80:
            summary_text = f"Highly rated service with {pos_pct}% positive feedback. Customers consistently appreciate the promptness and professional execution."
        elif pos_pct >= 50:
            summary_text = f"Generally favorable customer reception ({pos_pct}% positive). Reliable performance with a few isolated complaints regarding timing or parts."
        else:
            summary_text = f"Mixed to critical reception with only {pos_pct}% positive sentiment. Reviewers frequently cited delays and communication gaps."

        return ServiceReviewSummaryResponse(
            service_id=request.service_id,
            total_reviews=len(reviews),
            positive_percentage=pos_pct,
            summary=summary_text,
            pros=pros if pros else ["Satisfied overall service delivery"],
            cons=cons
        )

review_service = ReviewService()

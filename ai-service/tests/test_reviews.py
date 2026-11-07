def test_positive_review_sentiment(client, auth_headers):
    payload = {
        "review_id": 1,
        "service_id": 10,
        "rating": 5,
        "comment": "Excellent work! Plumber was very punctual, skilled and clean."
    }

    res = client.post("/api/v1/reviews/analyze", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["sentiment"] == "POSITIVE"
    assert data["sentiment_score"] > 0.3
    assert data["is_suspicious_or_fake"] is False

def test_spam_detection_in_review(client, auth_headers):
    payload = {
        "review_id": 2,
        "service_id": 10,
        "rating": 5,
        "comment": "Cheap service! Call me on 9876543210 or visit http://scam.example.com to buy promo code"
    }

    res = client.post("/api/v1/reviews/analyze", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["is_suspicious_or_fake"] is True
    assert len(data["flags"]) > 0

def test_service_review_summarization(client, auth_headers):
    payload = {
        "service_id": 10,
        "reviews": [
            {"review_id": 1, "service_id": 10, "rating": 5, "comment": "Great punctual and polite professional."},
            {"review_id": 2, "service_id": 10, "rating": 5, "comment": "Skilled and quick fix for broken tap."},
            {"review_id": 3, "service_id": 10, "rating": 2, "comment": "Arrived late but finished cleanly."}
        ]
    }

    res = client.post("/api/v1/reviews/summarize", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["total_reviews"] == 3
    assert data["positive_percentage"] >= 60.0
    assert "summary" in data
    assert len(data["pros"]) > 0

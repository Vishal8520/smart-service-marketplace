def test_recommendation_ranking(client, auth_headers):
    payload = {
        "user_city": "Bengaluru",
        "candidates": [
            {
                "id": 1,
                "title": "Cheap Plumbing",
                "category": "Plumbing",
                "price": 200.0,
                "rating": 3.8,
                "city": "Bengaluru",
                "provider_response_rate": 0.85
            },
            {
                "id": 2,
                "title": "Top Rated Plumbing",
                "category": "Plumbing",
                "price": 350.0,
                "rating": 4.9,
                "city": "Bengaluru",
                "provider_response_rate": 0.98
            },
            {
                "id": 3,
                "title": "Far City Plumbing",
                "category": "Plumbing",
                "price": 300.0,
                "rating": 4.5,
                "city": "Delhi",
                "provider_response_rate": 0.90
            }
        ],
        "limit": 3
    }

    res = client.post("/api/v1/recommendations/rank", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert len(data["recommendations"]) == 3
    # Top ranked should be the 4.9 star Bengaluru service with 98% response rate
    top = data["recommendations"][0]
    assert top["id"] == 2
    assert "why_recommended" in top
    assert len(top["why_recommended"]) > 10
    assert "exceptional 4.9★" in top["why_recommended"] or "Bengaluru" in top["why_recommended"]

def test_listing_assistant_clean(client, auth_headers):
    payload = {
        "category": "Plumbing",
        "title_keywords": "kitchen sink pipe leakage repair",
        "raw_notes": "We replace washers and tighten connections",
        "city": "Bengaluru",
        "benchmark_prices": [350.0, 450.0, 500.0, 600.0, 750.0]
    }

    res = client.post("/api/v1/listing/assist", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["is_compliant"] is True
    assert len(data["policy_violations"]) == 0
    assert "Plumbing" in data["suggested_title"]
    assert len(data["generated_description"]) > 50
    assert data["suggested_price_min"] < data["suggested_price_max"]
    assert data["suggested_price_median"] == 500.0

def test_listing_assistant_policy_violation(client, auth_headers):
    payload = {
        "category": "Electrical",
        "title_keywords": "fan repair",
        "raw_notes": "Call me directly on 9876543210 whatsapp for direct pay cash only bypass marketplace",
        "city": "Delhi"
    }

    res = client.post("/api/v1/listing/assist", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["is_compliant"] is False
    assert len(data["policy_violations"]) >= 1

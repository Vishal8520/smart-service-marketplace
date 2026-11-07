def test_normal_payment_scoring(client, auth_headers):
    payload = {
        "payment_id": 1001,
        "user_id": 42,
        "amount": 499.0,
        "expected_amount": 499.0,
        "upi_reference_id": "123456789012",
        "velocity_last_10m": 1,
        "account_age_hours": 120.0,
        "consecutive_failed_payments": 0,
        "is_reference_reused": False
    }

    res = client.post("/api/v1/fraud/score", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["payment_id"] == 1001
    assert data["risk_score"] < 0.35
    assert data["risk_level"] == "LOW"
    assert data["flagged_for_review"] is False

def test_high_risk_reused_reference_scoring(client, auth_headers):
    payload = {
        "payment_id": 1002,
        "user_id": 55,
        "amount": 499.0,
        "expected_amount": 499.0,
        "upi_reference_id": "999988887777",
        "velocity_last_10m": 1,
        "account_age_hours": 50.0,
        "consecutive_failed_payments": 0,
        "is_reference_reused": True  # Replay attack
    }

    res = client.post("/api/v1/fraud/score", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["risk_score"] >= 0.65
    assert data["risk_level"] == "HIGH"
    assert data["flagged_for_review"] is True
    assert any("replay" in r.lower() or "previously submitted" in r.lower() for r in data["reasons"])

def test_amount_mismatch_and_velocity_spike(client, auth_headers):
    payload = {
        "payment_id": 1003,
        "user_id": 99,
        "amount": 10.0,
        "expected_amount": 1500.0,  # Underpayment attack
        "upi_reference_id": "555566667777",
        "velocity_last_10m": 5,     # Velocity spike
        "account_age_hours": 0.5,   # Brand new account
        "consecutive_failed_payments": 3,
        "is_reference_reused": False
    }

    res = client.post("/api/v1/fraud/score", headers=auth_headers, json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["risk_score"] >= 0.65
    assert data["risk_level"] == "HIGH"
    assert data["flagged_for_review"] is True

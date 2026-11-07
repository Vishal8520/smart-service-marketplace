def test_admin_feedback_submission_and_metrics(client, auth_headers):
    # 1. Submit feedback
    payload = {
        "feature": "fraud_detection",
        "reference_id": "pay-1002",
        "is_correct": True,
        "feedback_notes": "Correctly caught reused UTR"
    }

    res = client.post("/api/v1/feedback", headers=auth_headers, json=payload)
    assert res.status_code == 200
    assert res.json()["status"] == "SUCCESS"

    # 2. Retrieve feedback
    res2 = client.get("/api/v1/feedback", headers=auth_headers)
    assert res2.status_code == 200
    feedbacks = res2.json()["feedback"]
    assert len(feedbacks) >= 1
    assert feedbacks[-1]["reference_id"] == "pay-1002"

    # 3. Check prometheus metrics endpoint
    m_res = client.get("/metrics")
    assert m_res.status_code == 200
    assert "ai_service_requests_total" in m_res.text

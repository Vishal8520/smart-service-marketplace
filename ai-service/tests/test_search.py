def test_health_check(client):
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "UP"

def test_search_unauthorized(client):
    res = client.get("/api/v1/search/smart?q=leak")
    assert res.status_code == 401

def test_sync_and_search_plumbing(client, auth_headers, sample_services):
    # 1. Sync sample services into search index
    sync_res = client.post("/api/v1/search/sync", headers=auth_headers, json={"services": sample_services})
    assert sync_res.status_code == 200
    assert sync_res.json()["synced_count"] == 3

    # 2. Search with natural language: "my tap is leaking tonight"
    res = client.get("/api/v1/search/smart?q=my tap is leaking tonight&city=Bengaluru", headers=auth_headers)
    assert res.status_code == 200
    data = res.json()

    assert data["detected_category"] == "Plumbing"
    assert data["total"] > 0
    top_hit = data["results"][0]
    assert top_hit["id"] == 101
    assert "Pipe Repair" in top_hit["title"]
    assert top_hit["city"] == "Bengaluru"

def test_search_city_filtering(client, auth_headers, sample_services):
    client.post("/api/v1/search/sync", headers=auth_headers, json={"services": sample_services})

    res = client.get("/api/v1/search/smart?q=leak&city=Mumbai", headers=auth_headers)
    assert res.status_code == 200
    data = res.json()
    assert all(item["city"] == "Mumbai" for item in data["results"])

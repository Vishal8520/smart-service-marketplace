import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def auth_headers():
    return {"X-Internal-API-Key": settings.internal_api_key}

@pytest.fixture
def sample_services():
    return [
        {
            "id": 101,
            "title": "Kitchen Sink and Pipe Repair",
            "category": "Plumbing",
            "description": "Expert fix for blocked sink pipes and leaking taps.",
            "city": "Bengaluru",
            "price": 450.0,
            "rating": 4.8
        },
        {
            "id": 102,
            "title": "Living Room Sofa Cleaning",
            "category": "Cleaning",
            "description": "Shampoo and vacuum deep cleaning for fabric sofas.",
            "city": "Bengaluru",
            "price": 899.0,
            "rating": 4.6
        },
        {
            "id": 103,
            "title": "Bathroom Leak Fix & Waterproofing",
            "category": "Plumbing",
            "description": "Wall leakage diagnosis and pipe sealing.",
            "city": "Mumbai",
            "price": 600.0,
            "rating": 4.9
        }
    ]

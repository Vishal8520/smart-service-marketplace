import logging
import sys
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST

from app.config import settings
from app.routers import (
    search,
    recommendations,
    fraud,
    reviews,
    listing,
    chatbot,
    feedback
)

# Logging configuration
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
log = logging.getLogger("ai_service")

# SlowAPI Limiter
limiter = Limiter(key_func=get_remote_address)

app = FastAPI(
    title=settings.app_name,
    version=settings.version,
    description="Microservice providing AI Intelligence (Semantic Search, Recommendations, Fraud Detection, Review Intelligence, Listing Assistant, Booking Chatbot) for ServeNow Marketplace.",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(search.router)
app.include_router(recommendations.router)
app.include_router(fraud.router)
app.include_router(reviews.router)
app.include_router(listing.router)
app.include_router(chatbot.router)
app.include_router(feedback.router)

@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "UP",
        "service": "ai-service",
        "version": settings.version,
        "environment": settings.environment
    }

@app.get("/metrics", tags=["Observability"])
def prometheus_metrics():
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)

@app.get("/", tags=["Root"])
def root():
    return {
        "message": "ServeNow AI Service is operational",
        "docs": "/docs",
        "health": "/health"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)

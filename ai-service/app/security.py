from fastapi import Security, HTTPException, status
from fastapi.security import APIKeyHeader
from app.config import settings

API_KEY_HEADER = APIKeyHeader(name="X-Internal-API-Key", auto_error=False)

def verify_internal_api_key(api_key: str = Security(API_KEY_HEADER)):
    # In test mode or when matching configured key
    if not api_key or api_key != settings.internal_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-API-Key header",
            headers={"WWW-Authenticate": "ApiKey"},
        )
    return api_key

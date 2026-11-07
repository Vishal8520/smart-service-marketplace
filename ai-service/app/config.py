import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    app_name: str = "ServeNow AI Intelligence Service"
    version: str = "1.0.0"
    environment: str = os.getenv("ENVIRONMENT", "development")
    internal_api_key: str = os.getenv("AI_SERVICE_API_KEY", "servenow-internal-ai-key-secret-2026")
    anthropic_api_key: str = os.getenv("ANTHROPIC_API_KEY", "")
    anthropic_model: str = os.getenv("ANTHROPIC_MODEL", "claude-3-haiku-20240307")
    rate_limit_per_minute: int = int(os.getenv("AI_RATE_LIMIT_PER_MINUTE", "60"))

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

settings = Settings()

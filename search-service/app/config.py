"""
PVK Cinemas Search Service — Application Configuration
Reads environment variables (with defaults) via pydantic-settings.
All configuration must be externalized; secrets must not be hardcoded.
Authority: ARCH §18 (Environment Architecture), IMPL-BASE §J §3.
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="SEARCH_",
        extra="ignore",
    )

    # Database — read-only for indexing pipeline only
    db_url: str = "mysql+pymysql://root:root@127.0.0.1:3306/pvk_cinemas_db"

    # Embedding model name (sentence-transformers hub)
    embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2"

    # Hybrid ranking weights (must sum to 1.0)
    lexical_weight: float = 0.4
    semantic_weight: float = 0.6

    # Semantic similarity cutoff threshold (raw cosine similarity floor before min-max normalization)
    # Empirically calibrated: nonsense max = 0.4288, valid queries >= 0.47+
    semantic_min_score: float = 0.45

    # Service binding
    service_host: str = "127.0.0.1"
    service_port: int = 8001

    # Indexing pipeline
    pipeline_version: str = "v4.0"
    index_on_startup: bool = True

    # Internal service URL (used by Spring Boot client — not consumed here)
    # Documented here for reference only
    max_query_length: int = 500
    max_results: int = 50


settings = Settings()

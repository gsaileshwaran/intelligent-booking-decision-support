"""
PVK Cinemas Search Service — Query Processor
Normalizes and validates incoming search queries before retrieval.
Authority: API-SPEC §25, SRS §13 SEC-007.
"""

import re
import unicodedata


class QueryProcessor:
    """
    Validates and normalizes user-supplied query text.
    Rules:
    - Strip leading/trailing whitespace
    - Reject empty or whitespace-only queries
    - Reject queries exceeding max_length (default 500)
    - Normalize unicode to NFC form
    - Collapse multiple internal whitespace sequences to single space
    - Remove control characters
    Authority: API-SPEC §25 (validate/normalize input before use), SEC-007.
    """

    def __init__(self, max_length: int = 500) -> None:
        self.max_length = max_length

    def process(self, raw_query: str) -> str:
        """
        Validate and normalize a raw query string.
        :param raw_query: raw query from caller
        :return: normalized query string
        :raises ValueError: if query is invalid
        """
        if raw_query is None:
            raise ValueError("Query must not be null")

        # Unicode NFC normalization
        normalized = unicodedata.normalize("NFC", raw_query)

        # Remove control characters (keep printable + whitespace)
        normalized = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]", "", normalized)

        # Strip leading/trailing whitespace
        stripped = normalized.strip()

        if not stripped:
            raise ValueError("Query must not be empty or whitespace only")

        if len(stripped) > self.max_length:
            raise ValueError(
                f"Query exceeds maximum length of {self.max_length} characters (got {len(stripped)})"
            )

        # Collapse internal whitespace
        collapsed = re.sub(r"\s+", " ", stripped)

        return collapsed

    def tokenize(self, query: str) -> list[str]:
        """
        Tokenize a normalized query into terms for BM25 lookup.
        Simple whitespace split with lowercase normalization.
        """
        return query.lower().split()

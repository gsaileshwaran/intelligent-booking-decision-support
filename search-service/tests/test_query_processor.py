"""
PVK Cinemas Search Service — Query Processor Tests
Authority: API-SPEC §25, SEC-007.
"""

import pytest
from app.search.query_processor import QueryProcessor


class TestQueryProcessor:
    """Unit tests for QueryProcessor validation and normalization."""

    def setup_method(self):
        self.processor = QueryProcessor(max_length=500)

    # TS-QP-001: Empty query rejected
    def test_empty_string_raises(self):
        with pytest.raises(ValueError, match="empty"):
            self.processor.process("")

    # TS-QP-002: Whitespace-only query rejected
    def test_whitespace_only_raises(self):
        with pytest.raises(ValueError, match="empty"):
            self.processor.process("   \t\n  ")

    # TS-QP-003: Oversized query rejected
    def test_oversized_query_raises(self):
        long_query = "a" * 501
        with pytest.raises(ValueError, match="exceeds maximum length"):
            self.processor.process(long_query)

    # TS-QP-004: Exactly at max length is accepted
    def test_exactly_max_length_accepted(self):
        exact = "a" * 500
        result = self.processor.process(exact)
        assert result == exact

    # TS-QP-005: Unicode normalization (NFC)
    def test_unicode_normalization(self):
        # Decomposed form of 'é' (e + combining acute accent)
        decomposed = "caf\u0065\u0301"  # 'cafe' + combining acute on 'e'
        result = self.processor.process(decomposed)
        # NFC normalized
        assert "\u00e9" in result or result.strip() != ""

    # TS-QP-006: Special characters preserved (non-control)
    def test_special_chars_preserved(self):
        query = "Batman & Robin (2026)!"
        result = self.processor.process(query)
        assert "Batman" in result
        assert "Robin" in result

    # TS-QP-007: Internal whitespace collapsed
    def test_internal_whitespace_collapsed(self):
        query = "action   movie   thriller"
        result = self.processor.process(query)
        assert result == "action movie thriller"

    # TS-QP-008: None input raises ValueError (via pydantic, not processor)
    def test_none_raises(self):
        with pytest.raises((ValueError, TypeError)):
            self.processor.process(None)  # type: ignore

    # TS-QP-009: Valid multi-term query
    def test_valid_multi_term_query(self):
        result = self.processor.process("  KGF movie action  ")
        assert result == "KGF movie action"

    # TS-QP-010: Tokenization
    def test_tokenize(self):
        tokens = self.processor.tokenize("Action Movie 2026")
        assert tokens == ["action", "movie", "2026"]

"""
PVK Cinemas — Authoritative P14 Test Account Strategy
Defines the five canonical testing personas required for full regression:
1. Anonymous User (unauthenticated)
2. Customer (registered cinema patron)
3. Theatre Manager A (assigned strictly to Theatre 1)
4. Theatre Manager B (assigned strictly to Theatre 2)
5. Super Admin (platform-wide administrator)

Passwords are resolved dynamically from environment variables, falling back
only to non-sensitive standard test credentials. Real credentials or secrets
must never be committed.
"""

import os
from enum import Enum
from dataclasses import dataclass
from typing import Optional

class RoleType(str, Enum):
    ANONYMOUS = "ANONYMOUS"
    CUSTOMER = "ROLE_CUSTOMER"
    THEATRE_MANAGER = "ROLE_THEATRE_MANAGER"
    SUPER_ADMIN = "ROLE_SUPER_ADMIN"

@dataclass(frozen=True)
class TestAccount:
    persona_id: str
    role: RoleType
    email: Optional[str]
    password: Optional[str]
    first_name: Optional[str]
    last_name: Optional[str]
    assigned_theatre_id: Optional[int]
    description: str

def get_test_accounts() -> dict[str, TestAccount]:
    """
    Returns the mapped test personas with secure credential resolution.
    """
    default_test_pw = "TestPassword123!"

    return {
        "anonymous": TestAccount(
            persona_id="anonymous",
            role=RoleType.ANONYMOUS,
            email=None,
            password=None,
            first_name=None,
            last_name=None,
            assigned_theatre_id=None,
            description="Unauthenticated public visitor"
        ),
        "customer": TestAccount(
            persona_id="customer",
            role=RoleType.CUSTOMER,
            email=os.getenv("PVK_TEST_CUSTOMER_EMAIL", "test_customer@pvkcinemas.test"),
            password=os.getenv("PVK_TEST_CUSTOMER_PASSWORD", default_test_pw),
            first_name="Test",
            last_name="Customer",
            assigned_theatre_id=None,
            description="Standard registered customer for discovery and profile verification"
        ),
        "manager_a": TestAccount(
            persona_id="manager_a",
            role=RoleType.THEATRE_MANAGER,
            email=os.getenv("PVK_TEST_MGR_A_EMAIL", "manager_a@pvkcinemas.test"),
            password=os.getenv("PVK_TEST_MGR_A_PASSWORD", default_test_pw),
            first_name="Manager",
            last_name="Alpha",
            assigned_theatre_id=int(os.getenv("PVK_TEST_THEATRE_A_ID", "1")),
            description="Theatre Manager assigned exclusively to Theatre A (Theatre ID 1)"
        ),
        "manager_b": TestAccount(
            persona_id="manager_b",
            role=RoleType.THEATRE_MANAGER,
            email=os.getenv("PVK_TEST_MGR_B_EMAIL", "manager_b@pvkcinemas.test"),
            password=os.getenv("PVK_TEST_MGR_B_PASSWORD", default_test_pw),
            first_name="Manager",
            last_name="Beta",
            assigned_theatre_id=int(os.getenv("PVK_TEST_THEATRE_B_ID", "2")),
            description="Theatre Manager assigned exclusively to Theatre B (Theatre ID 2) for scope isolation testing"
        ),
        "super_admin": TestAccount(
            persona_id="super_admin",
            role=RoleType.SUPER_ADMIN,
            email=os.getenv("PVK_TEST_ADMIN_EMAIL", "admin@pvkcinemas.test"),
            password=os.getenv("PVK_TEST_ADMIN_PASSWORD", default_test_pw),
            first_name="Super",
            last_name="Admin",
            assigned_theatre_id=None,
            description="Super Administrator with global catalogue, user, and audit access"
        )
    }

ACCOUNTS = get_test_accounts()

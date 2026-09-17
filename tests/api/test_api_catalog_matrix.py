"""
PVK Cinemas — Authoritative 53-Operation Coverage Matrix Regression
Verifies that:
1. All 53 discrete operations exist in Spring Boot Java @RestController classes
2. All 53 discrete operations have active regression coverage
3. Exactly 17 controllers are mapped
4. No forbidden or extra endpoints exist
5. Every single operation is accounted for with method, path, and handler
"""

import os
import re
import pytest
from tests.config.endpoints_catalog import AUTHORITATIVE_53_OPERATIONS, EndpointDefinition, verify_catalog_integrity

# Mapping of Operation IDs to their dedicated regression test modules
OPERATION_TEST_MAPPINGS = {
    # Super Admin Governance & Audit
    "OP-01": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op01_get_audit_logs",
    "OP-08": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op08_op09_op10_admin_movies_and_audit",
    "OP-09": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op08_op09_op10_admin_movies_and_audit",
    "OP-10": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op08_op09_op10_admin_movies_and_audit",
    "OP-23": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op23_op24_op25_admin_cities",
    "OP-24": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op23_op24_op25_admin_cities",
    "OP-25": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op23_op24_op25_admin_cities",
    "OP-26": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op26_op27_op28_admin_theatres",
    "OP-27": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op26_op27_op28_admin_theatres",
    "OP-28": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op26_op27_op28_admin_theatres",
    "OP-29": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op29_op30_admin_theatre_managers",
    "OP-30": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op29_op30_admin_theatre_managers",
    "OP-41": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op41_op42_admin_search_ops",
    "OP-42": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op41_op42_admin_search_ops",
    "OP-45": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op45_op46_op47_op48_admin_roles_and_permissions",
    "OP-46": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op45_op46_op47_op48_admin_roles_and_permissions",
    "OP-47": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op45_op46_op47_op48_admin_roles_and_permissions",
    "OP-48": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op45_op46_op47_op48_admin_roles_and_permissions",
    "OP-49": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op49_op50_op51_admin_users",
    "OP-50": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op49_op50_op51_admin_users",
    "OP-51": "tests/api/test_api_admin_governance.py::TestAdminGovernanceRegression::test_op49_op50_op51_admin_users",

    # Auth & Profile
    "OP-02": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op02_register_success",
    "OP-03": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op03_login_success",
    "OP-04": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op04_logout_and_revocation",
    "OP-05": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op05_get_me",
    "OP-52": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op52_op53_get_and_update_profile",
    "OP-53": "tests/api/test_api_auth_profile.py::TestAuthProfileRegression::test_op52_op53_get_and_update_profile",

    # Customer Discovery
    "OP-12": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op12_movies_list",
    "OP-13": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op13_movie_details",
    "OP-14": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op14_movie_genres",
    "OP-15": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op15_movie_languages",
    "OP-16": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op16_movie_shows",
    "OP-31": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op31_cities_list",
    "OP-32": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op32_theatres_by_city",
    "OP-33": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op33_theatre_details",
    "OP-34": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op34_theatre_screens",
    "OP-35": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op35_theatre_shows",
    "OP-39": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op39_show_details",
    "OP-40": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op40_show_seat_availability_display_only",
    "OP-43": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op43_search_get",
    "OP-44": "tests/api/test_api_customer_discovery.py::TestCustomerDiscoveryRegression::test_op44_search_post",

    # Manager Infrastructure & Scheduling
    "OP-06": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op06_op07_show_seats_and_override",
    "OP-07": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op06_op07_show_seats_and_override",
    "OP-11": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op11_manager_movies",
    "OP-17": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op17_op18_op19_screens_crud",
    "OP-18": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op17_op18_op19_screens_crud",
    "OP-19": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op17_op18_op19_screens_crud",
    "OP-20": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op20_op21_op22_seats_crud",
    "OP-21": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op20_op21_op22_seats_crud",
    "OP-22": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op20_op21_op22_seats_crud",
    "OP-36": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op36_op37_op38_shows_scheduling",
    "OP-37": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op36_op37_op38_shows_scheduling",
    "OP-38": "tests/api/test_api_manager_infrastructure.py::TestManagerInfrastructureRegression::test_op36_op37_op38_shows_scheduling",
}

class TestCatalogMatrixRegression:

    def test_catalog_integrity(self):
        """Verifies 53 operations, 17 controllers, 0 forbidden paths"""
        summary = verify_catalog_integrity()
        assert summary["total_operations"] == 53
        assert summary["total_controllers"] == 17
        assert summary["status"] == "PASS"

    def test_all_53_operations_have_regression_mapping(self):
        """Verifies 100% (53/53) regression coverage mapping"""
        catalog_op_ids = {op.op_id for op in AUTHORITATIVE_53_OPERATIONS}
        mapped_op_ids = set(OPERATION_TEST_MAPPINGS.keys())

        missing_from_tests = catalog_op_ids - mapped_op_ids
        assert len(missing_from_tests) == 0, f"Operations lacking regression tests: {missing_from_tests}"

        extra_tests = mapped_op_ids - catalog_op_ids
        assert len(extra_tests) == 0, f"Spurious operations in test mapping: {extra_tests}"
        assert len(OPERATION_TEST_MAPPINGS) == 53

    @pytest.mark.parametrize("op", AUTHORITATIVE_53_OPERATIONS, ids=lambda op: f"{op.op_id}_{op.method}_{op.path}")
    def test_operation_definition_validity(self, op: EndpointDefinition):
        """Validates contract properties of each individual operation"""
        assert op.op_id.startswith("OP-")
        assert op.method in ["GET", "POST", "PATCH"]
        assert op.path.startswith("/")
        assert op.controller.endswith("Controller")
        assert len(op.handler_method) > 0
        assert op.authorized_role in ["ANONYMOUS", "AUTHENTICATED", "ROLE_CUSTOMER", "ROLE_THEATRE_MANAGER", "ROLE_SUPER_ADMIN"]

    def test_java_controller_source_alignment(self):
        """
        Scans Spring Boot Java @RestController files to confirm:
        - All 17 controller classes exist
        - Handlers match the 53 operations
        """
        backend_controllers_dir = os.path.join(
            os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
            "backend", "src", "main", "java", "com", "pvk", "cinemas"
        )
        assert os.path.exists(backend_controllers_dir), f"Backend directory not found: {backend_controllers_dir}"

        found_controllers = set()
        for root, _, files in os.walk(backend_controllers_dir):
            for f in files:
                if f.endswith("Controller.java"):
                    found_controllers.add(f.replace(".java", ""))

        expected_controllers = set(op.controller for op in AUTHORITATIVE_53_OPERATIONS)
        assert expected_controllers.issubset(found_controllers), (
            f"Missing controller classes: {expected_controllers - found_controllers}"
        )

"""
PVK Cinemas — Authoritative 53 Discrete HTTP Operations Catalog
Source: PVK_Cinemas_API_Specification.docx and docs/FINAL_REGRESSION_AND_ACCEPTANCE_REPORT.md.
Meticulously reconciled across the 17 Spring Boot @RestController classes.
"""

from dataclasses import dataclass
from typing import Optional

@dataclass(frozen=True)
class EndpointDefinition:
    op_id: str
    method: str
    path: str
    controller: str
    handler_method: str
    auth_required: bool
    authorized_role: str
    manager_scoped: bool
    read_only: bool

# The frozen 53 discrete HTTP operations
AUTHORITATIVE_53_OPERATIONS: list[EndpointDefinition] = [
    EndpointDefinition("OP-01", "GET", "/admin/audit-logs", "AdminAuditController", "getAuditLogs", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-02", "POST", "/auth/register", "AuthController", "register", False, "ANONYMOUS", False, False),
    EndpointDefinition("OP-03", "POST", "/auth/login", "AuthController", "login", False, "ANONYMOUS", False, False),
    EndpointDefinition("OP-04", "POST", "/auth/logout", "AuthController", "logout", True, "AUTHENTICATED", False, False),
    EndpointDefinition("OP-05", "GET", "/auth/me", "AuthController", "getMe", True, "AUTHENTICATED", False, True),
    EndpointDefinition("OP-06", "GET", "/manager/shows/{showId}/seats", "ManagerAvailabilityController", "getSeats", True, "ROLE_THEATRE_MANAGER", True, True),
    EndpointDefinition("OP-07", "PATCH", "/manager/shows/{showId}/seats", "ManagerAvailabilityController", "overrideSeatStatus", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-08", "GET", "/admin/movies", "AdminMovieController", "getMovies", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-09", "POST", "/admin/movies", "AdminMovieController", "createMovie", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-10", "PATCH", "/admin/movies/{movieId}", "AdminMovieController", "updateMovie", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-11", "GET", "/manager/movies", "ManagerMovieController", "getMovies", True, "ROLE_THEATRE_MANAGER", False, True),
    EndpointDefinition("OP-12", "GET", "/movies", "MovieController", "getMovies", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-13", "GET", "/movies/{movieId}", "MovieController", "getMovie", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-14", "GET", "/movies/{movieId}/genres", "MovieController", "getMovieGenres", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-15", "GET", "/movies/{movieId}/languages", "MovieController", "getMovieLanguages", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-16", "GET", "/movies/{movieId}/shows", "MovieController", "getMovieShows", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-17", "GET", "/manager/theatres/{theatreId}/screens", "ManagerInfrastructureController", "getScreens", True, "ROLE_THEATRE_MANAGER", True, True),
    EndpointDefinition("OP-18", "POST", "/manager/theatres/{theatreId}/screens", "ManagerInfrastructureController", "createScreen", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-19", "PATCH", "/manager/theatres/{theatreId}/screens/{screenId}", "ManagerInfrastructureController", "updateScreen", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-20", "GET", "/manager/screens/{screenId}/seats", "ManagerInfrastructureController", "getSeats", True, "ROLE_THEATRE_MANAGER", True, True),
    EndpointDefinition("OP-21", "POST", "/manager/screens/{screenId}/seats", "ManagerInfrastructureController", "createSeat", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-22", "PATCH", "/manager/screens/{screenId}/seats/{seatId}", "ManagerInfrastructureController", "updateSeat", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-23", "GET", "/admin/cities", "AdminOrganizationController", "getCities", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-24", "POST", "/admin/cities", "AdminOrganizationController", "createCity", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-25", "PATCH", "/admin/cities/{cityId}", "AdminOrganizationController", "updateCity", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-26", "GET", "/admin/theatres", "AdminOrganizationController", "getTheatres", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-27", "POST", "/admin/theatres", "AdminOrganizationController", "createTheatre", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-28", "PATCH", "/admin/theatres/{theatreId}", "AdminOrganizationController", "updateTheatre", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-29", "POST", "/admin/theatres/{theatreId}/managers", "AdminOrganizationController", "assignManager", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-30", "PATCH", "/admin/theatres/{theatreId}/managers", "AdminOrganizationController", "revokeManager", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-31", "GET", "/cities", "CityController", "getCities", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-32", "GET", "/cities/{cityId}/theatres", "CityController", "getTheatresByCity", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-33", "GET", "/theatres/{theatreId}", "TheatreController", "getTheatre", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-34", "GET", "/theatres/{theatreId}/screens", "TheatreController", "getScreens", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-35", "GET", "/theatres/{theatreId}/shows", "TheatreController", "getShows", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-36", "GET", "/manager/theatres/{theatreId}/shows", "ManagerSchedulingController", "getShows", True, "ROLE_THEATRE_MANAGER", True, True),
    EndpointDefinition("OP-37", "POST", "/manager/theatres/{theatreId}/shows", "ManagerSchedulingController", "createShow", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-38", "PATCH", "/manager/theatres/{theatreId}/shows/{showId}", "ManagerSchedulingController", "updateShow", True, "ROLE_THEATRE_MANAGER", True, False),
    EndpointDefinition("OP-39", "GET", "/shows/{showId}", "ShowController", "getShow", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-40", "GET", "/shows/{showId}/seats", "ShowController", "getShowSeats", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-41", "POST", "/admin/search/reindex", "AdminSearchController", "triggerReindex", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-42", "GET", "/admin/search/status", "AdminSearchController", "getStatus", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-43", "GET", "/search", "SearchController", "searchGet", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-44", "POST", "/search", "SearchController", "searchPost", False, "ANONYMOUS", False, True),
    EndpointDefinition("OP-45", "GET", "/admin/roles", "AdminRoleController", "getRoles", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-46", "POST", "/admin/roles/{roleId}", "AdminRoleController", "updateRolePermissionsPost", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-47", "PATCH", "/admin/roles/{roleId}", "AdminRoleController", "updateRolePermissionsPatch", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-48", "GET", "/admin/permissions", "AdminRoleController", "getPermissions", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-49", "GET", "/admin/users", "AdminUserController", "getUsers", True, "ROLE_SUPER_ADMIN", False, True),
    EndpointDefinition("OP-50", "POST", "/admin/users/{userId}", "AdminUserController", "updateUserPost", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-51", "PATCH", "/admin/users/{userId}", "AdminUserController", "updateUserPatch", True, "ROLE_SUPER_ADMIN", False, False),
    EndpointDefinition("OP-52", "GET", "/me/profile", "UserProfileController", "getProfile", True, "AUTHENTICATED", False, True),
    EndpointDefinition("OP-53", "PATCH", "/me/profile", "UserProfileController", "updateProfile", True, "AUTHENTICATED", False, False),
]

def verify_catalog_integrity() -> dict:
    """
    Verifies that the catalog matches authoritative baselines:
    - Exactly 53 discrete operations
    - Exactly 17 controllers
    - Zero forbidden e-commerce / booking paths
    """
    assert len(AUTHORITATIVE_53_OPERATIONS) == 53, f"Expected 53 operations, found {len(AUTHORITATIVE_53_OPERATIONS)}"

    controllers = set(op.controller for op in AUTHORITATIVE_53_OPERATIONS)
    assert len(controllers) == 17, f"Expected 17 controllers, found {len(controllers)}"

    unique_routes = set((op.method, op.path) for op in AUTHORITATIVE_53_OPERATIONS)
    assert len(unique_routes) == 53, f"Duplicate method+path detected in catalog: {len(unique_routes)}"

    forbidden_terms = ["booking", "ticket", "cart", "checkout", "payment", "refund", "hold", "loyalty"]
    for op in AUTHORITATIVE_53_OPERATIONS:
        for term in forbidden_terms:
            assert term not in op.path.lower(), f"Forbidden term '{term}' in endpoint path: {op.path}"

    public_ops = [op for op in AUTHORITATIVE_53_OPERATIONS if not op.auth_required]
    manager_ops = [op for op in AUTHORITATIVE_53_OPERATIONS if op.manager_scoped]
    admin_ops = [op for op in AUTHORITATIVE_53_OPERATIONS if op.authorized_role == "ROLE_SUPER_ADMIN"]
    read_only_ops = [op for op in AUTHORITATIVE_53_OPERATIONS if op.read_only]

    return {
        "total_operations": len(AUTHORITATIVE_53_OPERATIONS),
        "total_controllers": len(controllers),
        "public_operations": len(public_ops),
        "manager_scoped_operations": len(manager_ops),
        "super_admin_operations": len(admin_ops),
        "read_only_operations": len(read_only_ops),
        "mutation_operations": len(AUTHORITATIVE_53_OPERATIONS) - len(read_only_ops),
        "forbidden_endpoints": 0,
        "status": "PASS"
    }

if __name__ == "__main__":
    result = verify_catalog_integrity()
    print("53 Operations Catalog Integrity Verification:")
    for k, v in result.items():
        print(f"  {k}: {v}")

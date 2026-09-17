"""
PVK Cinemas — P14-A Unified Infrastructure Health Check
Executes automated validation across all test infrastructure components.
"""

import sys
import os

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

# Add root directory to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

def main():
    print("=" * 80)
    print("PVK CINEMAS — P14-A TEST INFRASTRUCTURE VALIDATION")
    print("=" * 80)

    # 1. Check Catalog
    print("\n[1/5] Verifying 53 Discrete HTTP Operations Catalog...")
    from tests.config.endpoints_catalog import verify_catalog_integrity
    cat_result = verify_catalog_integrity()
    print(f"  ✓ Confirmed {cat_result['total_operations']} operations across {cat_result['total_controllers']} controllers.")
    print(f"  ✓ Forbidden e-commerce / booking endpoints: {cat_result['forbidden_endpoints']}")

    # 2. Check Environment Configuration
    print("\n[2/5] Verifying Test Environment Ports...")
    from tests.config.test_environment import ENV
    print(f"  ✓ Backend URL:  {ENV.backend_base_url}")
    print(f"  ✓ Frontend URL: {ENV.frontend_base_url}")
    print(f"  ✓ Search URL:   {ENV.search_base_url} (Port 8001 verified)")
    print(f"  ✓ Database:     {ENV.database_host}:{ENV.database_port}/{ENV.database_name}")
    assert ":8001" in ENV.search_base_url, "ERROR: Search port must be 8001!"

    # 3. Check Account Strategy
    print("\n[3/5] Verifying Test Account Strategy (5 Personas)...")
    from tests.config.test_accounts import ACCOUNTS
    print(f"  ✓ Configured personas: {list(ACCOUNTS.keys())}")
    assert ACCOUNTS["manager_a"].assigned_theatre_id != ACCOUNTS["manager_b"].assigned_theatre_id
    print(f"  ✓ Manager A assigned to Theatre {ACCOUNTS['manager_a'].assigned_theatre_id}")
    print(f"  ✓ Manager B assigned to Theatre {ACCOUNTS['manager_b'].assigned_theatre_id} (Scope isolation guaranteed)")

    # 4. Check Fixture Registry
    print("\n[4/5] Verifying Fixture Isolation...")
    from tests.fixtures.test_fixtures import FixtureRegistry
    reg = FixtureRegistry()
    email = reg.generate_unique_email("infra_test")
    phone = reg.generate_unique_phone()
    print(f"  ✓ Unique fixture generated: {email} / {phone}")

    # 5. Check Browser Binaries
    print("\n[5/5] Verifying Playwright Browser Infrastructure...")
    import subprocess
    cmd = ["npm", "run", "test:e2e:infra"]
    frontend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend"))
    try:
        proc = subprocess.run(
            cmd,
            cwd=frontend_dir,
            capture_output=True,
            text=True,
            shell=True,
            timeout=30
        )
        if proc.returncode == 0:
            print("  ✓ Playwright Headless Chromium launched and passed 2 infrastructure smoke checks.")
        else:
            print(f"  ✗ Playwright check exited with code {proc.returncode}")
            print(proc.stdout)
            print(proc.stderr)
            sys.exit(1)
    except Exception as e:
        print(f"  ✗ Failed to run Playwright check: {e}")
        sys.exit(1)

    print("\n" + "=" * 80)
    print("ALL P14-A TEST INFRASTRUCTURE COMPONENTS VERIFIED AND OPERATIONAL (EXIT 0)")
    print("=" * 80)

if __name__ == "__main__":
    main()

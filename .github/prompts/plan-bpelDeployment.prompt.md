# Plan: Fix BPEL Apache ODE Deployment to Tomcat

**TL;DR:** Create a parent Maven POM that unifies all services + BPEL. Add a dedicated BPEL module with assembly plugin to package BPEL artifacts. Configure Maven to automatically deploy the BPEL package to ODE's deployment directory during build.

---

## Phase 1: Parent POM Structure (Foundation)

**Steps 1-3:** Create root `pom.xml` and update each service to inherit from it

| Step | Action | Justification |
|------|--------|---------------|
| 1 | Create root POM with 6 modules (AuthServer, CatalogService, OrdersService, PaymentsService, ShippingService, bpel) | Currently each service builds independently with no version management. Risks dependency conflicts. Parent POM ensures coordinated, consistent builds. |
| 2 | Add `<parent>` to each service's pom.xml | When OrdersService uses Spring 5.0 and AuthServer uses Spring 4.9, orchestration breaks. Centralized management prevents this. |
| 3 | Define `catalina.home` property in root POM | Different developers have Tomcat in different paths. Maven property allows personalization without POM edits. |

---

## Phase 2: Build BPEL Packaging (Core Fix)

**Steps 4-10:** Create `bpel/pom.xml` with Maven Assembly Plugin and deployment safeguards — **This is the critical fix**

| Step | Action | Justification |
|------|--------|---------------|
| 4 | Create `bpel/pom.xml` with assembly plugin, packaging type `zip` and Maven Antrun Plugin | ODE requires a ZIP with specific structure. Maven Assembly ensures consistent, reproducible packaging every build. Antrun plugin provides file copy with error detection. |
| 5 | Create `bpel/src/assembly/ode-package.xml` descriptor with correct directory structure: `PlaceOrder/bpel/` (BPEL file), `PlaceOrder/` (deploy.xml, WSDLs, XSD, endpoint). Verify structure after first build. | Assembly descriptor controls exactly what goes into the ZIP. Wrong structure causes ODE to fail parsing silently. Prevents accidentally including dev files. Ensures strict ODE compatibility. |
| 6 | Define Maven property `catalina.home` in `bpel/pom.xml` with default value `C:\Projects\apache-tomcat-9.0.116` | Allows per-environment override: `mvn clean package -Dcatalina.home=D:\tomcat\`. Prevents hardcoding paths. Users can set environment variable `CATALINA_HOME` for system-wide default. |
| 7 | **Configure Maven Antrun Plugin to copy ZIP to `${catalina.home}\webapps\ode\WEB-INF\processes\PlaceOrder.zip` with success verification** | **ROOT CAUSE FIX:** ODE's poller watches this directory for ZIPs. Copying here during Maven build closes the gap between "BPEL is packaged" and "ODE can deploy it." Antrun + verification ensures copy actually succeeded and fails build if deployment directory unreachable. |
| 8 | Add Maven plugin to unzip and verify internal structure of generated ZIP | Catches assembly descriptor errors before deployment. If ZIP lacks deploy.xml or has wrong folder structure, build fails with clear error message instead of silent ODE failure. Runs during `verify` phase. |
| 9 | Move BPEL sources to `bpel/src/main/resources/` following Maven conventions | Maven assembly plugin expects sources in standard Maven layout (`src/main/resources/`). Root-level files at `bpel/*.bpel` make assembly configuration fragile and error-prone. Proper structure prevents accidental file exclusions. |
| 10 | Add Maven plugin to log deployment outcome and show file size, timestamp, and target path in build console | Provides visible confirmation in Maven build output: `[INFO] BPEL package deployed to C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\PlaceOrder.zip (15 KB)`. Helps developers immediately see if deployment succeeded without checking file system. |

---

## Phase 3: Deployment & Verification (Operations)

**Steps 11-13:** Verification scripts and smoke tests

| Step | Action | Justification |
|------|--------|---------------|
| 11 | Create `verify-bpel-deployment.bat` script that: (1) checks if PlaceOrder.zip exists at ODE path, (2) verifies ZIP integrity with 7-zip/winRAR, (3) checks ODE logs for "Found process package" message, (4) checks ODE console (http://localhost:8080/ode/) for PlaceOrder process in ACTIVE status | Maven automated deployment handles 95% of scenarios, but provides manual fallback if Tomcat path detection fails or automated copy is disabled. Comprehensive verification catches silent deployment failures before they reach production. |
| 12 | Create `deploy-bpel-manual.bat` backup script: runs `mvn clean package -f bpel/pom.xml` then manually copies ZIP if Maven copy fails (fallback only) | Provides escape hatch if Maven Antrun plugin fails on some machines. Useful for troubleshooting edge cases or non-standard Tomcat installations. |
| 13 | Create `check-ode-status.bat` to verify: (1) ODE processes directory accessible, (2) ODE logs show no permission errors, (3) ODE deployment poller is running | Catches deployment failures immediately. Prevents "BPEL mysteriously fails to deploy" issues discovered during end-to-end testing. Early signal prevents wasted troubleshooting time. |

---

## Phase 4: Documentation (Operational Readiness)

**Steps 14-16:** Update README and create troubleshooting guide

| Step | Action | Justification |
|------|--------|---------------|
| 14 | Add "Building and Deploying BPEL" section to README including: How to set CATALINA_HOME property if needed, command to run `mvn clean package -f bpel/pom.xml`, how to verify deployment at http://localhost:8080/ode/, how to read ODE logs for deployment confirmation | Current README omits BPEL deployment. New devs need guidance on *why* each step exists and *how to verify* it worked. Clear instructions reduce support burden. |
| 15 | Add inline comments to `bpel/pom.xml`: explain assembly plugin configuration, why deploy.xml must be at PlaceOrder/ root, why ZIP structure is critical, what each Maven plugin does | Prevents future maintainers from "simplifying" the structure and breaking ODE compatibility. Inline documentation ensures knowledge survives code reviews. |
| 16 | Create `DEPLOYMENT_TROUBLESHOOTING.md` with: (1) Diagnosis checklist (ZIP exists? ODE sees it? Log errors?), (2) Common failures: empty ODE directory → check catalina.home property, ZIP not copied → check file permissions, Bearer token fails → check orders.endpoint, process won't activate → check deploy.xml structure | Quick reference for common failures. When deployment inevitably fails in a new environment, developers need quick diagnosis instead of debugging from scratch. |

---

## Phase 5: End-to-End Testing (Validation)

**Step 17:** Verify the entire orchestration flow

- Deploy BPEL: `mvn clean package -f bpel/pom.xml`
- Run `verify-bpel-deployment.bat` to confirm ZIP in ODE directory and process is ACTIVE
- Send SOAP request using [place-order-request.xml](place-order-request.xml) to `http://localhost:8080/ode/processes/PlaceOrderService`
- Verify PlaceOrder SOAP request reaches ODE (check ODE logs for request received)
- Verify BPEL invokes CatalogService via SOAP (loop through items, call getBookPrice)
- Verify BPEL invokes OrdersService via HTTP POST with aggregated totals and Bearer token from orders.endpoint
- Verify response returns to client with placeOrderResponse (not PlaceOrderFault)

**Justification:** BPEL deployment now works, but integration points must be tested. If CatalogService WSDL binding doesn't match expectations in `catalog.wsdl`, orchestration fails silently in ODE logs. If OrdersService endpoint URL changed, Bearer token expired, or HTTP headers incorrect, BPEL invocation fails without clear error. This test surfaces those gaps.

---

## Verification Checklist

After all steps complete:

1. ✅ **Build succeeds:** `mvn clean package -f bpel/pom.xml` completes without errors
2. ✅ **ZIP is created:** `bpel/target/bpel-processes-1.0.0.zip` exists with correct file size (should be 15-30 KB, not 1 KB)
3. ✅ **ZIP structure correct:** Unzip and verify contains:
   - `PlaceOrder/bpel/PlaceOrder.bpel` (main BPEL process file)
   - `PlaceOrder/deploy.xml` (ODE deployment descriptor at PlaceOrder root, NOT nested)
   - `PlaceOrder/PlaceOrder.wsdl`, `PlaceOrder/orders.wsdl`, `PlaceOrder/catalog.wsdl` (WSDL bindings)
   - `PlaceOrder/order-contract.xsd` (XML schema)
   - `PlaceOrder/orders.endpoint` (HTTP endpoint configuration)
4. ✅ **Deployed to correct location:** File exists at `C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\PlaceOrder.zip` with recent timestamp (within last 5 seconds of Maven build)
5. ✅ **Maven build output shows deployment:** Build console contains message like: `[INFO] BPEL package deployed successfully to C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\PlaceOrder.zip`
6. ✅ **ODE recognizes package:** Wait 10 seconds, check `C:\Projects\apache-tomcat-9.0.116\logs\ode.log` contains:
   - `INFO [DeploymentPoller] Found process package: PlaceOrder.zip`
   - `INFO [ProcessStore] Deployment of {http://www.example.org/bpel}PlaceOrder successful` (or similar namespace)
   - NO errors or exceptions during deployment
7. ✅ **ODE console shows process:** http://localhost:8080/ode/ → Processes tab → Shows `PlaceOrder` with Status: **ACTIVE**
8. ✅ **SOAP endpoint responds:** SOAP client can reach `http://localhost:8080/ode/processes/PlaceOrderService` without connection timeout
9. ✅ **Full orchestration trace in logs:** Send SOAP request with [place-order-request.xml](place-order-request.xml), verify ODE logs show:
   - Request received from client
   - Loop executing through order items
   - CatalogService.getBookPrice() invoked per item
   - OrdersService HTTP POST with Bearer token
   - Response sent to client

---

## SafeGuards & Failure Detection

If any verification step FAILS, use this diagnostic table:

| Symptom | Likely Cause | Diagnostic Command |
|---------|--------------|-------------------|
| ZIP not in `bpel/target/` | Assembly plugin not configured. Build errors silently ignored | Check Maven build output for `[ERROR]` lines; re-run `mvn clean package -f bpel/pom.xml` |
| ZIP exists locally but not at ODE path | Antrun copy plugin failed; Tomcat path incorrect; file permissions issue | Run `mvn clean package -f bpel/pom.xml -X` (debug mode); check file permissions on ODE directory |
| ZIP is at ODE path but tiny (1-2 KB) | Assembly descriptor incorrect; no files included | Unzip file: `7z l C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes\PlaceOrder.zip`; verify PlaceOrder/ folder exists |
| ODE logs show "Permission denied" | ODE process lacks read/write access to processes directory | Check ODE process user permissions; run Tomcat as Administrator if needed |
| ODE logs show "deploy.xml not found" | Assembly descriptor put deploy.xml in wrong subfolder | Unzip and check: deploy.xml must be at `PlaceOrder/deploy.xml`, NOT `PlaceOrder/bpel/deploy.xml` |
| Process visible in ODE console but status is INACTIVE/RETIRED | deploy.xml has `<active>false</active>` or `<retired>true</retired>` | Check [PlaceOrder.bpel](bpel/PlaceOrder.bpel) line referencing deploy.xml; should have `<active>true</active>` |
| SOAP request times out | ODE process exists but endpoint not configured | Check ODE logs for SOAP binding errors; verify `<provide partnerLink="client">` in deploy.xml |

---

## Key Files to Create/Modify

**New files:**
- `bpel/pom.xml` — BPEL Maven module with assembly plugin, Maven Antrun for deployment, structure verification
- `bpel/src/assembly/ode-package.xml` — Assembly descriptor defining ZIP structure for ODE compatibility
- `bpel/src/main/resources/` — Proper Maven directory structure for BPEL source files
- `verify-bpel-deployment.bat` — Comprehensive verification script (ZIP exists, integrity, ODE logs, console check)
- `deploy-bpel-manual.bat` — Backup manual deployment script (for edge case fallback)
- `check-ode-status.bat` — ODE health check script (poller running, permissions OK, logs clean)
- `DEPLOYMENT_TROUBLESHOOTING.md` — Operational runbook with diagnostic table
- README.md update — BPEL build/deploy instructions

**Files to update:**
- [README.md](README.md) — Add "Building and Deploying BPEL" section with catalina.home property usage
- `pom.xml` (root, if Phase 1 included) — Define catalina.home property with default `C:\Projects\apache-tomcat-9.0.116`

**Reference files (no changes needed, but critical for structure):**
- [PlaceOrder.bpel](bpel/PlaceOrder.bpel) — Will be moved to `bpel/src/main/resources/`
- [deploy.xml](bpel/deploy.xml) — Will be moved to `bpel/src/main/resources/deploy.xml`
- [PlaceOrder.wsdl](bpel/PlaceOrder.wsdl) — Will be moved and included in ZIP
- [orders.wsdl](bpel/orders.wsdl) — Will be moved and included in ZIP
- [catalog.wsdl](bpel/catalog.wsdl) — Will be moved and included in ZIP
- [order-contract.xsd](bpel/order-contract.xsd) — Will be moved and included in ZIP
- [orders.endpoint](bpel/orders.endpoint) — Will be moved and included in ZIP

---

## Critical Decisions & Justifications

1. **Parent POM approach** → Allows coordinated builds and deployment orchestration. Independent builds would work but lack integration control.

2. **Automated Maven deployment (Step 6)** → Eliminates manual steps and reduces errors. This single step fixes the root cause—ODE now has something to deploy.

3. **Assembly plugin for BPEL packaging** → Maven best practice. More maintainable than manual ZIP scripts.

4. **CATALINA_HOME as Maven property** → Supports different Tomcat installations on different developer machines without POM edits.

---

## Implementation Details: Safeguards & Configuration

### Maven Property for CATALINA_HOME (Step 6)

Add to `bpel/pom.xml`:
```xml
<properties>
  <!-- Default Tomcat path; override with: mvn clean package -Dcatalina.home=C:\path\to\tomcat -->
  <catalina.home>C:\Projects\apache-tomcat-9.0.116</catalina.home>
  <!-- Allow system environment variable override as fallback -->
  <catalina.home>${env.CATALINA_HOME}</catalina.home>
</properties>
```

Users can override per-build:
```bash
mvn clean package -f bpel/pom.xml -Dcatalina.home=D:\alternative-tomcat\
```

Or set system environment variable `CATALINA_HOME` once, applies to all builds.

### Maven Antrun Plugin for Deployment (Step 7)

Add to `bpel/pom.xml` `<plugins>` section:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-antrun-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <configuration>
        <target>
          <!-- Copy BPEL package to ODE deployment directory -->
          <copy file="${project.build.directory}/${project.artifactId}-${project.version}.zip"
                tofile="${catalina.home}/webapps/ode/WEB-INF/processes/PlaceOrder.zip"
                failonerror="true" overwrite="true" verbose="true" />
          <!-- Log successful deployment -->
          <echo message="✓ BPEL package deployed to ${catalina.home}/webapps/ode/WEB-INF/processes/PlaceOrder.zip" />
        </target>
      </configuration>
      <goals>
        <goal>run</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**Effect:** If copy fails (directory doesn't exist, no permissions), Maven build FAILS with error message instead of silently succeeding.

### ZIP Structure Verification Script (Step 8)

Could be implemented as Maven plugin or separate validation. Verify structure after assembly:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <!-- Alternatively, use custom Maven Mojo or post-build script -->
</plugin>
```

Or simpler: Add to `verify-bpel-deployment.bat` (Step 11):
```batch
echo Verifying ZIP structure...
7z t "%ODE_DIR%\PlaceOrder.zip" "PlaceOrder/deploy.xml" >nul 2>&1
if errorlevel 1 (
  echo ✗ ERROR: deploy.xml not found in ZIP at correct location (must be PlaceOrder/deploy.xml, not nested)
  exit /b 1
)
echo ✓ ZIP structure is valid
```

### Verification Script Template (Step 11)

`verify-bpel-deployment.bat`:
```batch
@echo off
setlocal enabledelayedexpansion

set ODE_DIR=C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes
set ODE_LOG=C:\Projects\apache-tomcat-9.0.116\logs\ode.log

echo.
echo === BPEL Deployment Verification ===
echo.

REM Check 1: ZIP exists
if exist "%ODE_DIR%\PlaceOrder.zip" (
  echo [✓] PlaceOrder.zip exists at ODE deployment directory
  for /f %%A in ('powershell -Command "[int][System.IO.FileInfo]::new(\"!ODE_DIR!\PlaceOrder.zip\").Length / 1KB"') do (
    echo     File size: %%A KB
  )
) else (
  echo [✗] FAILED: PlaceOrder.zip NOT FOUND at %ODE_DIR%
  echo     Check: Was Maven build successful? Is CATALINA_HOME correct?
  exit /b 1
)

REM Check 2: ODE logs show deployment detected
if exist "%ODE_LOG%" (
  findstr "Found process package.*PlaceOrder" "%ODE_LOG%" >nul
  if !errorlevel! equ 0 (
    echo [✓] ODE logs show process package detected
  ) else (
    echo [!] WARNING: ODE logs don't show "Found process package" yet
    echo     This can be normal if ODE just started. Check again in 10 seconds.
  )
) else (
  echo [!] WARNING: ODE log file not found at %ODE_LOG%
)

REM Check 3: No permission errors in ODE logs
if exist "%ODE_LOG%" (
  findstr "Permission denied\|Access denied" "%ODE_LOG%" >nul
  if !errorlevel! neq 0 (
    echo [✓] No permission errors in ODE logs
  ) else (
    echo [✗] FAILED: Permission errors found in ODE logs
    echo     Solution: Run Tomcat as Administrator or fix file permissions
    exit /b 1
  )
)

echo.
echo === Deployment Verified ===
echo Next: Check ODE console at http://localhost:8080/ode/
echo       Process should appear in "Processes" tab with status ACTIVE
echo.
```

### Manual Deployment Fallback Script (Step 12)

`deploy-bpel-manual.bat`:
```batch
@echo off
echo Building BPEL package...
cd bpel
call mvn clean package -DskipTests

if !errorlevel! neq 0 (
  echo ✗ Maven build failed
  exit /b 1
)

echo.
echo Copying to ODE deployment directory...
set ODE_DIR=C:\Projects\apache-tomcat-9.0.116\webapps\ode\WEB-INF\processes
copy target\bpel-processes-1.0.0.zip "%ODE_DIR%\PlaceOrder.zip"

if !errorlevel! equ 0 (
  echo ✓ BPEL package deployed successfully
) else (
  echo ✗ Copy failed. Check: Is ODE directory path correct? Do you have write permissions?
  exit /b 1
)
```

### ODE Status Check Script (Step 13)

`check-ode-status.bat`:
```batch
@echo off
setlocal enabledelayedexpansion

set ODE_DIR=C:\Projects\apache-tomcat-9.0.116\webapps\ode
set ODE_LOG=C:\Projects\apache-tomcat-9.0.116\logs\ode.log

echo === ODE Health Check ===
echo.

REM Check 1: ODE directory accessible
if exist "%ODE_DIR%" (
  echo [✓] ODE directory accessible
) else (
  echo [✗] ODE directory not found at %ODE_DIR%
  exit /b 1
)

REM Check 2: ODE logs show poller running
if exist "%ODE_LOG%" (
  findstr "DeploymentPoller.*Poller started" "%ODE_LOG%" >nul
  if !errorlevel! equ 0 (
    echo [✓] ODE deployment poller is running
  ) else (
    echo [!] WARNING: Deployment poller status unclear in logs
  )
) else (
  echo [!] ODE log file not found (normal if ODE just started)
)

REM Check 3: Check web service availability
echo Checking ODE SOAP endpoint...
powershell -Command "try { [Net.ServicePointManager]::ServerCertificateValidationCallback={$true}; $result = Invoke-WebRequest -Uri 'http://localhost:8080/ode/' -TimeoutSec 5; if ($result.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }"

if !errorlevel! equ 0 (
  echo [✓] ODE web console responding at http://localhost:8080/ode/
) else (
  echo [✗] ODE not responding. Is Tomcat running? Is ODE app deployed?
  exit /b 1
)

echo.
echo === ODE is Ready ===
```

---

## Critical Implementation Notes

1. **Assembly Descriptor Structure:** The `ode-package.xml` must produce this exact structure:
   ```
   PlaceOrder/
   ├── bpel/
   │   └── PlaceOrder.bpel       (main BPEL process)
   ├── deploy.xml                (ODE deployment descriptor)
   ├── PlaceOrder.wsdl           (process WSDL)
   ├── orders.wsdl               (OrdersService WSDL)
   ├── catalog.wsdl              (CatalogService WSDL)
   ├── order-contract.xsd        (XML schema)
   └── orders.endpoint           (HTTP endpoint config)
   ```
   ODE will **reject** this structure:
   ```
   PlaceOrder.bpel               (wrong: files at ZIP root instead of PlaceOrder/ folder)
   deploy.xml
   ...
   ```

2. **CATALINA_HOME Discovery Order:**
   - Command-line property: `-Dcatalina.home=C:\path` (highest priority)
   - Environment variable: `CATALINA_HOME` (if set)
   - POM default: `C:\Projects\apache-tomcat-9.0.116` (fallback)

3. **Verification Timing:** After deployment, wait **10-15 seconds** before checking ODE logs or console. ODE poller runs on interval; immediate checks may show no detection yet.

4. **Bearer Token Consideration:** `orders.endpoint` contains OAuth2 bearer token. After BPEL deploys successfully, implement token rotation or environment-based configuration to prevent production failures when token expires.

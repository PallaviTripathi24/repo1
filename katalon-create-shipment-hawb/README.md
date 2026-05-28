# Create Shipment HAWB Katalon Automation

This folder is an importable Katalon Studio project for the Boomi middleware API:

`Create Shipment Canonical - HAWBRange Service GetHAWBNumber Method`

It automates the Create Shipment HAWB API scenarios for positive, negative, edge, header/gateway, request structure, Boomi mapping/integration, and non-functional coverage.

## Project contents

| Path | Purpose |
|---|---|
| `CreateShipmentHAWB.prj` | Katalon project file. Open this file from Katalon Studio. |
| `Profiles/default.glbl` | Environment variables for URLs, API keys, source header, and response SLA. |
| `Keywords/api/CreateShipmentHawbKeywords.groovy` | Reusable custom keyword that builds requests, sends API calls, and asserts responses. |
| `Include/resources/testdata/create_shipment_hawb_scenarios.json` | Data-driven scenario file with test case IDs `CS-HAWB-001` through `CS-HAWB-062`. |
| `Test Cases/Create Shipment HAWB/Run All Create Shipment HAWB API Tests.tc` | Executable Katalon test case. |
| `Scripts/Create Shipment HAWB/Run All Create Shipment HAWB API Tests/Script1716800000000.groovy` | Script that runs the scenario file. |
| `Test Suites/Create Shipment HAWB API Suite.ts` | Katalon test suite to execute all automated scenarios. |
| `Docs/test-case-coverage.md` | Jira-friendly coverage summary and manual verification notes. |

## Required runtime variables

Update these values in `Profiles/default.glbl` or override them using your secured Katalon/CI execution profile.

Do not commit real API keys.

| Variable | Example / Placeholder | Description |
|---|---|---|
| `BASE_URL` | `https://aramex-test-gw.boomi.cloud:443` | Main Boomi Gateway base URL. |
| `CREATE_SHIPMENT_HAWB_PATH` | `/ws/rest/api/v1/shipmentCreate/HAWBNumberMethod` | Main API resource path. |
| `API_KEY` | `{{API_KEY}}` | Main API key. |
| `BOOMI_GATEWAY_URL` | `https://ws.staging.aramex.net/Wcfgateway/api/Gateway/Execute` | Direct Boomi Gateway Execute endpoint. |
| `BOOMI_API_KEY` | `{{BOOMI_API_KEY}}` | Direct Boomi Gateway API key. |
| `BOOMI_SOURCE` | `aramexdotcom` | Direct Boomi Gateway source header. |
| `RESPONSE_TIME_SLA_MS` | `5000` | Response-time threshold used by non-functional scenarios. |
| `RUN_BOOMI_GATEWAY_TESTS` | `false` | When `false`, skips direct Gateway scenarios (`CS-HAWB-032`–`050`) that call `ws.staging.aramex.net`. Set `true` only on VPN/corporate network. |

## Open as a standalone Katalon project

1. Open Katalon Studio.
2. Select **File > Open Project**.
3. Select `katalon-create-shipment-hawb/CreateShipmentHAWB.prj`.
4. Open **Profiles > default** and replace placeholder values with secured local/test values.
5. Open **Test Suites > Create Shipment HAWB API Suite**.
6. Click **Run**.

## Import into an existing Katalon project

Copy these folders/files into the target Katalon project:

```text
Keywords/api/CreateShipmentHawbKeywords.groovy
Include/resources/testdata/create_shipment_hawb_scenarios.json
Test Cases/Create Shipment HAWB/Run All Create Shipment HAWB API Tests.tc
Scripts/Create Shipment HAWB/Run All Create Shipment HAWB API Tests/Script1716800000000.groovy
Test Suites/Create Shipment HAWB API Suite.ts
```

Then add the variables from `Profiles/default.glbl` to the target project's active execution profile and refresh the project in Katalon Studio.

## Execute from Katalon Runtime Engine

Use the command below after replacing local paths and secure runtime credentials.

```bash
katalonc -noSplash -runMode=console \
  -projectPath="/absolute/path/to/katalon-create-shipment-hawb/CreateShipmentHAWB.prj" \
  -retry=0 \
  -testSuitePath="Test Suites/Create Shipment HAWB API Suite" \
  -browserType="Web Service" \
  -executionProfile="default" \
  -apiKey="<KATALON_RUNTIME_API_KEY>"
```

For CI, pass service API keys through secure profile values or encrypted variables. Keep `{{API_KEY}}` and `{{BOOMI_API_KEY}}` placeholders in source control.

## Execution behavior

- Automated scenarios assert the client requirement that every API response returns HTTP `200 OK`.
- Valid scenarios assert `Success = true`.
- Invalid scenarios assert `Success = false` and validate that a meaningful error/message field is returned.
- Manual-only scenarios are logged as warnings because they require environment manipulation, deployment validation, or backend fault injection.
- If any automated scenario fails, the suite continues collecting failures and then marks the test run failed with a consolidated failure list.

## Connection timeout on `ws.staging.aramex.net`

If Katalon fails with `HttpHostConnectException: Connect to ws.staging.aramex.net:443 ... Connection timed out`, your machine cannot reach the **direct Boomi WCF Gateway** (staging). That host is usually available only on the **corporate VPN** or inside the Aramex network.

**Option A — Run main API tests only (recommended locally):**

1. Open **Profiles > default** (or your active profile).
2. Keep `RUN_BOOMI_GATEWAY_TESTS` = `false` (default).
3. Re-run **Create Shipment HAWB API Suite**.

Main API scenarios (`CS-HAWB-001`–`031`, `035`–`043`, `049`, `055`–`061`) use `BASE_URL` (`aramex-test-gw.boomi.cloud`) and will still execute. Gateway scenarios are logged as **Skipped** with a warning.

**Option B — Run direct Gateway scenarios:**

1. Connect to corporate VPN (or a network that can reach `ws.staging.aramex.net`).
2. Verify in a browser or `curl` that `https://ws.staging.aramex.net/Wcfgateway/api/Gateway/Execute` responds (not a connect timeout).
3. Set `RUN_BOOMI_GATEWAY_TESTS` = `true` and set `BOOMI_API_KEY` in the profile.
4. Re-run the suite.

## Notes before first execution

- Positive and direct Gateway scenarios require valid non-production API keys.
- Direct Gateway scenarios (`endpointType: boomiGateway`) are skipped unless `RUN_BOOMI_GATEWAY_TESTS` is `true`.
- Header-negative scenarios intentionally use missing or invalid credentials.
- If the Gateway returns `401`, `403`, `405`, or another non-200 status for invalid requests, Katalon will fail those scenarios because the stated client requirement is HTTP `200 OK` for all requests.

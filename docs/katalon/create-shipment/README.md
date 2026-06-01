# Create Shipment — Katalon Studio Automation

This folder contains a **production-style** Groovy script for the Boomi Gateway `POST .../Wcfgateway/api/Gateway/Execute` flow targeting `SaveShipmentsAsync`, plus setup instructions for Object Repository (optional), Global Variables, data-driven execution, and CI.

**Primary script:** `CreateShipment_Gateway_Execute.groovy`

**Security:** Do not commit real API keys. Use Katalon **Profiles** / CI secrets and placeholders such as `{{BOOMI_API_KEY}}` in documentation only.

---

## 1. Katalon Groovy Script

### Import into Katalon

1. Create a Test Case: `Test Cases/Create Shipment/Gateway/TC_Gateway_Execute_Script`.
2. Switch the Test Case to **Script** mode.
3. Paste the contents of `CreateShipment_Gateway_Execute.groovy` (or use **Add > Existing** if you copy the file into the Katalon project under `Keywords/` or keep as external reference and paste for CI snapshot).

The script builds the REST request **inline** with `RequestObject` so you can run it **without** pre-creating many REST objects. You may still add Object Repository objects later and refactor `sendGatewayPost` to use `findTestObject(...)`.

### Design highlights

- **Katalon WS keywords:** `WS.sendRequest`, `WS.verifyResponseStatusCode`, `WS.verifyElementPropertyValue`, `WS.verifyResponseTime` (with fallback if the keyword signature differs by version).
- **Assertions:** HTTP **200** always; JSON `Success` boolean; negative paths require a **non-empty** aggregated error string from common fields (`Message`, `ErrorMessage`, `Errors`, `ValidationErrors`, nested `Data.*`).
- **Scenarios:** Switch on `executionScenario` (see table below). Extend `buildScenarioDefinition` for the full TC matrix from your Jira suite.
- **Logging:** Endpoint, masked headers, full body (avoid logging secrets in shared logs in production CI).
- **Performance:** `MAX_RESPONSE_TIME_MS` / `GlobalVariable.maxResponseTimeMs` plus client-side elapsed check.

### Scenario keys (starter set)

| Key | Intent |
| --- | --- |
| `VALID_BASELINE` | Happy path single shipment |
| `VALID_MULTI_SHIPMENT` | Two shipments |
| `NEG_BLANK_GUID` | Blank GUID (expects message containing `guid`, adjust to real message) |
| `NEG_NULL_GUID` | Null GUID |
| `NEG_INVALID_GUID` | Non-UUID string |
| `NEG_EMPTY_SHIPMENTS` | Empty `Shipments` array |
| `NEG_INVALID_COUNTRY` | Invalid `DestCountryCode` |
| `NEG_INVALID_PRODUCT_TYPE` | Invalid `Type` |
| `NEG_MISSING_USER_ID` | Omit `UserId` inside `payload` |
| `HDR_MISSING_API_KEY` | No `X-API-KEY` header |
| `HDR_INVALID_API_KEY` | Wrong key |
| `HDR_MISSING_SOURCE` | No `source` header |
| `HDR_MISSING_CONTENT_TYPE` | No `Content-Type` |
| `HDR_WRONG_CONTENT_TYPE` | `text/plain` |
| `ROUTE_WRONG_CONTRACT` | Wrong `contract` |
| `ROUTE_WRONG_METHOD` | Wrong `method` |

---

## 2. Required Test Objects (optional OR-based design)

If you prefer Object Repository instead of inline `RequestObject`, create one parameterized REST object per row (names are suggestions).

| Object Name | Method | Endpoint (relative or full) | Notes |
| --- | --- | --- | --- |
| `OR_CreateShipment_Gateway_Execute` | POST | `${gatewayBaseUrl}/Wcfgateway/api/Gateway/Execute` | Primary gateway call |

**Additional optional objects** (if you split scenarios in Manual mode):

- `OR_CreateShipment_Gateway_MissingKey`
- `OR_CreateShipment_Gateway_InvalidContentType`

You can duplicate the primary object and vary default headers in each OR entry, or keep a **single** object and override headers in script (current approach).

---

## 3. Object Repository Configuration (OR approach)

For `OR_CreateShipment_Gateway_Execute`:

1. **HTTP Method:** POST  
2. **URL:** Use a project URL style `{{gatewayBaseUrl}}/Wcfgateway/api/Gateway/Execute` **or** full URL variable.  
3. **Headers:**

| Name | Value |
| --- | --- |
| `Content-Type` | `application/json` |
| `X-API-KEY` | `${boomiApiKey}` (Katalon variable / Profile) |
| `source` | `${defaultSource}` |

4. **Body:** Choose **Text / JSON** and paste a template body with placeholders, **or** leave body empty and set body entirely in script (recommended for dynamic maps).

**Variables required (examples):**

- `gatewayBaseUrl`
- `boomiApiKey`
- `defaultSource`

**Parameterization:** Bind OR variables to **Profiles** (DEV/QA/UAT) or to **Test Data** columns via Test Case variables of the same names.

---

## 4. Global Variables Required

Define these in **Profiles > Execution Settings > Global Variables** (names must match the script).

| Global Variable | Example / placeholder | Required |
| --- | --- | --- |
| `gatewayBaseUrl` | `https://ws.staging.example.com` | Yes |
| `boomiApiKey` | From secret store | Yes |
| `defaultSource` | `aramexdotcom` | Yes |
| `executionScenario` | `VALID_BASELINE` | No (defaults in script) |
| `maxResponseTimeMs` | `15000` | No |
| `lastGatewayResponseJson` | (empty) | No — optional chaining |

**Never** hardcode production URLs or keys in the script; always use Profiles / CI injection.

---

## 5. Data-Driven Configuration

### Option A — Test Suite data binding (recommended)

1. Create **Test Data** (`Data Files/Shipment Data/CreateShipment_Scenarios.csv` or Excel) with at least:

| executionScenario |
| --- |
| `VALID_BASELINE` |
| `NEG_BLANK_GUID` |
| `HDR_MISSING_API_KEY` |

2. Open your **Test Suite**, add the Test Case, open **Data Binding**, map column `executionScenario` to Test Case variable `executionScenario`.

3. In the Test Case **Script**, add at the top (before scenario resolution) a line to read the bound variable if your Katalon version does not auto-inject into `GlobalVariable`:

   - Preferred: set `GlobalVariable.executionScenario = executionScenario` in a **Test Listener** `@BeforeTestCase`, reading the data-bound variable from the test context.

Because Katalon data binding behavior varies by version, the script reads **`GlobalVariable.executionScenario`** by default. The simplest reliable pattern:

- **BeforeTestCase listener:** `GlobalVariable.executionScenario = testCaseContext.getVariables()['executionScenario']`

### Option B — One Test Case per scenario

Duplicate the Test Case and set `GlobalVariable.executionScenario` per copy in a **Before Test Case** hook or default Profile.

---

## 6. Execution Instructions

### Local execution

1. Set active **Profile** (DEV/QA/UAT) with correct `gatewayBaseUrl`, `boomiApiKey`, `defaultSource`.
2. Run Test Case `TC_Gateway_Execute_Script`.
3. Review **Log Viewer** for masked headers and response JSON.

### Test Suite execution

1. Create `Test Suites/Create Shipment/TS_CreateShipment_Regression`.
2. Add the Test Case with data binding (see section 5).
3. Run the suite; export reports as HTML/JUnit for CI.

### CI/CD (CLI)

Use Katalon Runtime Engine / Docker with `-runMode` console, for example:

```bash
katalonc -noSplash -runMode=console \
  -projectPath="$WORKSPACE/YourProject.prj" \
  -retry=0 \
  -testSuitePath="Test Suites/Create Shipment/TS_CreateShipment_Regression" \
  -apiKey="$KATALON_API_KEY" \
  -browserType="Web Service"
```

Adjust flags to your licensed runner. Inject Boomi secrets via **environment variables** mapped into Profiles or use `-g_globalVariableValues` if supported in your version.

### AWB / account data

The baseline shipment uses `AWB` placeholder text. Replace via Profile-specific overrides or extend the script to read `GlobalVariable.testAwb` for environments that require a real AWB.

---

## Recommended folder structure (inside `.prj`)

```text
Test Cases/
  Create Shipment/
    Gateway/
      TC_Gateway_Execute_Script.groovy
Object Repository/
  Create Shipment/
    OR_CreateShipment_Gateway_Execute.rs
Data Files/
  Shipment Data/
    CreateShipment_Scenarios.csv
Test Listeners/
  TL_CreateShipment_BeforeTestCase.groovy
Profiles/
  DEV.glbl
  QA.glbl
  UAT.glbl
```

---

## Maintenance notes

- If `WS.verifyElementPropertyValue` fails on boolean comparison, rely on the `JsonSlurper` assertions already in `assertJsonEnvelope`.
- If `HttpTextBodyContent` constructor differs, consult your KS version docs (two-arg vs three-arg); adjust `sendGatewayPost` only.
- Tighten `errorContains` for `NEG_BLANK_GUID` once the real validation message text is known from Boomi/backend.

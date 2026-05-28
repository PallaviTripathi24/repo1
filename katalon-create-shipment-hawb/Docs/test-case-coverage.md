# Create Shipment HAWB API Automation Coverage

The executable scenario source of truth is:

`Include/resources/testdata/create_shipment_hawb_scenarios.json`

Each scenario is executed by:

`Test Suites/Create Shipment HAWB API Suite`

## Endpoint under test

```text
POST {{BASE_URL}}/ws/rest/api/v1/shipmentCreate/HAWBNumberMethod
Content-Type: application/json
x-api-key: {{API_KEY}}
```

## Direct Boomi Gateway endpoint

```text
POST https://ws.staging.aramex.net/Wcfgateway/api/Gateway/Execute
X-API-KEY: {{BOOMI_API_KEY}}
source: aramexdotcom
Content-Type: application/json
```

## Mandatory mapping covered

| Main API / Canonical Field | Boomi Payload Field | Covered By |
|---|---|---|
| `ClientInfo.AccountEntity` | `Payload.Entity` | `CS-HAWB-003`, `CS-HAWB-004`, `CS-HAWB-042` |
| `Shipments[].Details.ProductGroup` | `Payload.ProductGroup` | `CS-HAWB-001`, `CS-HAWB-002`, `CS-HAWB-004`, `CS-HAWB-043` |
| Static value | `Payload.CategoryID = 3` | `CS-HAWB-044` |
| Static value | `Payload.MediaTypeID = 1` | `CS-HAWB-045` |
| Static value | `Payload.PaymentMethodID = 1` | `CS-HAWB-046` |
| Static value | `Method = GetHAWBNumber` | `CS-HAWB-047` |
| Static value | `Contract = Corp.InfoAXS.ExpAcc.Contracts.IHAWBRangeService` | `CS-HAWB-048` |

## Jira-friendly coverage table

| Test Case ID | Scenario Type | Test Case Description | Automation Status |
|---|---|---|---|
| CS-HAWB-001 | Positive | Valid ProductGroup `EXP` returns successful HAWB response | Automated |
| CS-HAWB-002 | Positive | Valid ProductGroup `DOM` returns successful HAWB response | Automated |
| CS-HAWB-003 | Positive | Valid AccountEntity is accepted | Automated |
| CS-HAWB-004 | Positive | Valid AccountEntity and ProductGroup together | Automated |
| CS-HAWB-005 | Positive | Optional Create Shipment fields do not block processing | Automated |
| CS-HAWB-006 | Positive | Multiple shipments handled when first shipment has valid details | Automated |
| CS-HAWB-007 | Negative | Invalid ProductGroup | Automated |
| CS-HAWB-008 | Negative | Empty ProductGroup | Automated |
| CS-HAWB-009 | Negative | Missing ProductGroup | Automated |
| CS-HAWB-010 | Negative | Null ProductGroup | Automated |
| CS-HAWB-011 | Negative | Invalid AccountEntity | Automated |
| CS-HAWB-012 | Negative | Empty AccountEntity | Automated |
| CS-HAWB-013 | Negative | Missing AccountEntity | Automated |
| CS-HAWB-014 | Negative | Null AccountEntity | Automated |
| CS-HAWB-015 | Negative | Invalid ProductGroup and AccountEntity | Automated |
| CS-HAWB-016 | Negative | Empty ProductGroup and AccountEntity | Automated |
| CS-HAWB-017 | Edge | Lowercase ProductGroup | Automated |
| CS-HAWB-018 | Edge | ProductGroup with leading/trailing spaces | Automated |
| CS-HAWB-019 | Edge | Lowercase AccountEntity | Automated |
| CS-HAWB-020 | Edge | AccountEntity with leading/trailing spaces | Automated |
| CS-HAWB-021 | Edge | Very long AccountEntity | Automated |
| CS-HAWB-022 | Edge | Special characters in AccountEntity | Automated |
| CS-HAWB-023 | Edge | Numeric ProductGroup | Automated |
| CS-HAWB-024 | Edge | Numeric AccountEntity | Automated |
| CS-HAWB-025 | Edge | Extra unknown fields are handled consistently | Automated |
| CS-HAWB-026 | Header/Gateway | Missing `x-api-key` | Automated |
| CS-HAWB-027 | Header/Gateway | Invalid `x-api-key` | Automated |
| CS-HAWB-028 | Header/Gateway | Missing `Content-Type` | Automated |
| CS-HAWB-029 | Header/Gateway | Invalid `Content-Type` | Automated |
| CS-HAWB-030 | Header/Gateway | Wrong HTTP method `GET` | Automated |
| CS-HAWB-031 | Header/Gateway | Wrong HTTP method `PUT` | Automated |
| CS-HAWB-032 | Header/Gateway | Direct Boomi Gateway call with required headers | Automated |
| CS-HAWB-033 | Header/Gateway | Direct Boomi Gateway call missing `X-API-KEY` | Automated |
| CS-HAWB-034 | Header/Gateway | Direct Boomi Gateway call with invalid `source` header | Automated |
| CS-HAWB-035 | Request Structure | Empty request body | Automated |
| CS-HAWB-036 | Request Structure | Malformed JSON | Automated |
| CS-HAWB-037 | Request Structure | Missing `ClientInfo` | Automated |
| CS-HAWB-038 | Request Structure | Missing `Shipments` | Automated |
| CS-HAWB-039 | Request Structure | Empty `Shipments` array | Automated |
| CS-HAWB-040 | Request Structure | Missing `Details` object | Automated |
| CS-HAWB-041 | Request Structure | `Shipments` is not an array | Automated |
| CS-HAWB-042 | Boomi Mapping/Integration | AccountEntity mapping to `Payload.Entity` through successful API flow | Automated response-level verification |
| CS-HAWB-043 | Boomi Mapping/Integration | ProductGroup mapping to `Payload.ProductGroup` through successful API flow | Automated response-level verification |
| CS-HAWB-044 | Boomi Mapping/Integration | Direct Gateway payload with `CategoryID = 3` is accepted | Automated |
| CS-HAWB-045 | Boomi Mapping/Integration | Direct Gateway payload with `MediaTypeID = 1` is accepted | Automated |
| CS-HAWB-046 | Boomi Mapping/Integration | Direct Gateway payload with `PaymentMethodID = 1` is accepted | Automated |
| CS-HAWB-047 | Boomi Mapping/Integration | Direct Gateway payload uses `Method = GetHAWBNumber` | Automated |
| CS-HAWB-048 | Boomi Mapping/Integration | Direct Gateway payload uses `IHAWBRangeService` contract | Automated |
| CS-HAWB-049 | Boomi Mapping/Integration | Backend HAWBRange method invocation returns HAWB data | Automated response-level verification |
| CS-HAWB-050 | Boomi Mapping/Integration | Backend business error is mapped clearly | Automated |
| CS-HAWB-051 | Boomi Mapping/Integration | Downstream timeout handling | Manual fault-injection scenario |
| CS-HAWB-052 | Boomi Mapping/Integration | Downstream endpoint failure handling | Manual fault-injection scenario |
| CS-HAWB-053 | Boomi Mapping/Integration | Process deployment to MCS Runtime | Manual deployment verification |
| CS-HAWB-054 | Boomi Mapping/Integration | API deployment and route through Gateway | Manual deployment verification |
| CS-HAWB-055 | Non-Functional | Success response schema validation | Automated |
| CS-HAWB-056 | Non-Functional | Error response schema validation | Automated |
| CS-HAWB-057 | Non-Functional | No sensitive data in success response | Automated |
| CS-HAWB-058 | Non-Functional | No sensitive data in error response | Automated |
| CS-HAWB-059 | Non-Functional | Response time validation for valid request | Automated |
| CS-HAWB-060 | Non-Functional | Response time validation for invalid request | Automated |
| CS-HAWB-061 | Non-Functional | Repeated valid request consistency | Automated |
| CS-HAWB-062 | Non-Functional | Katalon profile variables and secret handling | Manual configuration verification |

## Direct Boomi Gateway scenarios (`ws.staging.aramex.net`)

These scenarios use `endpointType: boomiGateway` and call `BOOMI_GATEWAY_URL` (`https://ws.staging.aramex.net/Wcfgateway/api/Gateway/Execute`):

- `CS-HAWB-032`, `CS-HAWB-033`, `CS-HAWB-034`
- `CS-HAWB-044`, `CS-HAWB-045`, `CS-HAWB-046`, `CS-HAWB-047`, `CS-HAWB-048`, `CS-HAWB-050`

By default, `RUN_BOOMI_GATEWAY_TESTS` is `false` in `Profiles/default.glbl`, so they are **skipped** unless you enable the flag and are on a network that can reach the staging WCF gateway (typically corporate VPN). This avoids `HttpHostConnectException: Connection timed out` on machines that can reach the main Boomi API (`BASE_URL`) but not `ws.staging.aramex.net`.

## Manual scenarios

The following scenarios are included in the JSON file as `executionMode: manual`. During suite execution they are logged as warnings, because they require deployment validation, backend simulation, or environment-level configuration:

- `CS-HAWB-051` downstream timeout handling
- `CS-HAWB-052` downstream endpoint failure handling
- `CS-HAWB-053` MCS Runtime deployment verification
- `CS-HAWB-054` API Gateway deployment verification
- `CS-HAWB-062` Katalon secure profile/secret verification

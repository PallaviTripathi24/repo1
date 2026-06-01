/**
 * Katalon Studio — Create Shipment (Boomi Gateway → SaveShipmentsAsync)
 *
 * Purpose:
 * - Executes parameterized Gateway/Execute requests for positive, negative, header, and routing scenarios.
 * - Validates HTTP 200 + Success flag + optional error message substring + response time.
 *
 * Prerequisites (Global Variables — see README):
 * - gatewayBaseUrl, boomiApiKey, defaultSource, maxResponseTimeMs, executionScenario
 *
 * Data-driven:
 * - Bind column "executionScenario" in Test Data to override GlobalVariable per row (optional).
 *
 * Security:
 * - Never log full API keys. Mask in logs.
 *
 * Compatible with: Katalon Studio 8.x / 9.x (Web Service keywords)
 */
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent.TextContentType
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import internal.GlobalVariable

// ---------------------------------------------------------------------------
// Configuration (tune per environment / profile)
// ---------------------------------------------------------------------------

/** Max allowed round-trip time for Gateway/Execute (milliseconds). */
int MAX_RESPONSE_TIME_MS = resolveMaxResponseTimeMs()

/** Scenario key selects payload + expectation set. Override via GlobalVariable or Test Data binding. */
String SCENARIO_KEY = resolveScenarioKey()

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

try {
	runScenario(SCENARIO_KEY)
} catch (Exception e) {
	KeywordUtil.markFailed("Create Shipment Gateway test failed: ${e.message}")
	throw e
}

// ---------------------------------------------------------------------------
// Core scenario runner
// ---------------------------------------------------------------------------

void runScenario(String scenarioKey) {
	KeywordUtil.logInfo("=== Create Shipment Gateway /Execute ===")
	KeywordUtil.logInfo("Scenario: ${scenarioKey}")
	KeywordUtil.logInfo("Gateway base URL (host only in logs): ${maskUrl(GlobalVariable.gatewayBaseUrl)}")

	Map scenario = buildScenarioDefinition(scenarioKey)

	String url = buildGatewayExecuteUrl(GlobalVariable.gatewayBaseUrl)
	Map headers = buildHeaders(scenario.headers as Map)
	String body = JsonOutput.toJson(scenario.payload)

	logSafeRequestPreview(url, headers, body)

	long t0 = System.currentTimeMillis()
	def response = sendGatewayPost(url, headers, body)
	long elapsed = System.currentTimeMillis() - t0

	KeywordUtil.logInfo("Elapsed (client-side): ${elapsed} ms")

	WS.verifyResponseStatusCode(response, 200)
	if (elapsed > MAX_RESPONSE_TIME_MS) {
		KeywordUtil.markFailed("Response time exceeded threshold: ${elapsed} ms > ${MAX_RESPONSE_TIME_MS} ms")
	}

	String raw = response.getResponseBodyContent()
	KeywordUtil.logInfo("Response body: ${raw}")

	def json = new JsonSlurper().parseText(raw ?: '{}')
	assertJsonEnvelope(json, scenario)

	// Optional: Katalon native property checks when paths are flat
	if (json.containsKey('Success')) {
		if (scenario.expectSuccess) {
			WS.verifyElementPropertyValue(response, 'Success', true)
		} else {
			WS.verifyElementPropertyValue(response, 'Success', false)
		}
	}

	if (scenario.errorContains) {
		String haystack = extractErrorText(json).toLowerCase()
		String needle = scenario.errorContains.toString().toLowerCase()
		assert haystack.contains(needle) : "Expected error text containing '${scenario.errorContains}' but got: ${haystack}"
	}

	// Stash commonly reused values for chained tests (best-effort)
	stashExtractedIdentifiers(json)

	KeywordUtil.logInfo("=== Scenario PASSED: ${scenarioKey} ===")
}

// ---------------------------------------------------------------------------
// HTTP transport
// ---------------------------------------------------------------------------

def sendGatewayPost(String url, Map headers, String jsonBody) {
	RequestObject request = new RequestObject("Gateway_Execute_Inline_${System.currentTimeMillis()}")
	request.setRestUrl(url)
	request.setRestRequestMethod('POST')

	List<TestObjectProperty> headerProps = []
	headers.each { String k, String v ->
		headerProps.add(new TestObjectProperty(k, ConditionType.EQUALS, v, false))
	}
	request.setHttpHeaderProperties(headerProps)

	// APPLICATION_JSON content type for body
	request.setBodyContent(new HttpTextBodyContent(jsonBody, 'UTF-8', TextContentType.APPLICATION_JSON))

	def response = WS.sendRequest(request)

	// Katalon built-in threshold check (uses response metadata where available)
	try {
		WS.verifyResponseTime(response, MAX_RESPONSE_TIME_MS)
	} catch (MissingMethodException ignored) {
		KeywordUtil.logInfo('WS.verifyResponseTime not available in this Katalon version; using client-side elapsed check only.')
	}

	return response
}

// ---------------------------------------------------------------------------
// Scenario definitions (extend for full regression matrix)
// ---------------------------------------------------------------------------

Map buildScenarioDefinition(String key) {
	// Defaults for a valid shipment row (align with your canonical / Boomi mapping)
	Map baseShipment = baselineShipment()
	Map basePayload = [
		contract: 'Corp.InfoAXS.Alerting.Contracts.IWaybillService',
		method  : 'SaveShipmentsAsync',
		payload : [
			Shipments: [baseShipment],
			UserId    : '12345'
		]
	]

	switch (key) {
		case 'VALID_BASELINE':
			return [
				expectSuccess: true,
				headers      : [:],
				payload      : basePayload,
				errorContains: null
			]
		case 'VALID_MULTI_SHIPMENT':
			def s1 = baselineShipment()
			def s2 = baselineShipment()
			s2.Guid = UUID.randomUUID().toString()
			s2.ShipperReference = 'REF_SECOND'
			return [
				expectSuccess: true,
				headers      : [:],
				payload      : [
					contract: basePayload.contract,
					method  : basePayload.method,
					payload : [
						Shipments: [s1, s2],
						UserId    : '12345'
					]
				],
				errorContains: null
			]
		case 'NEG_BLANK_GUID':
			def s = baselineShipment()
			s.Guid = ''
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : clonePayloadWithShipments(basePayload, [s]),
				errorContains: 'guid' // relax matching; adjust per actual API messages
			]
		case 'NEG_NULL_GUID':
			def s = baselineShipment()
			s.Guid = null
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : clonePayloadWithShipments(basePayload, [s]),
				errorContains: null
			]
		case 'NEG_INVALID_GUID':
			def s = baselineShipment()
			s.Guid = 'not-a-valid-uuid'
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : clonePayloadWithShipments(basePayload, [s]),
				errorContains: null
			]
		case 'NEG_EMPTY_SHIPMENTS':
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : [
					contract: basePayload.contract,
					method  : basePayload.method,
					payload : [
						Shipments: [],
						UserId    : '12345'
					]
				],
				errorContains: null
			]
		case 'NEG_INVALID_COUNTRY':
			def s = baselineShipment()
			s.DestCountryCode = 'XX'
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : clonePayloadWithShipments(basePayload, [s]),
				errorContains: null
			]
		case 'NEG_INVALID_PRODUCT_TYPE':
			def s = baselineShipment()
			s.Type = 'INVALID_TYPE'
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : clonePayloadWithShipments(basePayload, [s]),
				errorContains: null
			]
		case 'NEG_MISSING_USER_ID':
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : [
					contract: basePayload.contract,
					method  : basePayload.method,
					payload : [
						Shipments: [baselineShipment()]
						// UserId intentionally omitted
					]
				],
				errorContains: null
			]
		case 'HDR_MISSING_API_KEY':
			return [
				expectSuccess: false,
				headers      : [stripApiKey: true],
				payload      : basePayload,
				errorContains: null
			]
		case 'HDR_INVALID_API_KEY':
			return [
				expectSuccess: false,
				headers      : [apiKeyOverride: 'INVALID_KEY_PLACEHOLDER'],
				payload      : basePayload,
				errorContains: null
			]
		case 'HDR_MISSING_SOURCE':
			return [
				expectSuccess: false,
				headers      : [stripSource: true],
				payload      : basePayload,
				errorContains: null
			]
		case 'HDR_MISSING_CONTENT_TYPE':
			return [
				expectSuccess: false,
				headers      : [stripContentType: true],
				payload      : basePayload,
				errorContains: null
			]
		case 'HDR_WRONG_CONTENT_TYPE':
			return [
				expectSuccess: false,
				headers      : [contentTypeOverride: 'text/plain'],
				payload      : basePayload,
				errorContains: null
			]
		case 'ROUTE_WRONG_CONTRACT':
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : [
					contract: 'Invalid.Contract.IExample',
					method  : 'SaveShipmentsAsync',
					payload : basePayload.payload
				],
				errorContains: null
			]
		case 'ROUTE_WRONG_METHOD':
			return [
				expectSuccess: false,
				headers      : [:],
				payload      : [
					contract: basePayload.contract,
					method  : 'WrongMethod',
					payload : basePayload.payload
				],
				errorContains: null
			]
		default:
			KeywordUtil.markFailed("Unknown executionScenario / scenario key: ${key}")
			throw new IllegalArgumentException("Unknown scenario key: ${key}")
	}
}

Map baselineShipment() {
	String guid = UUID.randomUUID().toString()
	return [
		Guid                : guid,
		CreatedBy           : 12345,
		CreatedDate         : '2026-04-27T12:00:00',
		IsLocalized         : false,
		AWB                 : 'REPLACE_WITH_VALID_AWB_OR_EMPTY_PER_ENV',
		ChargingWeight      : 0.5,
		Weight              : 0.5,
		WeightUnit          : 'Kg',
		SourceID            : 64,
		ReceivedAt          : 'Reception',
		Origin              : 'DXB',
		Destination         : 'RUH',
		CMT                 : 'P',
		PayMethod           : 'P',
		PaymentType         : '',
		ProductGroup        : 'EXP',
		Type                : 'EPX',
		PickupDate          : '2026-04-27T12:00:00',
		DueDate             : '2026-04-27T12:00:00',
		PCS                 : 1,
		CommodityDescription: 'Docs',
		Customs             : 10,
		CustomsCurrency     : 'AED',
		CODValue            : 0,
		CODCurrency         : 'AED',
		InsuranceValue      : 0,
		InsuranceCurrency   : '',
		CashValue           : 0,
		CashValueCurrency   : '',
		CollectAmount       : 0,
		CollectCurrency     : '',
		ShipperNumber       : '45796',
		ShipperName         : 'Aramex',
		SentBy              : 'Michael',
		ShipperReference    : 'Ref 111111',
		ShipperReference2   : 'Ref 222222',
		ShipperAddress      : 'Mecca St',
		ShipperTel          : '97148707766',
		ShipperTel2         : '',
		ShipperMobile       : '07777777',
		OriginCity          : 'Dubai',
		OriginState         : '',
		OriginCountryCode   : 'AE',
		OriginZipCode       : '',
		ShipperFax          : '',
		ShipperEmail        : 'shipper@example.com',
		ConsigneeNumber     : '45796',
		ConsigneeName       : 'Aramex',
		AttnOf              : 'Mazen',
		ConsigneeReference  : 'Ref 333333',
		ConsigneeReference2 : 'Ref 444444',
		ConsigneeAddress    : '15 ABC St',
		ConsigneeTelephone  : '962795979550',
		ConsigneeTel2       : '',
		ConsigneeMobile     : '962795979550',
		DestCity            : 'Nimra',
		DestState           : '',
		DestCountryCode     : 'SA',
		DestZipCode         : '',
		ConsigneeFAX        : '',
		ConsigneeEmail     : 'consignee@example.com',
		ModifiedBy          : 12345,
		Services            : '',
		ForeignHAWBNumber   : '',
		Remarks             : 'Shpt 0001',
		HandlingInformation : '',
		AccountingInformation: '',
		Reference1          : 'Shpt 0001',
		Reference2          : '',
		Reference3          : '',
		TransportType       : 0,
		ShipmentItems       : [
			[
				CustomsValue      : 500,
				Description       : 'Description',
				Reference         : 'itemno',
				Pieces            : 4,
				Packing           : 'Box',
				CommodityNo       : 'COMNO',
				CountryOfOrigin   : '',
				GoodsDescription  : 'Description',
				ItemNumber        : 'itemno',
				GrossWeight       : 0.5
			]
		]
	]
}

Map clonePayloadWithShipments(Map basePayload, List shipments) {
	return [
		contract: basePayload.contract,
		method  : basePayload.method,
		payload : [
			Shipments: shipments,
			UserId    : basePayload.payload.UserId
		]
	]
}

// ---------------------------------------------------------------------------
// Headers
// ---------------------------------------------------------------------------

Map buildHeaders(Map scenarioHeaderFlags) {
	Map flags = scenarioHeaderFlags ?: [:]
	String apiKey = GlobalVariable.boomiApiKey?.toString()
	String source = GlobalVariable.defaultSource?.toString()

	Map headers = [:]
	if (!flags.stripApiKey) {
		headers['X-API-KEY'] = (flags.apiKeyOverride ?: apiKey)
	}
	if (!flags.stripSource) {
		headers['source'] = source
	}
	if (!flags.stripContentType) {
		String ct = flags.contentTypeOverride ?: 'application/json'
		headers['Content-Type'] = ct
	}
	return headers
}

String buildGatewayExecuteUrl(String gatewayBaseUrl) {
	String base = gatewayBaseUrl?.toString()?.trim()
	assert base : 'GlobalVariable.gatewayBaseUrl is required'
	if (base.endsWith('/')) {
		base = base[0..-2]
	}
	return "${base}/Wcfgateway/api/Gateway/Execute"
}

// ---------------------------------------------------------------------------
// Assertions
// ---------------------------------------------------------------------------

void assertJsonEnvelope(def json, Map scenario) {
	assert json != null : 'Parsed JSON is null'

	// Top-level success flag (per your acceptance criteria)
	assert json.containsKey('Success') : "Response missing 'Success' field. Keys: ${json.keySet()}"
	assert json.Success instanceof Boolean : "'Success' must be boolean"

	if (scenario.expectSuccess) {
		assert json.Success == true : "Expected Success=true"
	} else {
		assert json.Success == false : "Expected Success=false for negative scenario"
		assert extractErrorText(json).length() > 0 : 'Expected a non-empty error/validation message for failed cases'
	}
}

String extractErrorText(def json) {
	// Normalize common error shapes without assuming a single schema
	def parts = []
	if (json.containsKey('Message')) {
		parts << json.Message?.toString()
	}
	if (json.containsKey('ErrorMessage')) {
		parts << json.ErrorMessage?.toString()
	}
	if (json.containsKey('Errors')) {
		parts << json.Errors?.toString()
	}
	if (json.containsKey('ValidationErrors')) {
		parts << json.ValidationErrors?.toString()
	}
	if (json.containsKey('Data') && json.Data instanceof Map) {
		parts << json.Data.Message?.toString()
		parts << json.Data.ErrorMessage?.toString()
	}
	return parts.findAll { it }.join(' | ')
}

void stashExtractedIdentifiers(def json) {
	try {
		GlobalVariable.lastGatewayResponseJson = JsonOutput.toJson(json)
	} catch (MissingPropertyException ignored) {
		// Define optional GlobalVariable lastGatewayResponseJson if you want to chain tests
	}
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

String resolveScenarioKey() {
	// Prefer explicit GlobalVariable; allow Test Data binding in Katalon by defining
	// a Test Case variable with the same name in the Manual view or use a Test Listener.
	String key = 'VALID_BASELINE'
	try {
		key = GlobalVariable.executionScenario
	} catch (MissingPropertyException ignored) {
		// keep default
	}
	return key?.toString()?.trim() ?: 'VALID_BASELINE'
}

int safeInt(Object v, int defaultValue) {
	try {
		return (v ?: defaultValue) as int
	} catch (Exception ignored) {
		return defaultValue
	}
}

int resolveMaxResponseTimeMs() {
	try {
		return safeInt(GlobalVariable.maxResponseTimeMs, 15000)
	} catch (MissingPropertyException ignored) {
		return 15000
	}
}

String maskUrl(String url) {
	if (!url) {
		return ''
	}
	try {
		def u = new java.net.URL(url)
		return "${u.protocol}://${u.host}/..."
	} catch (Exception ignored) {
		return '(unparseable url)'
	}
}

void logSafeRequestPreview(String url, Map headers, String body) {
	KeywordUtil.logInfo("POST ${url}")
	Map safeHeaders = new LinkedHashMap(headers)
	if (safeHeaders.containsKey('X-API-KEY')) {
		String k = safeHeaders['X-API-KEY']?.toString()
		safeHeaders['X-API-KEY'] = (k?.length() > 6) ? "${k.substring(0, 3)}...${k.substring(k.length() - 3)}" : '(set)'
	}
	KeywordUtil.logInfo("Headers (masked): ${safeHeaders}")
	KeywordUtil.logInfo("Request body: ${body}")
}

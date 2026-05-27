package api

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import internal.GlobalVariable

class CreateShipmentHawbKeywords {

    private static final String JSON_CONTENT_TYPE = 'application/json'

    @Keyword
    static void runScenarioFile(String relativePath) {
        File scenarioFile = new File(RunConfiguration.getProjectDir(), relativePath)
        if (!scenarioFile.exists()) {
            KeywordUtil.markFailedAndStop("Scenario file not found: ${scenarioFile.absolutePath}")
        }

        List scenarios = (List) new JsonSlurper().parse(scenarioFile)
        List<String> failures = []

        scenarios.eachWithIndex { Map scenario, int index ->
            String scenarioId = scenario.id ?: "Scenario-${index + 1}"
            try {
                runScenario(scenario)
                KeywordUtil.logInfo("PASSED ${scenarioId} - ${scenario.description}")
            } catch (Throwable t) {
                String message = "FAILED ${scenarioId} - ${scenario.description}: ${t.message}"
                failures.add(message)
                KeywordUtil.markWarning(message)
            }
        }

        if (!failures.isEmpty()) {
            KeywordUtil.markFailedAndStop("Create Shipment HAWB API suite failed ${failures.size()} scenario(s):\n" + failures.join('\n'))
        }
    }

    @Keyword
    static void runScenario(Map scenario) {
        if ((scenario.executionMode ?: 'automated') == 'manual') {
            KeywordUtil.markWarning("Manual verification required for ${scenario.id}: ${scenario.manualNote ?: scenario.description}")
            return
        }

        PayloadData payload = buildPayload(scenario)
        RequestObject request = buildRequest(scenario, payload)
        ResponseObject response = WS.sendRequest(request, FailureHandling.STOP_ON_FAILURE)

        int expectedStatus = asInt(scenario.expectedStatus, 200)
        assertEquals("HTTP status", expectedStatus, response.getStatusCode())

        String responseBody = response.getResponseBodyContent() ?: ''
        Object responseJson = parseJsonIfPossible(responseBody)

        if (scenario.containsKey('expectedSuccess')) {
            Object successValue = findValueIgnoreCase(responseJson, 'Success')
            assertTrue("Response must contain Success field", successValue != null)
            assertEquals("Success flag", asBoolean(scenario.expectedSuccess), asBoolean(successValue))
        }

        if (scenario.expectedMessageAnyOf) {
            assertBodyContainsAny(responseBody, (List) scenario.expectedMessageAnyOf)
        }

        if (scenario.expectedBodyAnyOf) {
            assertBodyContainsAny(responseBody, (List) scenario.expectedBodyAnyOf)
        }

        if (asBoolean(scenario.assertSuccessSchema ?: false)) {
            assertTrue("Success response must be a JSON object", responseJson instanceof Map)
            assertTrue("Success response must contain Success field", findValueIgnoreCase(responseJson, 'Success') != null)
        }

        if (asBoolean(scenario.assertErrorSchema ?: false)) {
            assertTrue("Error response must be a JSON object", responseJson instanceof Map)
            assertTrue("Error response must contain Success field", findValueIgnoreCase(responseJson, 'Success') != null)
            assertTrue("Error response must contain an error/message field", containsAnyKeyIgnoreCase(responseJson, ['Error', 'Errors', 'Message', 'Description', 'StatusDescription']))
        }

        if (asBoolean(scenario.assertNoSensitiveData ?: false)) {
            assertNoSensitiveData(responseBody)
        }

        if (asBoolean(scenario.assertResponseTime ?: false)) {
            long slaMs = String.valueOf(GlobalVariable.RESPONSE_TIME_SLA_MS).toLong()
            assertTrue("Response time ${response.getElapsedTime()} ms must be <= ${slaMs} ms", response.getElapsedTime() <= slaMs)
        }
    }

    private static RequestObject buildRequest(Map scenario, PayloadData payload) {
        String endpointType = scenario.endpointType ?: 'main'
        String method = scenario.method ?: 'POST'
        String url = endpointType == 'boomiGateway'
                ? String.valueOf(GlobalVariable.BOOMI_GATEWAY_URL)
                : String.valueOf(GlobalVariable.BASE_URL) + String.valueOf(GlobalVariable.CREATE_SHIPMENT_HAWB_PATH)

        RequestObject request = new RequestObject(scenario.id ?: 'Create Shipment HAWB Request')
        request.setRestUrl(url)
        request.setRestRequestMethod(method)
        request.setHttpHeaderProperties(headersFor(scenario))
        request.setBodyContent(new HttpTextBodyContent(payload.body ?: '', 'UTF-8', payload.contentType ?: JSON_CONTENT_TYPE))
        return request
    }

    private static List<TestObjectProperty> headersFor(Map scenario) {
        String variant = scenario.headersVariant ?: ((scenario.endpointType ?: 'main') == 'boomiGateway' ? 'validGateway' : 'validMain')
        List<TestObjectProperty> headers = []

        switch (variant) {
            case 'validMain':
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                headers.add(header('x-api-key', String.valueOf(GlobalVariable.API_KEY)))
                break
            case 'missingApiKey':
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                break
            case 'invalidApiKey':
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                headers.add(header('x-api-key', 'invalid-key'))
                break
            case 'missingContentType':
                headers.add(header('x-api-key', String.valueOf(GlobalVariable.API_KEY)))
                break
            case 'invalidContentType':
                headers.add(header('Content-Type', 'text/plain'))
                headers.add(header('x-api-key', String.valueOf(GlobalVariable.API_KEY)))
                break
            case 'validGateway':
                headers.add(header('X-API-KEY', String.valueOf(GlobalVariable.BOOMI_API_KEY)))
                headers.add(header('source', String.valueOf(GlobalVariable.BOOMI_SOURCE)))
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                break
            case 'missingGatewayApiKey':
                headers.add(header('source', String.valueOf(GlobalVariable.BOOMI_SOURCE)))
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                break
            case 'invalidGatewayApiKey':
                headers.add(header('X-API-KEY', 'invalid-boomi-key'))
                headers.add(header('source', String.valueOf(GlobalVariable.BOOMI_SOURCE)))
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                break
            case 'invalidGatewaySource':
                headers.add(header('X-API-KEY', String.valueOf(GlobalVariable.BOOMI_API_KEY)))
                headers.add(header('source', 'invalid-source'))
                headers.add(header('Content-Type', JSON_CONTENT_TYPE))
                break
            default:
                throw new IllegalArgumentException("Unsupported headersVariant: ${variant}")
        }

        return headers
    }

    private static TestObjectProperty header(String name, String value) {
        return new TestObjectProperty(name, ConditionType.EQUALS, value)
    }

    private static PayloadData buildPayload(Map scenario) {
        String endpointType = scenario.endpointType ?: 'main'
        String variant = scenario.payloadVariant ?: 'validExp'

        if (endpointType == 'boomiGateway') {
            return new PayloadData(body: JsonOutput.toJson(buildGatewayPayload(variant)))
        }

        if (variant == 'emptyBody') {
            return new PayloadData(body: '')
        }
        if (variant == 'malformedJson') {
            return new PayloadData(body: '{"ClientInfo":{"AccountEntity":"DXB"},"Shipments":[{"Details":{"ProductGroup":"EXP"}}]')
        }

        Map payload = [
                ClientInfo: [
                        AccountEntity: 'DXB'
                ],
                Shipments : [[
                        Details: [
                                ProductGroup: 'EXP'
                        ]
                ]]
        ]

        switch (variant) {
            case 'validExp':
                break
            case 'validDom':
                payload.Shipments[0].Details.ProductGroup = 'DOM'
                break
            case 'optionalFields':
                payload.ClientInfo.AccountNumber = '123456'
                payload.Transaction = [Reference1: 'AUTO-HAWB']
                payload.Shipments[0].Reference1 = 'AUTO-OPTIONAL'
                payload.Shipments[0].Details.DescriptionOfGoods = 'Automation test shipment'
                break
            case 'multipleShipmentsValidFirst':
                payload.Shipments.add([Details: [ProductGroup: 'DOM']])
                break
            case 'invalidProductGroup':
                payload.Shipments[0].Details.ProductGroup = 'XYZ'
                break
            case 'emptyProductGroup':
                payload.Shipments[0].Details.ProductGroup = ''
                break
            case 'missingProductGroup':
                payload.Shipments[0].Details.remove('ProductGroup')
                break
            case 'nullProductGroup':
                payload.Shipments[0].Details.ProductGroup = null
                break
            case 'invalidAccountEntity':
                payload.ClientInfo.AccountEntity = 'INVALID'
                break
            case 'emptyAccountEntity':
                payload.ClientInfo.AccountEntity = ''
                break
            case 'missingAccountEntity':
                payload.ClientInfo.remove('AccountEntity')
                break
            case 'nullAccountEntity':
                payload.ClientInfo.AccountEntity = null
                break
            case 'bothInvalid':
                payload.ClientInfo.AccountEntity = 'INVALID'
                payload.Shipments[0].Details.ProductGroup = 'XYZ'
                break
            case 'bothEmpty':
                payload.ClientInfo.AccountEntity = ''
                payload.Shipments[0].Details.ProductGroup = ''
                break
            case 'lowerProductGroup':
                payload.Shipments[0].Details.ProductGroup = 'exp'
                break
            case 'spacedProductGroup':
                payload.Shipments[0].Details.ProductGroup = ' EXP '
                break
            case 'lowerAccountEntity':
                payload.ClientInfo.AccountEntity = 'dxb'
                break
            case 'spacedAccountEntity':
                payload.ClientInfo.AccountEntity = ' DXB '
                break
            case 'longAccountEntity':
                payload.ClientInfo.AccountEntity = 'D' * 256
                break
            case 'specialAccountEntity':
                payload.ClientInfo.AccountEntity = 'D@B#'
                break
            case 'numericProductGroup':
                payload.Shipments[0].Details.ProductGroup = 123
                break
            case 'numericAccountEntity':
                payload.ClientInfo.AccountEntity = 123
                break
            case 'extraUnknownFields':
                payload.UnexpectedRootField = 'ignored-or-rejected-by-design'
                payload.ClientInfo.UnexpectedClientField = 'ignored-or-rejected-by-design'
                payload.Shipments[0].UnexpectedShipmentField = 'ignored-or-rejected-by-design'
                break
            case 'missingClientInfo':
                payload.remove('ClientInfo')
                break
            case 'missingShipments':
                payload.remove('Shipments')
                break
            case 'emptyShipments':
                payload.Shipments = []
                break
            case 'missingDetails':
                payload.Shipments[0].remove('Details')
                break
            case 'shipmentsNotArray':
                payload.Shipments = [Details: [ProductGroup: 'EXP']]
                break
            default:
                throw new IllegalArgumentException("Unsupported payloadVariant: ${variant}")
        }

        return new PayloadData(body: JsonOutput.toJson(payload))
    }

    private static Map buildGatewayPayload(String variant) {
        Map gatewayPayload = [
                Method  : 'GetHAWBNumber',
                Contract: 'Corp.InfoAXS.ExpAcc.Contracts.IHAWBRangeService',
                Payload : [
                        Entity         : 'DXB',
                        ProductGroup   : 'EXP',
                        CategoryID     : 3,
                        MediaTypeID    : 1,
                        PaymentMethodID: 1
                ]
        ]

        switch (variant) {
            case 'validGatewayExp':
            case 'validExp':
                break
            case 'validGatewayDom':
                gatewayPayload.Payload.ProductGroup = 'DOM'
                break
            case 'gatewayBackendBusinessError':
                gatewayPayload.Payload.Entity = 'INVALID'
                break
            default:
                throw new IllegalArgumentException("Unsupported Boomi Gateway payloadVariant: ${variant}")
        }

        return gatewayPayload
    }

    private static Object parseJsonIfPossible(String body) {
        if (!body?.trim()) {
            return null
        }
        try {
            return new JsonSlurper().parseText(body)
        } catch (Exception ignored) {
            return null
        }
    }

    private static Object findValueIgnoreCase(Object node, String key) {
        if (node instanceof Map) {
            Map map = (Map) node
            Object directKey = map.keySet().find { String.valueOf(it).equalsIgnoreCase(key) }
            if (directKey != null) {
                return map[directKey]
            }
            for (Object value : map.values()) {
                Object found = findValueIgnoreCase(value, key)
                if (found != null) {
                    return found
                }
            }
        }
        if (node instanceof List) {
            for (Object item : (List) node) {
                Object found = findValueIgnoreCase(item, key)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    private static boolean containsAnyKeyIgnoreCase(Object node, List<String> keys) {
        if (node instanceof Map) {
            Map map = (Map) node
            if (map.keySet().any { Object candidate -> keys.any { String.valueOf(candidate).equalsIgnoreCase(it) } }) {
                return true
            }
            return map.values().any { containsAnyKeyIgnoreCase(it, keys) }
        }
        if (node instanceof List) {
            return ((List) node).any { containsAnyKeyIgnoreCase(it, keys) }
        }
        return false
    }

    private static void assertBodyContainsAny(String body, List values) {
        String normalizedBody = (body ?: '').toLowerCase()
        boolean found = values.any { normalizedBody.contains(String.valueOf(it).toLowerCase()) }
        assertTrue("Response body must contain one of: ${values}", found)
    }

    private static void assertNoSensitiveData(String body) {
        List<String> disallowed = ['x-api-key', 'api_key', 'apikey', 'password', 'secret', 'token', 'authorization', 'stack trace', 'stacktrace']
        String normalizedBody = (body ?: '').toLowerCase()
        List<String> found = disallowed.findAll { normalizedBody.contains(it) }
        assertTrue("Response must not expose sensitive/internal data. Found markers: ${found}", found.isEmpty())
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value
        }
        String text = String.valueOf(value)
        if (text.equalsIgnoreCase('true') || text == '1') {
            return true
        }
        if (text.equalsIgnoreCase('false') || text == '0') {
            return false
        }
        throw new IllegalArgumentException("Value cannot be converted to boolean: ${value}")
    }

    private static int asInt(Object value, int defaultValue) {
        return value == null ? defaultValue : String.valueOf(value).toInteger()
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("${label} expected <${expected}> but was <${actual}>")
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            throw new AssertionError(label)
        }
    }

    private static class PayloadData {
        String body
        String contentType = JSON_CONTENT_TYPE
    }
}

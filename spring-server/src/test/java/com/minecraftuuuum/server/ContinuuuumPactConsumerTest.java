package com.minecraftuuuum.server;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consumer contract: Minecraftuuuum Spring UCC → Continuuuum Flask.
 * Cave {@code POST /cave/route} lives on Spring, not in this pact.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "continuuuum", pactVersion = PactSpecVersion.V3)
class ContinuuuumPactConsumerTest {
    private static final String TENANT = "minecraftuuuum";

    @Pact(consumer = "minecraftuuuum")
    RequestResponsePact continuuuumTenantApis(PactDslWithProvider builder) {
        return builder
                .given("library search is available")
                .uponReceiving("a library search reachability probe")
                .path("/api/library/search")
                .method("GET")
                .query("limit=1")
                .headers(ContinuuuumTenantClient.TENANT_HEADER, TENANT)
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("[]")
                .given("minecraftuuuum tenant payroll is seeded")
                .uponReceiving("a tenant retainer split request")
                .path("/api/payroll/tenants/" + TENANT + "/split")
                .method("GET")
                .headers(ContinuuuumTenantClient.TENANT_HEADER, TENANT)
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringValue("tenantId", TENANT)
                        .stringType("companyId", "pay_example0000")
                        .numberType("creatorPct", 0.70)
                        .numberType("platformPct", 0.30)
                        .numberType("continuuuumHwmPct", 0.10)
                        .stringValue("platformKind", "platform_microsoft")
                        .booleanType("platformEnabled", true)
                        .booleanType("serviceUnityEnabled", false)
                        .booleanType("serviceCursorEnabled", false)
                        .booleanType("serviceUnrealEnabled", false)
                        .booleanType("retainer", true))
                .given("minecraftuuuum oauth connections table exists")
                .uponReceiving("a tenant oauth connections request")
                .path("/api/tenant/oauth-connections")
                .method("GET")
                .headers(ContinuuuumTenantClient.TENANT_HEADER, TENANT)
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringValue("tenantId", TENANT)
                        .array("items")
                        .closeArray())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "continuuuumTenantApis")
    void minecraftuuuumReadsContinuuuumTenantApis(MockServer mockServer) {
        ContinuuuumTenantClient client = new ContinuuuumTenantClient(
                TENANT, "default", mockServer.getUrl(), "", new ObjectMapper());

        assertTrue(client.continuuuumReachable());

        Map<String, Object> split = client.fetchTenantSplit();
        assertEquals("continuuuum", split.get("source"));
        assertEquals(TENANT, split.get("tenantId"));
        assertEquals(0.70, ((Number) split.get("creatorPct")).doubleValue(), 1e-6);
        assertEquals(0.30, ((Number) split.get("platformPct")).doubleValue(), 1e-6);
        assertEquals(0.10, ((Number) split.get("continuuuumHwmPct")).doubleValue(), 1e-6);
        assertEquals("platform_microsoft", split.get("platformKind"));
        assertEquals(Boolean.TRUE, split.get("platformEnabled"));
        assertEquals(Boolean.FALSE, split.get("serviceUnityEnabled"));
        assertEquals(Boolean.FALSE, split.get("serviceCursorEnabled"));
        assertEquals(Boolean.TRUE, split.get("retainer"));

        Map<String, Object> oauth = client.fetchOauthStatus();
        assertEquals(TENANT, oauth.get("tenantId"));
        assertInstanceOf(List.class, oauth.get("items"));
    }
}

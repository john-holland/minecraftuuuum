package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class LibraryProxyController {
    private final ContinuuuumTenantClient tenantClient;
    private final ObjectMapper mapper;

    public LibraryProxyController(ContinuuuumTenantClient tenantClient, ObjectMapper mapper) {
        this.tenantClient = tenantClient;
        this.mapper = mapper;
    }

    @RequestMapping("/api/library/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        String pathAndQuery = uri + (qs == null || qs.isBlank() ? "" : "?" + qs);
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String contentType = request.getContentType();
        ContinuuuumTenantClient.ProxyResponse resp =
                tenantClient.proxyLibrary(request.getMethod(), pathAndQuery, body.length == 0 ? null : body, contentType);
        HttpHeaders headers = new HttpHeaders();
        headers.set(ContinuuuumTenantClient.TENANT_HEADER, resp.tenant());
        headers.setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        if (resp.body() != null) {
            bytes = resp.body().getBytes(StandardCharsets.UTF_8);
        } else if (resp.json() != null) {
            bytes = mapper.writeValueAsBytes(resp.json());
        } else {
            bytes = "{\"ok\":false}".getBytes(StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(resp.status()).headers(headers).body(bytes);
    }
}

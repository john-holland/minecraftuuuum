package com.minecraftuuuum.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {
    @Value("${minecraftuuuum.optional-fee-platforms.cursor:false}")
    private boolean cursorOptional;

    @Value("${minecraftuuuum.lm-studio-base}")
    private String lmStudioBase;

    @Value("${minecraftuuuum.lm-studio-model}")
    private String lmStudioModel;

    private final ContinuuuumTenantClient tenantClient;

    public SettingsController(ContinuuuumTenantClient tenantClient) {
        this.tenantClient = tenantClient;
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        Map<String, Object> fees = new LinkedHashMap<>();
        fees.put("cursor", cursorOptional);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("optionalFeePlatforms", fees);
        out.put("lmStudioBase", lmStudioBase);
        out.put("lmStudioModel", lmStudioModel);
        out.put("tenantId", tenantClient.tenantId());
        out.put("fallbackTenant", tenantClient.fallbackTenant());
        out.put("continuuuumBase", tenantClient.continuuuumBase());
        out.put("uscBase", tenantClient.uscBase());
        Map<String, Object> split = tenantClient.fetchTenantSplit();
        out.put("retainer", split);
        out.put("oauth", tenantClient.fetchOauthStatus());
        return out;
    }
}

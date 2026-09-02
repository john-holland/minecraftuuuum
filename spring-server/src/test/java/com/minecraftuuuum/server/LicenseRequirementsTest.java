package com.minecraftuuuum.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseRequirementsTest {
    @Test
    void webOnlyHasNoUnityItems() {
        List<LicenseRequirements.Item> items = LicenseRequirements.required("web", false, false, "none");
        assertTrue(items.stream().anyMatch(i -> i.id().equals("ack_mojang_disclaimer")));
        assertFalse(items.stream().anyMatch(i -> i.id().startsWith("ack_unity")));
    }

    @Test
    void unityLocalAddsEditorAndNoCommitBuild() {
        List<LicenseRequirements.Item> items = LicenseRequirements.required("unity_webgl", false, false, "personal");
        assertTrue(items.stream().anyMatch(i -> i.id().equals("ack_unity_editor_license")));
        assertTrue(items.stream().anyMatch(i -> i.id().equals("ack_no_commit_build")));
        assertFalse(items.stream().anyMatch(i -> i.id().equals("ack_unity_redistribution")));
    }

    @Test
    void redistributeAndPublishAddExtraAcks() {
        List<LicenseRequirements.Item> items = LicenseRequirements.required("unity_webgl", true, true, "pro");
        assertTrue(items.stream().anyMatch(i -> i.id().equals("ack_unity_redistribution")));
        assertTrue(items.stream().anyMatch(i -> i.id().equals("ack_marketplace_retainer")));
        List<String> missing = LicenseRequirements.missing(items, List.of("ack_mojang_disclaimer"));
        assertTrue(missing.contains("ack_unity_editor_license"));
        assertTrue(LicenseRequirements.missing(items, items.stream().map(LicenseRequirements.Item::id).toList()).isEmpty());
    }
}

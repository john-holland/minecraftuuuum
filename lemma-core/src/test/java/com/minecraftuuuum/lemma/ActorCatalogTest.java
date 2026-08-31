package com.minecraftuuuum.lemma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorCatalogTest {
    @Test
    void seedsOfficialActors() {
        assertTrue(ActorCatalog.all().size() > 100);
        assertEquals("mediapipe_holistic@v1", ActorCatalog.byId("minecraft:villager").poseEngine());
        assertEquals("mocapanything@v2", ActorCatalog.byId("wolf").poseEngine());
        assertEquals("wolf", ActorCatalog.byId("minecraft:wolf").species());
    }
}

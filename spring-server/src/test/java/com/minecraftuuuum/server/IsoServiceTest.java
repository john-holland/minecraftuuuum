package com.minecraftuuuum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimined.craftpressor.db.CraftpressorDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsoServiceTest {
    @TempDir
    Path tmp;

    @Test
    void joinAndTreePlaybackOmitCleanSiblings() throws Exception {
        Path dbPath = tmp.resolve("iso.db");
        try (CraftpressorDb db = new CraftpressorDb(dbPath)) {
            IsoService iso = new IsoService(db, new ObjectMapper());
            byte[] png = png(0xff224488);
            var src = new org.springframework.mock.web.MockMultipartFile("image", "n.png", "image/png", png);
            iso.storeScreenshot("art1", "north", -1, src, GranularitySettings.minecraft());
            iso.extrapolate(Map.of("artworkId", "art1", "acceptedExtrapolated",
                    List.of("east", "west", "south", "up", "down")));
            Map<String, Object> joined = iso.join("art1", -1, GranularitySettings.minecraft());
            assertTrue(joined.containsKey("atlasKind"));
            iso.splitTree("art1", -1);
            iso.splitTree("art1", 0);
            Map<String, Object> play = iso.playback("art1", List.of(0), true, "web");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> frames = (List<Map<String, Object>>) play.get("frames");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> swaps = (List<Map<String, Object>>) frames.get(0).get("swaps");
            List<String> nodes = swaps.stream().map(s -> String.valueOf(s.get("nodeId"))).toList();
            assertFalse(nodes.contains("body_leftArm") && nodes.size() == 1 && nodes.get(0).equals("body_leftArm"));
            assertTrue(play.get("displayMode").equals("web"));
        }
    }

    private static byte[] png(int argb) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}

package com.minecraftuuuum.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lemma-build")
public class LemmaBuildController {
    @Value("${minecraftuuuum.lm-studio-base}")
    private String lmStudioBase;

    @Value("${minecraftuuuum.lm-studio-model}")
    private String model;

    private final HttpClient http = HttpClient.newHttpClient();

    @GetMapping("/preface")
    public String preface() throws Exception {
        return new String(new ClassPathResource("data/LemmaBuildEngineMinecraft.md").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            String preface = preface();
            String user = String.valueOf(body.getOrDefault("message", ""));
            String payload = """
                    {"model":"%s","messages":[{"role":"system","content":%s},{"role":"user","content":%s}]}
                    """.formatted(escape(model), jsonStr(preface), jsonStr(user));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(lmStudioBase.replaceAll("/$", "") + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            out.put("ok", res.statusCode() / 100 == 2);
            out.put("status", res.statusCode());
            out.put("body", res.body());
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            out.put("hint", "Start LM Studio OpenAI server at " + lmStudioBase + " with Codestral");
        }
        return out;
    }

    @GetMapping("/models")
    public Map<String, Object> models() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(lmStudioBase.replaceAll("/$", "") + "/models"))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            out.put("status", res.statusCode());
            out.put("body", res.body());
        } catch (Exception e) {
            out.put("error", e.getMessage());
            out.put("models", List.of(Map.of("id", model)));
        }
        return out;
    }

    private static String jsonStr(String s) {
        return "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

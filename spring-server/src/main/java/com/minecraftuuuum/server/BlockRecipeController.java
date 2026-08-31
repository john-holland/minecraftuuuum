package com.minecraftuuuum.server;

import com.minecraftuuuum.lemma.BlockTypeCatalog;
import com.minecraftuuuum.lemma.RecipeCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BlockRecipeController {
    @GetMapping("/api/blocks")
    public Map<String, Object> blocks(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "family", required = false) String family) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("families", BlockTypeCatalog.families());
        out.put("blocks", BlockTypeCatalog.filter(q, family).stream().map(BlockTypeCatalog.BlockType::toMap).toList());
        return out;
    }

    @GetMapping("/api/blocks/{namespace}/{path}")
    public Map<String, Object> one(@PathVariable String namespace, @PathVariable String path) {
        String id = namespace + ":" + path;
        BlockTypeCatalog.BlockType block = BlockTypeCatalog.byId(id);
        if (block == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }
        Map<String, Object> out = new LinkedHashMap<>(block.toMap());
        out.putAll(RecipeCatalog.forItem(id));
        return out;
    }

    @GetMapping("/api/recipes")
    public Map<String, Object> recipes(@RequestParam("item") String item) {
        return RecipeCatalog.forItem(item);
    }

    @PostMapping("/api/recipes")
    public RecipeCatalog.Recipe wrap(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ingredients = (List<String>) body.getOrDefault("ingredients", List.of());
        RecipeCatalog.Recipe r = new RecipeCatalog.Recipe(
                String.valueOf(body.getOrDefault("id", "wrap:" + body.get("result"))),
                String.valueOf(body.getOrDefault("type", "crafting_shapeless")),
                String.valueOf(body.get("result")),
                body.get("count") instanceof Number n ? n.intValue() : 1,
                ingredients);
        RecipeCatalog.add(r);
        return r;
    }
}

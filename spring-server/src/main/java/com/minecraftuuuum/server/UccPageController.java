package com.minecraftuuuum.server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UccPageController {
    @GetMapping({"/", "/library", "/library/"})
    public String library() {
        return "forward:/library/index.html";
    }

    @GetMapping({"/video-generation", "/video-generation/"})
    public String videoGeneration() {
        return "forward:/video-generation/index.html";
    }

    @GetMapping({"/video-animation", "/video-animation/"})
    public String videoAnimation() {
        return "redirect:/video-generation";
    }

    @GetMapping({"/lemma-library", "/lemma-library/"})
    public String lemmaLibrary() {
        return "forward:/lemma-library/index.html";
    }

    @GetMapping({"/lemma-implementation", "/lemma-implementation/"})
    public String lemmaImplementation() {
        return "forward:/lemma-implementation/index.html";
    }

    @GetMapping({"/pixellight", "/pixellight/"})
    public String pixellight() {
        return "forward:/pixellight/index.html";
    }

    @GetMapping({"/block-recipes", "/block-recipes/"})
    public String blockRecipes() {
        return "forward:/block-recipes/index.html";
    }

    @GetMapping({"/cave", "/cave/"})
    public String cave() {
        return "forward:/cave/index.html";
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "minecraftuuuum"
include("lemma-core", "spring-server")
includeBuild("../unimined-craftantic-craftpressor") {
    dependencySubstitution {
        substitute(module("com.unimined:unimined-craftantic-craftpressor")).using(project(":"))
    }
}

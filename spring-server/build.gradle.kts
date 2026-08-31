plugins {
    java
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.6"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":lemma-core"))
    implementation("com.unimined:unimined-craftantic-craftpressor:0.1.0")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("au.com.dius.pact.consumer:junit5:4.6.17")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("minecraftuuuum-server.jar")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("pact.rootDir", rootProject.layout.projectDirectory.dir("pacts").asFile.absolutePath)
    systemProperty("pact.writer.overwrite", "true")
    systemProperty("pact_do_not_track", "true")
}

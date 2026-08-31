plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("exportBuiltins") {
    group = "lemma"
    doLast {
        javaexec {
            classpath = sourceSets.main.get().runtimeClasspath
            mainClass.set("com.minecraftuuuum.lemma.BuiltinVocabularyRegistry")
            args(layout.projectDirectory.file("../spring-server/src/main/resources/data/builtin_vocabulary.json").asFile.absolutePath)
        }
    }
}

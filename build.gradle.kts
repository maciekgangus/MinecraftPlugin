plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.casino"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

val lombokVersion = "1.18.36"
val paperVersion  = "1.21.4-R0.1-SNAPSHOT"

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // bStats metrics — add when ready: implementation("org.bstats:bstats-bukkit:3.0.2")
    // Note: requires shadow plugin with ASM 9.8+ support

    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.45.0")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }

    shadowJar {
        archiveClassifier.set("")
        // relocate("org.bstats", "dev.casino.libs.bstats") — uncomment when bStats added
    }

    jar { enabled = false }
    build { dependsOn(shadowJar) }

    test { useJUnitPlatform() }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}

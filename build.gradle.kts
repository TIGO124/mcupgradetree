// Derleme: ./gradlew build   (Windows: gradlew.bat build)
// Çıktı:   build/libs/UpgradeTree-<sürüm>.jar
// Test sunucusu: ./gradlew runServer  (Paper 26.3'ü indirir ve eklentiyle başlatır)
plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.upgradetree"
version = "2.2.0"

val paperVersion = "26.3.build.242-stable"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("me.clip:placeholderapi:2.12.3")

    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    processResources {
        val props = mapOf("version" to project.version.toString())
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    jar {
        archiveFileName.set("UpgradeTree-${project.version}.jar")
    }
    test {
        useJUnitPlatform()
    }
    runServer {
        minecraftVersion("26.3")
    }
}

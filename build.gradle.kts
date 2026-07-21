plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.novelplugin"
version = "1.2.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC")
    downloadSources.set(false)
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("263.*")
    }
}
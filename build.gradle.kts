import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.opencode.plugin"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        testFramework(TestFrameworkType.Platform)
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "253.*"
        }
    }
}

tasks {
    patchPluginXml {
        changeNotes.set("""
            <h3>1.0.0 - Initial Release</h3>
            <ul>
                <li>Send selected code to OpenCode with file path and line number context</li>
                <li>Keyboard shortcuts: Cmd/Ctrl+Shift+. (with dialog), Cmd/Ctrl+Alt+. (quick send)</li>
                <li>Right-click context menu integration</li>
                <li>Quick templates: Explain, Refactor, Write Tests, Fix Bugs, Optimize, Document, Review</li>
                <li>Dynamic agent selection (auto-discovered from server)</li>
                <li>Multiple session support with session selector</li>
                <li>TUI mode: append code references to terminal prompt</li>
                <li>Auto-submit option for TUI mode</li>
                <li>Server discovery: scan ports 4096-4105 for running instances</li>
                <li>HTTP Basic Auth support</li>
                <li>Configurable settings: server URL, session ID, default agent</li>
            </ul>
        """.trimIndent())
    }
}

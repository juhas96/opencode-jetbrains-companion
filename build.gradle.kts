import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "eu.devmoon"
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
            <h3>1.0.0</h3>
            <p>Initial release of OpenCode Companion by devmoon.</p>
            <ul>
                <li>Send selected code to OpenCode with file path and line number context</li>
                <li>Keyboard shortcuts for quick access</li>
                <li>Right-click context menu integration</li>
                <li>Pre-defined templates: Explain, Refactor, Write Tests, Fix Bugs, Optimize, Document, Review</li>
                <li>Dynamic agent selection from server</li>
                <li>Multiple session support</li>
                <li>TUI mode with optional auto-submit</li>
                <li>Automatic server discovery</li>
                <li>HTTP Basic Auth support</li>
            </ul>
        """.trimIndent())
    }
    
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

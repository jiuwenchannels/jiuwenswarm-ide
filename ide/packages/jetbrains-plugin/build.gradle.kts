plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.jiuwenswarm"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against the locally installed IDE so the plugin matches its platform (build 262).
        local("C:\\Program Files\\JetBrains\\WebStorm 2026.1")
        bundledPlugin("com.intellij.modules.jcef")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }

    // OkHttp for WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.jiuwenswarm.ide-plugin"
        name = "JiuwenSwarm — AI Code Agent"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }

        changeNotes = """
            <b>0.1.0</b> — Initial release (Phase 1)
            <ul>
              <li>Chat panel with streaming responses</li>
              <li>Session management (create / switch)</li>
              <li>Tool call display</li>
              <li>Connection status widget</li>
            </ul>
        """.trimIndent()
    }

    signing {
        // Configure for Marketplace publishing; leave empty for local dev
    }

    publishing {
        // token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
    }
}

tasks {
    wrapper {
        gradleVersion = "9.6.0"
    }
}

// ── Shared webview assets ──────────────────────────────────────────
// shared-webview/ is the single source of truth for the webview html
// and the panel icon. Copy them into the plugin resource tree before
// processResources so the packaged plugin always ships copies in sync
// with the source — no committed duplicates, no drift. The icon is
// renamed to match the JetBrains reference in plugin.xml.
val copySharedWebview by tasks.registering(Copy::class) {
    val src = layout.projectDirectory.dir("../shared-webview")
    onlyIf { src.asFile.exists() }
    from(src) {
        include("chat.html", "swarm_map.html")
    }
    into(layout.projectDirectory.dir("src/main/resources/webview"))
}

val copySharedIcon by tasks.registering(Copy::class) {
    val src = layout.projectDirectory.dir("../shared-webview")
    onlyIf { src.asFile.exists() }
    from(src) {
        include("icon.svg")
    }
    rename { "jiuwenswarm.svg" }
    into(layout.projectDirectory.dir("src/main/resources/icons"))
}

tasks.named("processResources") {
    dependsOn(copySharedWebview, copySharedIcon)
}

tasks.named("processResources") {
    dependsOn(copySharedWebview)
}

tasks.named("instrumentCode") {
    enabled = false
}


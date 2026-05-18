import com.github.gradle.node.npm.task.NpmTask

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("io.ktor.plugin") version "3.5.0"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "dev.fathony.tired"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("dev.fathony.tired.MainKt")
    // Netty uses native libraries (e.g. epoll, SSL) loaded via System::loadLibrary.
    // JVM 25 restricts this by default — without this flag it warns and will eventually block.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_25)
    }
}

repositories {
    mavenCentral()
}

node {
    download = true
    version = "24.15.0"
    nodeProjectDir = file("${project.projectDir}/web-assets")
}

val npmBuildCss = tasks.register<NpmTask>("npmBuildCss") {
    description = "Build CSS stylesheet"

    dependsOn(tasks.named("npmInstall"))
    args = listOf("run", "build:css", "--", "--minify")

    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.dir("${project.projectDir}/src/main/kotlin")
    inputs.file("${project.projectDir}/web-assets/postcss.config.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/dist/stylesheets")
}

val generatedDir = project.layout.buildDirectory.dir("generated/kotlin")

val copyIcons = tasks.register<Copy>("copyIcons") {
    description = "Copy Icons.kt from buildSrc to generated sources"
    from("${project.projectDir}/buildSrc/src/main/kotlin/Icons.kt")
    into(generatedDir)
}

sourceSets["main"].kotlin.srcDir(generatedDir)

tasks.named("compileKotlin") {
    dependsOn(copyIcons)
}

val npmBuildJs = tasks.register<NpmTask>("npmBuildJs") {
    description = "Build JavaScript bundle"

    dependsOn(tasks.named("npmInstall"))
    args = listOf("run", "build:js", "--", "--minify")

    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.file("${project.projectDir}/web-assets/scripts/build-js.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/dist/scripts")
}

val npmBuildSvg = tasks.register<NpmTask>("npmBuildSvg") {
    description = "Build SVG spritesheet from Lucide icons"

    dependsOn(tasks.named("npmInstall"))

    val icons = Icons.entries.joinToString(",") { it.lucideIcon }
    args = listOf("run", "build:svg", "--", icons)

    inputs.file("${project.projectDir}/web-assets/scripts/build-icons.js")
    inputs.dir("${project.projectDir}/web-assets/node_modules/lucide-static/icons")
    outputs.dir("${project.projectDir}/web-assets/dist/icons")
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(npmBuildCss, npmBuildJs, npmBuildSvg)
    from("${project.projectDir}/web-assets/dist/stylesheets") {
        into("static")
    }
    from("${project.projectDir}/web-assets/dist/icons") {
        into("static")
    }
    from("${project.projectDir}/web-assets/dist/scripts") {
        into("static")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-htmx")
    implementation("io.ktor:ktor-htmx-html")
    implementation("io.ktor:ktor-server-htmx")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("com.googlecode.htmlcompressor:htmlcompressor:1.5.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
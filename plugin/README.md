# tired-gradle-plugin

Gradle plugin for the Tired Stack: a Ktor app with KTML templates and bundled web assets. Icons are opt-in, and CSS
tooling such as Tailwind is added by the app through PostCSS.

```kotlin
plugins {
    id("dev.fathony.tired") version "0.1.0"        // Ktor app + KTML + web assets
    id("dev.fathony.tired.icons") version "0.1.0"  // optional: Lucide icons
}
```

Releases are published to [maven.fathony.dev](https://maven.fathony.dev). The plugin comes from there, and it
declares the repository itself for `tired-library`, which it adds to the app:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.fathony.dev/releases")
        gradlePluginPortal()
    }
}
```

| Plugin | Applied by `dev.fathony.tired` | What it sets up |
|---|---|---|
| `dev.fathony.tired.kotlin` | yes | Kotlin JVM 25, JUnit with kotlin-test, ktlint, `format` |
| `dev.fathony.tired.ktor-app` | yes | Ktor with Docker defaults and serialization; adds `ktor-server-core`, `ktor-server-netty` and `logback-classic` |
| `dev.fathony.tired.ktml` | yes | KTML templates from `src/main/ktml`, hot reloaded under `run`; adds tired-library (`installTired()`, `KtmlView`, `respondView`, `page`, `sendView`) |
| `dev.fathony.tired.web-assets` | yes | Bundles the script and stylesheet into hashed files with esbuild, exposed as `AssetManifest` |
| `dev.fathony.tired.icons` | no | Lucide icons: one SVG sprite, the `Icons` enum and, with KTML, the `<icon>` tag |

The app sets its main class and its own feature libraries, such as `ktor-server-resources` or `ktor-server-sse`:

```kotlin
application {
    mainClass = "com.example.MainKt"
}
```

## Web assets

```kotlin
webAssets {
    sourceDir = layout.projectDirectory.dir("src/main/web")  // default
    stylesheet = "stylesheet.css"                             // default, relative to sourceDir
    script = "index.js"                                       // default, relative to sourceDir

    npm("htmx.org", "2.0.11")  // packages imported from sourceDir, exact versions
}
```

A missing entry is skipped. The built files are exposed as `AssetManifest.stylesheet_css` and
`AssetManifest.index_js`, and `installTired()` serves them:

```kotlin
embeddedServer(Netty, port = 3000) {
    installTired()
    routing { /* ... */ }
}
```

npm packages can be imported from both entries, e.g. `@import "some-package";` in CSS or `import "htmx.org";` in JS.
`./gradlew npmLatest -Ppackage=<name>` prints a package's latest version.

Under `./gradlew run`, the stylesheet and script are rebuilt whenever a source changes and served uncached, so a
browser refresh shows the change without a restart.

## Icons

```kotlin
icons {
    add("search")                          // Icons.Search
    add("chevron-down", alias = "Chevron") // Icons.Chevron
}
```

The sprite is exposed as `AssetManifest.icons_svg`. With KTML, templates can use the generated `<icon>` tag:

```html
<icon name="Icons.Search" class="size-4"/>
<icon name="${view.icon}"/>
```

## PostCSS

The stylesheet can go through PostCSS plugins before esbuild bundles it. They're npm packages the app adds, so the app
owns their versions. Tailwind v4:

```kotlin
webAssets {
    npm("tailwindcss", "4.3.3")
    npm("@tailwindcss/postcss", "4.3.3")
    postcss("@tailwindcss/postcss")  // plugins run in order; options: postcss("name", mapOf(...))
}
```

```css
/* src/main/web/stylesheet.css */
@import "tailwindcss" source(none);

@source "../kotlin";
@source "../ktml";
@source "./";
```

With PostCSS, `src/main/kotlin` and `src/main/ktml` are reachable from the stylesheet at their usual relative paths.
For an `@source` pointing anywhere else, add that folder:

```kotlin
webAssets {
    mirror("src/main/resources/templates")
}
```

## Toolchain

The build tools and their lockfile ship in `src/main/resources/dev/fathony/tired/toolchain`. To update them, edit
its `package.json` and refresh the lockfile:

```bash
./gradlew -p plugin npmInstall
```

## Development

This plugin and `library/` are included builds of this repo, so the app always builds against their current source.
To publish the plugin locally:

```bash
./gradlew -p plugin publishToMavenLocal
```

To release, run the [Bump Version](../.github/workflows/bump.yml) workflow and pick `patch`, `minor` or `major`:

```bash
gh workflow run bump.yml -f bump=minor                         # from main
gh workflow run bump.yml -f bump=patch --ref release/1.x      # patch an older major
```

It takes the latest `v*` tag on the branch it runs on, runs the checks, publishes the plugin and `tired-library` at
the next version, then creates the tag and a GitHub Release. Run it on `main` for new releases, or on a
`release/<major>.x` branch to patch an older major. A version can only be published once.

# tired-gradle-plugin

Gradle plugin for the Tired Stack: a Ktor app with KTML templates and bundled web assets. The base is bare on
purpose; icons are an opt-in plugin, and CSS tooling such as Tailwind is added by the app through PostCSS.

```kotlin
plugins {
    id("dev.fathony.tired")            // Ktor app + KTML + web assets
    id("dev.fathony.tired.icons")      // optional: Lucide icons
}
```

| Plugin | Applied by `dev.fathony.tired` | What it sets up |
|---|---|---|
| `dev.fathony.tired.kotlin` | yes (via `ktor-app`) | Kotlin JVM 25, JUnit with kotlin-test, ktlint, `format`; also used on its own by library modules |
| `dev.fathony.tired.ktor-app` | yes | Serialization, Ktor (with Docker defaults), `setupGitHooks`; adds `ktor-server-core`, `ktor-server-netty` and `logback-classic` |
| `dev.fathony.tired.ktml` | yes | KTML templates from `src/main/ktml`; dev mode (hot reload) on `run` only, never in the jar; adds `dev.fathony.tired:tired-library` (`installTired()` and typed views: `KtmlView`, `respondView`, `page`, `sendView`) |
| `dev.fathony.tired.web-assets` | yes | esbuild bundles the script and stylesheet entries (npm packages included) into hashed files, optionally through PostCSS plugins; `AssetManifest` |
| `dev.fathony.tired.icons` | no | Lucide icons: one SVG sprite, the `Icons` enum and, with KTML, the `<icon>` tag |

The app still sets what's specific to it, including feature libraries such as `ktor-server-resources`,
`ktor-server-sse`, `ktor-server-htmx` or `kotlinx-serialization-json`:

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

Each entry is optional: a missing one is skipped. The built file names are exposed as `AssetManifest.stylesheet_css`
and `AssetManifest.index_js` (named after the entries) and served from the root of the app's static resources by
`installTired()` from tired-library, which also installs KTML, typed routes and Server-Sent Events:

```kotlin
embeddedServer(Netty, port = 3000) {
    installTired()
    routing { /* ... */ }
}
```

### Under `./gradlew run`

Assets are built unminified, with stable names (`stylesheet.css` instead of `stylesheet-<hash>.css`), and a watcher
runs next to the app: when a web source, or a mirrored folder such as `src/main/ktml`, changes, it rebuilds the
stylesheet (and the script, if one changed) into the served folder. A browser refresh shows the change; there's no
need to restart for new Tailwind classes. The watcher stops with `run`.

`run` also sets `-Dtired.dev=true` (`isTiredDev` in tired-library), which the asset route uses to send
`Cache-Control: no-cache`, so a refresh never shows a stale file. Production names are content-hashed and keep the
default caching.

Imports of npm packages work from both entries: `@import "basecoat-css";` in CSS, `import "htmx.org";` in JS.
`./gradlew npmLatest -Ppackage=<name>` prints a package's latest version.

## Icons

```kotlin
icons {
    add("search")                          // Icons.Search
    add("chevron-down", alias = "Chevron") // Icons.Chevron
}
```

Icons are bundled into one sprite, exposed as `AssetManifest.icons_svg`. With KTML applied, the plugin also writes
the `<icon>` tag to `src/main/ktml/tired/icon.ktml` (KTML only reads templates from there). That folder ignores
itself in git, so templates can use it right away:

```html
<icon name="Icons.Search" class="size-4"/>
<icon name="${view.icon}"/>
```

## PostCSS (Tailwind, Basecoat, ...)

The stylesheet can go through PostCSS plugins before esbuild bundles it. The plugins are ordinary npm packages the
app adds, so the app owns their versions; the tired plugin only runs them. Tailwind v4, with Basecoat on top:

```kotlin
webAssets {
    npm("tailwindcss", "4.3.3")
    npm("@tailwindcss/postcss", "4.3.3")
    npm("basecoat-css", "1.0.2")
    postcss("@tailwindcss/postcss")  // plugins run in order; options: postcss("name", mapOf(...))
}
```

```css
/* src/main/web/stylesheet.css */
@import "tailwindcss" source(none);
@import "basecoat-css";

@source "../kotlin";
@source "../ktml";
@source "./";
```

```js
// src/main/web/index.js, only for Basecoat's interactive components
import "basecoat-css/all";
```

The web sources are built from a copy under `build/tired/web-assets/sources`, which mirrors the project layout. With
any PostCSS plugin, `src/main/kotlin` and `src/main/ktml` are copied there too, so paths relative to the stylesheet,
like Tailwind's `@source "../kotlin"`, resolve the same as in the source tree. The plugin doesn't read the CSS: an
`@source` pointing anywhere else needs that folder copied as well, or Tailwind silently finds nothing there:

```kotlin
webAssets {
    mirror("src/main/resources/templates")  // added to the defaults
    // mirroredDirs.set(listOf(...))         // or replace them
}
```

## Toolchain

The build tools (esbuild, and PostCSS and Lucide when used), their lockfile and the build scripts ship in
this plugin under `src/main/resources/dev/fathony/tired/toolchain`. At build time they are extracted to the app's
`build/tired/web-assets` with only the tools that are needed, together with the app's `npm(...)` packages and a
copy of its sources.

To update the toolchain, edit its `package.json` (or let Renovate) and refresh the lockfile:

```bash
./gradlew -p plugin npmInstall
```

## Formatting

The plugin doesn't format an app's JS and CSS; apps set up their own formatter. The toolchain lists Prettier only for
this repo, whose root build formats `app/src/main/web` and the toolchain's scripts with it (`./gradlew format`), and
it's never installed for apps.

## Development

This plugin is an included build of the Tired Stack repo (`pluginManagement { includeBuild("plugin") }`), so the
app always builds against its current source. So is `library/` (`includeBuild("library")`): Gradle substitutes it
for the `dev.fathony.tired:tired-library` dependency the plugin adds. Both share the repo's `gradle/libs.versions.toml`,
including the `tired` version they're published under.

It can still be published on its own when other projects need it, e.g. from the repo root:

```bash
./gradlew -p plugin publishToMavenLocal
```

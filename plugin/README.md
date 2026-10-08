# tired-gradle-plugin

Gradle plugin for the Tired Stack: a Ktor app with KTML templates and bundled web assets. Icons and native images are
opt-in, and you add CSS tooling such as Tailwind yourself, through PostCSS.

```kotlin
plugins {
    id("dev.fathony.tired") version "0.1.0"        // Ktor app + KTML + web assets
    id("dev.fathony.tired.icons") version "0.1.0"  // optional: Lucide icons
    id("dev.fathony.tired.native") version "0.1.0" // optional: GraalVM native image
}
```

The plugin is published to [maven.fathony.dev](https://maven.fathony.dev). Add that repository once, and the plugin
fetches its runtime library, `tired-library`, from it for you:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.fathony.dev/releases")
        gradlePluginPortal()
    }
}
```

| Plugin | Applied by `dev.fathony.tired` | What it sets up | Tasks it adds |
|---|---|---|---|
| `dev.fathony.tired.kotlin` | yes | Kotlin on JDK 25, JUnit with kotlin-test, and ktlint | `format` |
| `dev.fathony.tired.ktor-app` | yes | Ktor with serialization, Netty, logback and Docker defaults | `smokeTest`, `setupGitHooks` |
| `dev.fathony.tired.ktml` | yes | KTML templates from `src/main/ktml`, reloaded while `run` is going, and tired-library's helpers: `installTired()`, `KtmlView`, `respondView`, `page`, `sendView` | |
| `dev.fathony.tired.web-assets` | yes | Your script and stylesheet, bundled into hashed files with esbuild and exposed as `AssetManifest` | `npmLatest` |
| `dev.fathony.tired.icons` | no | Lucide icons: one SVG sprite, the `Icons` enum and, with KTML, the `<icon>` tag | |
| `dev.fathony.tired.native` | no | The app shipped as a GraalVM native image | `nativeCompile`, `nativeSmokeTest` |

You set the main class and add the Ktor features you use, such as `ktor-server-resources` or `ktor-server-sse`:

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

If you have no stylesheet or no script, that step is skipped. The built files are available as
`AssetManifest.stylesheet_css` and `AssetManifest.index_js`, and `installTired()` serves them:

```kotlin
embeddedServer(Netty, port = 3000) {
    installTired()
    routing { /* ... */ }
}
```

You can import npm packages from both files, e.g. `@import "some-package";` in CSS or `import "htmx.org";` in JS.
`./gradlew npmLatest -Ppackage=<name>` prints a package's latest version.

While `./gradlew run` is going, the stylesheet and script are rebuilt whenever you save and served uncached, so a
browser refresh shows the change.

## PostCSS

Your stylesheet can go through PostCSS plugins before esbuild bundles it. They're npm packages you add, so you pick
their versions. For Tailwind v4:

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

With PostCSS, the stylesheet can reach `src/main/kotlin` and `src/main/ktml` at their usual relative paths. To point
an `@source` anywhere else, add that folder:

```kotlin
webAssets {
    mirror("src/main/resources/templates")
}
```

## Icons

Every [Lucide](https://lucide.dev/icons) icon is an `Icons` constant, named in PascalCase: `search` is `Icons.Search`
and `shopping-cart` is `Icons.ShoppingCart`. There's nothing to declare. The sprite is exposed as
`AssetManifest.icons_svg`. With KTML, templates can use the generated `<icon>` tag:

```html
<icon name="Icons.Search" class="size-4"/>
<icon name="${view.icon}"/>
```

While `./gradlew run` is going, the sprite holds every icon, so a new one shows up on refresh. Other builds only keep
the icons that `src/main/kotlin` and `src/main/ktml` mention as `Icons.Name`. An icon you reach any other way, such as
a star import or `Icons.valueOf`, works while developing but is missing from the built sprite.

## Native image

With `dev.fathony.tired.native`, the app ships as a GraalVM native image in place of the JVM build.

| Command | What it does |
|---|---|
| `./gradlew nativeCompile` | Builds the executable at `build/native/nativeCompile/<project name>` |
| `./gradlew nativeSmokeTest` | Gives the executable the [same test](#smoke-test) as the JVM build |
| `./gradlew publishImage`, `buildImage`, `runDocker`, ... | Same tasks and `ktor.docker` settings, now packaging the executable |

Pros:

- Starts instantly
- Much less memory
- Small image, with no JRE or shell

Cons:

- Slower builds
- The executable only runs on the system that built it
- Reflection and resources have to be declared

Requirements:

- A C compiler and zlib
- Linux, to build the image

There's nothing else to install: Gradle downloads GraalVM.

The base image is `gcr.io/distroless/java-base-debian12`; `ktor.docker.customBaseImage` changes it.

The heap is capped at 35% of the container's memory, because the executable needs room next to it. The root README
has the [limits and how to change them](../README.md#memory).

The JVM image is still there: `-Ptired.image=jvm` makes the image tasks build it, tagged `jvm`.

```bash
./gradlew publishImage -Ptired.image=jvm
```

You don't declare anything for the stack itself: tired-library covers its templates, assets, icons and logging. A
library you add works as it is when it ships its own configuration or is in
[GraalVM's metadata repository](https://github.com/oracle/graalvm-reachability-metadata). When one doesn't:

1. Run `./gradlew nativeSmokeTest` to see what fails.
2. Declare what the library needs in a
   [`reachability-metadata.json`](https://www.graalvm.org/latest/reference-manual/native-image/metadata/) under
   `src/main/resources/META-INF/native-image/<group>/<name>/`.
3. Run the smoke test again.

## Smoke test

`./gradlew smokeTest` starts the built app and requests pages from it, along with the stylesheets, scripts and icons
they link to. It fails when the app doesn't start or anything answers with an error:

```kotlin
smokeTest {
    port = 3000                         // default
    paths = listOf("/", "/contacts")    // default: "/"
}
```

It isn't part of `check`, because it needs the port free. Run it in CI.

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

### Build tools

esbuild, PostCSS, Prettier and the Lucide icons ship with their lockfile in
`src/main/resources/dev/fathony/tired/toolchain`. To update them, edit its `package.json` and refresh the lockfile:

```bash
./gradlew -p plugin npmInstall
```

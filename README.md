# Tired Stack

A server-rendered web stack for people who are tired: Kotlin on the server, HTML that compiles, and just enough
JavaScript to be dangerous.

- **[Ktor](https://ktor.io)** serves the app: coroutines, typed routes, no Spring.
- **[KTML](https://github.com/ktool-dev/ktml)** templates are plain HTML that compile to Kotlin functions, so a typo
  in a template fails the build instead of the page.
- **[HTMX](https://htmx.org)** swaps server-rendered fragments into the page, including over Server-Sent Events.
- **[Tailwind CSS v4](https://tailwindcss.com)** styles it, picking up classes from Kotlin and templates alike.
- **[Lucide](https://lucide.dev)** icons are exposed as a type-safe `Icons` enum, and the SVG sprite only keeps the ones you use.
- **Gradle** runs everything, including Node.js: there is no `npm install` and no `package.json` to maintain.

## Try the Sample

You need a JDK (17 or newer) to run Gradle. The build downloads JDK 25 and Node.js itself.

```bash
git clone https://github.com/fathonyfath/tired-stack.git
cd tired-stack
./gradlew run
```

Open http://localhost:3000. While `run` is going, templates, styles (including new Tailwind classes) and scripts
are rebuilt on save: refresh the browser to see them. Changes to Kotlin code need a restart.

## Installation

Releases of the plugin are published to [maven.fathony.dev](https://maven.fathony.dev). Add the repository to
`pluginManagement` in `settings.gradle.kts`; the plugin declares it for `tired-library`, which it adds to the app:

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.fathony.dev/releases")
        gradlePluginPortal()
    }
}
```

Then apply it in the app's `build.gradle.kts`:

```kotlin
plugins {
    id("dev.fathony.tired") version "0.1.0"
    id("dev.fathony.tired.icons") version "0.1.0"   // optional
    id("dev.fathony.tired.native") version "0.1.0"  // optional
}

application {
    mainClass = "com.example.MainKt"
}
```

Templates go in `src/main/ktml`, and the script and stylesheet in `src/main/web`. Call `installTired()` in your Ktor
module. The [plugin README](plugin/README.md) covers the options.

## Project Layout

```
app/                        The web app
├── build.gradle.kts        Plugins, npm packages, dependencies
└── src/main/
    ├── kotlin/             Main.kt and one file per feature: route, view classes, handlers
    ├── ktml/               Templates: pages/, fragments/, layouts/
    ├── web/                index.js and stylesheet.css
    └── resources/          logback.xml
library/                    Runtime helpers the plugin adds to the app: typed KTML views for Ktor
plugin/                     The tired Gradle plugin, built from source as part of the build
gradle/libs.versions.toml   Versions
```

All the build logic lives in [`plugin/`](plugin/README.md), so the app's build file only says what the app uses.

## How a Page Works

Each feature lives in one Kotlin file: a typed route, a view class holding what the template needs, and the route
wiring. From [`features/Home.kt`](app/src/main/kotlin/dev/fathony/tired/features/Home.kt):

```kotlin
@Resource("/")
class Home

data class HomePage(val title: String, val icon: Icons) : KtmlView {
    override val template = "pages/home"
}

fun Route.home() = page<Home> { HomePage(title = "Hello world!", icon = Icons.Search) }
```

The template declares the view it expects, so everything it reads is checked by the compiler:

```html
<!-- ktml/pages/home.ktml, shortened -->
import dev.fathony.tired.features.HomePage

<html lang="en" @view="$HomePage">
<app-layout title="${view.title}">
    <h1 class="text-2xl font-bold"><icon name="${view.icon}"/> ${view.title}</h1>
</app-layout>
</html>
```

`<app-layout>` is a custom tag from [`layouts/app-layout.ktml`](app/src/main/ktml/layouts/app-layout.ktml); any
template can be a tag.

[`Main.kt`](app/src/main/kotlin/dev/fathony/tired/Main.kt) calls `installTired()` once to set up KTML and serve the
web assets, and installs `Resources` and `SSE` for the helpers that need them. These helpers connect views to Ktor:

| Helper | Use |
|---|---|
| `page<Route> { view }` | A route that only renders a view; needs `Resources` |
| `call.respondView(view)` | Render a page or fragment from any handler, e.g. an HTMX request |
| `sendView(event, view)` | Send a rendered fragment as a Server-Sent Event; needs `SSE` |

The [HTMX demo](app/src/main/kotlin/dev/fathony/tired/features/HtmxDemo.kt) and
[SSE demo](app/src/main/kotlin/dev/fathony/tired/features/SseDemo.kt) show the fragment side.

## Styles, Scripts and Icons

`app/src/main/web/index.js` and `stylesheet.css` are bundled by esbuild into hashed files, referenced from templates
through the generated `AssetManifest`. npm packages and Tailwind (as a PostCSS plugin) are declared in
[`app/build.gradle.kts`](app/build.gradle.kts); the [plugin README](plugin/README.md) has the full set of options.
Every Lucide icon is available as `Icons.Name`, and the built sprite only keeps the ones you use.

## Commands

| Command | What it does |
|---|---|
| `./gradlew run` | Run the app on port 3000; templates, CSS and JS rebuild on save (refresh to see them) |
| `./gradlew check` | ktlint, Prettier and all tests |
| `./gradlew format` | Format Kotlin, JavaScript and CSS |
| `./gradlew setupGitHooks` | Enable the pre-commit hook, which lints staged files |
| `./gradlew buildFatJar` | Build `app/build/libs/app-all.jar` (`java -jar app/build/libs/app-all.jar`) |
| `./gradlew nativeCompile` | Build a GraalVM native executable at `app/build/native/nativeCompile/app` |
| `./gradlew smokeTest` | Start the built app and request a page of every feature from it |
| `./gradlew nativeSmokeTest` | The same, against the native executable |
| `./gradlew runDocker` | Build the Docker image and run it locally (needs Docker and Linux) |
| `./gradlew publishImage` | Push the image to `ghcr.io/fathonyfath/tired-stack-sample` (needs `GHCR_USERNAME`, `GHCR_TOKEN`) |

## Deployment

Every push to `main` is checked and the sample is published to GHCR ([`publish.yml`](.github/workflows/publish.yml)):
the native image as `latest` and `sha-<commit>`, the JVM image as `jvm` and `jvm-sha-<commit>`. To run it:

```bash
docker compose up -d
```

### Memory

Both images size their heap from the container's memory limit, so always set `mem_limit`.

| Image | `mem_limit` | Peak | Settings |
|---|---|---|---|
| Native (`latest`) | `64m` | 51 MiB | Defaults |
| JVM (`jvm`) | `256m` | 199 MiB | Defaults |
| JVM (`jvm`) | `256m` | 176 MiB | Tuned, below |

Peaks are from one CPU at 1,000 requests a second, 20% of them writes.

#### Native image

```yaml
services:
  app:
    image: ghcr.io/fathonyfath/tired-stack-sample:latest
    mem_limit: 64m
    cpus: 1
```

The heap defaults to 35% of `mem_limit`; the executable needs the rest. To change it, pass an argument:

```yaml
    command: ["-Xmx24m"]
```

| Argument | Heap |
|---|---|
| none | 35% of `mem_limit` |
| `-Xmx<size>` | A fixed maximum, e.g. `-Xmx24m` |
| `-XX:MaximumHeapSizePercent=<n>` | Another share of `mem_limit` |

#### JVM image

The image's default, which holds from `256m` up:

```
JAVA_TOOL_OPTIONS=--enable-native-access=ALL-UNNAMED -XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport
```

To use less memory, replace it and cap each part of the JVM, since the heap is only one of them:

```yaml
services:
  app:
    image: ghcr.io/fathonyfath/tired-stack-sample:jvm
    mem_limit: 256m
    cpus: 1
    environment:
      MALLOC_ARENA_MAX: "2"
      JAVA_TOOL_OPTIONS: >-
        --enable-native-access=ALL-UNNAMED
        -XX:+UseSerialGC -Xmx96m
        -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=24m -XX:MaxMetaspaceSize=64m
        -XX:MaxDirectMemorySize=16m -Dio.netty.allocator.numDirectArenas=1 -Dio.netty.allocator.numHeapArenas=1
        -Xss256k -Dkotlinx.coroutines.io.parallelism=8
        -XX:TrimNativeHeapInterval=5000
        -XX:+ExitOnOutOfMemoryError
```

| Settings | What they cap |
|---|---|
| `-XX:+UseSerialGC`, `-Xmx96m` | The heap, with the smallest collector |
| `-XX:TieredStopAtLevel=1`, `-XX:ReservedCodeCacheSize`, `-XX:MaxMetaspaceSize` | Compiled code and class metadata |
| `-XX:MaxDirectMemorySize`, `-Dio.netty.allocator.num*Arenas` | Netty's buffers outside the heap |
| `-Xss256k`, `-Dkotlinx.coroutines.io.parallelism` | Thread stacks, and how many threads `Dispatchers.IO` starts |
| `MALLOC_ARENA_MAX`, `-XX:TrimNativeHeapInterval` | Native memory that glibc holds on to |
| `-XX:+ExitOnOutOfMemoryError` | Nothing: the app exits when memory runs out, so the container restarts it |

Releases of the plugin and library are cut with the [Bump Version](.github/workflows/bump.yml) workflow; see the
[plugin README](plugin/README.md#development).

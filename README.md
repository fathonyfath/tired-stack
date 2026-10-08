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

## Try the Sample

You need a JDK (17 or newer) to run Gradle. The build downloads JDK 25 and Node.js itself.

```bash
git clone https://github.com/fathonyfath/tired-stack.git
cd tired-stack
./gradlew run
```

Open http://localhost:3000. While `run` is going, templates, styles (including new Tailwind classes) and scripts
are rebuilt on save: refresh the browser to see them. Changes to Kotlin code need a restart.

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

Every push to `main` is checked and the sample is published to GHCR, tagged `latest` and `sha-<commit>`, with its JVM
build as `jvm` and `jvm-sha-<commit>` ([`publish.yml`](.github/workflows/publish.yml)). To run it:

```bash
docker compose up -d
```

The sample ships as a GraalVM native image, through the `dev.fathony.tired.native` plugin: the image holds one
executable on a distroless base. The JVM image is published next to it, tagged `jvm`; see the
[plugin README](plugin/README.md#native-image).

### The JVM image in little memory

The JVM image's defaults (ZGC, a heap of 75% of the container) suit a roomy host. On a small one, cap each part of
the JVM's memory, since the heap is only one of them. With the settings below, the sample served 1,000 requests a
second, a fifth of them writes, for ten minutes on one CPU in 256 MB, peaking at 176 MiB; at 128 MB it was killed.

```yaml
services:
  app:
    image: ghcr.io/fathonyfath/tired-stack-sample:jvm
    ports:
      - "3000:3000"
    mem_limit: 256m
    cpus: 1
    restart: unless-stopped
    environment:
      MALLOC_ARENA_MAX: "2"
      JAVA_TOOL_OPTIONS: >-
        --enable-native-access=ALL-UNNAMED
        -XX:+UseSerialGC
        -XX:+UseContainerSupport
        -Xmx96m
        -XX:TieredStopAtLevel=1
        -XX:ReservedCodeCacheSize=24m
        -XX:MaxMetaspaceSize=64m
        -XX:MaxDirectMemorySize=16m
        -Dio.netty.allocator.numDirectArenas=1
        -Dio.netty.allocator.numHeapArenas=1
        -Xss256k
        -Dkotlinx.coroutines.io.parallelism=8
        -XX:TrimNativeHeapInterval=5000
        -XX:+ExitOnOutOfMemoryError
```

Setting `JAVA_TOOL_OPTIONS` replaces the image's own, so it repeats the flags worth keeping.

| Setting | What it limits |
|---|---|
| `MALLOC_ARENA_MAX=2` | The memory pools glibc keeps per thread for native allocations |
| `--enable-native-access=ALL-UNNAMED` | Nothing: Netty and sqlite-jdbc load native code, which JDK 25 warns about without it |
| `-XX:+UseSerialGC` | The collector's own memory; it is the smallest, and enough for one CPU |
| `-XX:+UseContainerSupport` | Nothing: the JVM reads the container's limits, which is already the default |
| `-Xmx96m` | The heap |
| `-XX:TieredStopAtLevel=1` | The compiler: only the quick one runs, for less compiled code and compiler memory, and lower peak speed |
| `-XX:ReservedCodeCacheSize=24m` | Compiled code |
| `-XX:MaxMetaspaceSize=64m` | Class metadata |
| `-XX:MaxDirectMemorySize=16m` | Buffers outside the heap, which Netty uses for I/O |
| `-Dio.netty.allocator.num{Direct,Heap}Arenas=1` | Netty's buffer pools, otherwise two per core |
| `-Xss256k` | Each thread's stack |
| `-Dkotlinx.coroutines.io.parallelism=8` | The threads of `Dispatchers.IO`, otherwise up to 64 |
| `-XX:TrimNativeHeapInterval=5000` | Freed native memory that glibc holds on to: it is handed back every five seconds |
| `-XX:+ExitOnOutOfMemoryError` | Nothing: the app exits when it runs out, so the container restarts it |

The heap, metaspace, code cache and direct memory add up to 200 MB; threads and the JVM itself take the rest.

Releases of the plugin and library are cut with the [Bump Version](.github/workflows/bump.yml) workflow; see the
[plugin README](plugin/README.md#development).

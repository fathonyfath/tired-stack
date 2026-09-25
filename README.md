# Tired Stack

A server-rendered web stack for people who are tired: Kotlin on the server, HTML that compiles, and just enough
JavaScript to be dangerous.

- **[Ktor](https://ktor.io)** serves the app: coroutines, typed routes, no Spring.
- **[KTML](https://github.com/ktool-dev/ktml)** templates are plain HTML that compile to Kotlin functions, so a typo
  in a template fails the build instead of the page.
- **[HTMX](https://htmx.org)** swaps server-rendered fragments into the page, including over Server-Sent Events.
- **[Tailwind CSS v4](https://tailwindcss.com)** styles it, picking up classes from Kotlin and templates alike.
- **[Lucide](https://lucide.dev)** icons are bundled into one SVG sprite and exposed as a type-safe `Icons` enum.
- **Gradle** runs everything, including Node.js: there is no `npm install` and no `package.json` to maintain.

## Getting Started

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
├── build.gradle.kts        Plugins, npm packages, icons, dependencies
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

[`Main.kt`](app/src/main/kotlin/dev/fathony/tired/Main.kt) calls `installTired()` once to set everything up. These
helpers connect views to Ktor:

| Helper | Use |
|---|---|
| `page<Route> { view }` | A route that only renders a view |
| `call.respondView(view)` | Render a page or fragment from any handler, e.g. an HTMX request |
| `sendView(event, view)` | Send a rendered fragment as a Server-Sent Event |

The [HTMX demo](app/src/main/kotlin/dev/fathony/tired/features/HtmxDemo.kt) and
[SSE demo](app/src/main/kotlin/dev/fathony/tired/features/SseDemo.kt) show the fragment side.

## Styles, Scripts and Icons

`app/src/main/web/index.js` and `stylesheet.css` are bundled by esbuild into hashed files. Templates reference them
through the generated `AssetManifest` (`AssetManifest.index_js`, `AssetManifest.stylesheet_css`).

npm packages are declared in `app/build.gradle.kts` with exact versions, then imported as usual:

```kotlin
webAssets {
    npm("htmx.org", "2.0.11")
}
```

```bash
./gradlew npmLatest -Ppackage=htmx.org   # look up the latest version
```

Tailwind runs as a PostCSS plugin (`postcss("@tailwindcss/postcss")`); its sources are listed at the top of
[`stylesheet.css`](app/src/main/web/stylesheet.css).

Icons are registered by their [Lucide](https://lucide.dev/icons) name:

```kotlin
icons {
    add("search")                           // Icons.Search
    add("chevron-down", alias = "Chevron")  // Icons.Chevron
}
```

```html
<icon name="Icons.Search" class="size-4"/>
```

The [plugin README](plugin/README.md) has the full set of options.

## Commands

| Command | What it does |
|---|---|
| `./gradlew run` | Run the app on port 3000; templates, CSS and JS rebuild on save (refresh to see them) |
| `./gradlew check` | ktlint, Prettier and all tests |
| `./gradlew format` | Format Kotlin, JavaScript and CSS |
| `./gradlew test` | Run the tests of the app, library and plugin |
| `./gradlew setupGitHooks` | Enable the pre-commit hook, which lints staged files |
| `./gradlew buildFatJar` | Build `app/build/libs/app-all.jar` (`java -jar app/build/libs/app-all.jar`) |
| `./gradlew runDocker` | Build the Docker image and run it locally (needs Docker) |
| `./gradlew publishImage` | Push the image to `ghcr.io/fathonyfath/tired-stack-sample` (needs `GHCR_USERNAME`, `GHCR_TOKEN`) |

## Deployment

Every push to `main` is checked and published to GHCR by GitHub Actions
([`lint.yml`](.github/workflows/lint.yml), [`publish.yml`](.github/workflows/publish.yml)), tagged `latest` and
`sha-<commit>`. A weekly [`cleanup.yml`](.github/workflows/cleanup.yml) deletes untagged images and keeps the 10
newest tagged ones. To run the published image:

```bash
docker compose up -d
```

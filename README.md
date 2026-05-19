# Tired Stack

This project is built with the Tired Stack — a custom stack for web development. The previous version used TypeScript, which was abandoned because life is too short for JavaScript. This one uses Kotlin.

## Stack Overview

The Tired Stack (Kotlin edition) is comprised of the following key technologies:

- **Kotlin**: A language that compiles to JVM bytecode and actually has a type system that works. No `undefined is not a function` here.
- **Ktor**: A Kotlin-native async web framework for building the server. Lightweight, coroutine-based, and not Spring.
- **kotlinx-html**: Type-safe HTML templating in Kotlin. Your templates are just Kotlin code — the compiler catches your typos before your users do.
- **HTMX**: Gives your server-rendered HTML just enough JavaScript to be dangerous, without you having to write any JavaScript.
- **Tailwind CSS v4**: A utility-first CSS framework. Still PostCSS under the hood.
- **Prettier**: Keeps the JavaScript and CSS in the `web-assets` directory formatted, since those files can't format themselves.
- **Lucide Icons**: SVG icon set, bundled into a sprite at build time so you're not making 47 HTTP requests for icons.
- **Gradle**: Build tool that manages everything, including downloading Node.js so you don't have to have it installed globally.

## Getting Started

Clone the repository:

```bash
git clone https://github.com/fathonyfath/tired-stack.git
cd tired-stack
```

No `npm install` needed — Gradle handles that.

## Development

```bash
./gradlew run
```

Open http://localhost:3000 with your browser to see the result.

## Checks and Formatting

Run all checks (Kotlin lint + Prettier):

```bash
./gradlew check
```

Format all source files (Kotlin + JS/CSS):

```bash
./gradlew format
```

Run tests:

```bash
./gradlew test
```

## Building for Production

Build a fat JAR:

```bash
./gradlew buildFatJar
java -jar build/libs/tired-stack-all.jar
```

Build and run via Docker:

```bash
./gradlew runDocker
```

Push to GHCR (requires `GHCR_USERNAME` and `GHCR_TOKEN` env vars):

```bash
./gradlew publishImage
```

## Git Hooks

To enable the pre-commit lint hook:

```bash
./gradlew setupGitHooks
```

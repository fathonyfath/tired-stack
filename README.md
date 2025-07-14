# Tired Stack

This project is built with the Tired Stack, a custom stack designed for efficient web development.

## Stack Overview

The Tired Stack is comprised of the following key technologies:

* **Bun**: A fast all-in-one JavaScript runtime, bundler, and package manager, used for running the development server, building assets, and managing dependencies.
* **Elysia.js**: A fast, performant, and type-safe web framework for Bun, used for building the server-side application.
* **Tailwind CSS**: A utility-first CSS framework for rapidly building custom designs. It's configured with PostCSS for processing.
* **PostCSS**: A tool for transforming CSS with JavaScript plugins, used for processing Tailwind CSS and other CSS imports.
* **Prettier**: A code formatter to ensure consistent code style across the project.
* **KitaJS/HTML**: A JSX-like library for server-side HTML rendering
* **DaisyUI**: A Tailwind CSS component library that provides pre-built UI components.
* **Iconify**: A tool for easily adding SVG icons to your project, integrated with Tailwind CSS.
* **SimpleBar**: A customizable scrollbar plugin, used to create custom scroll areas.

While only Bun and Elysia are essential, other libraries are included to enhance basic web development. Additional tools like HTMX or Alpine.js can be integrated to address specific use cases.

## Getting Started

To get started with this project, clone the repository and install the dependencies:

```bash
git clone https://github.com/fathonyfath/tired-stack.git
cd tired-stack
bun install
```

## Development

To start the development server and watch for changes in assets and server files, run:

```bash
bun run dev
```

This will run both `watch:assets` and `watch:server` scripts concurrently.

* `watch:assets`: Compiles CSS and JavaScript assets and watches for changes.
* `watch:server`: Restarts the Elysia.js server on changes to src/main.tsx.

Open http://localhost:3000/ with your browser to see the result.

## Building for Production

```bash
bun run build
```

This will run both `build:assets` and `build:server` scripts concurrently.

* `build:assets`: Compiles and minifies CSS and JavaScript assets.
* `build:server`: Compiles the server-side code into a single, minified executable.

## Code Formatting

To format the code using Prettier, run:

```bash
bun run prettier:write
```

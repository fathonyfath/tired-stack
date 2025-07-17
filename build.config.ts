import { build } from "bun";
import { existsSync } from "fs";
import { mkdir, watch, readFile, writeFile } from "fs/promises";
import postcss from "postcss";

interface BuildOptions {
  watch?: boolean;
  minify?: boolean;
}

async function ensureDir(dir: string) {
  try {
    await mkdir(dir, { recursive: true });
  } catch (error) {
    // dir might be already exist
  }
}

async function buildCSS(opitons: BuildOptions = {}) {
  const inputCSS = "./src/stylesheet.css";
  const outputCSS = "./public/stylesheet.css";

  await ensureDir("./public");

  try {
    const config = require("./postcss.config");
    const presetPlugins = config.plugins;
    const plugins = [
      ...presetPlugins,
      ...(opitons.minify ? [require("postcss-minify")()] : []),
    ];

    const inputStringCSS = await readFile(inputCSS, { encoding: "utf8" });
    const result = await postcss(plugins).process(inputStringCSS, {
      from: inputCSS,
      to: outputCSS,
      map: { inline: false },
    });

    await writeFile(outputCSS, result.css);

    if (result.map) {
      await writeFile(`${result.opts.to}.map`, result.map.toString());
    }

    if (result.warnings().length > 0) {
      console.log("⚠️ PostCSS Warnings:");
      result.warnings().forEach((warning) => console.log(warning.toString()));
    }

    console.log("✅ CSS build completed");
  } catch (error) {
    console.error("❌ CSS build error:", error);
    throw error;
  }
}

async function buildJS(options: BuildOptions = {}) {
  await ensureDir("./public");

  try {
    const result = await build({
      entrypoints: ["./src/script.ts"],
      outdir: "./public",
      target: "browser",
      format: "esm",
      minify: options.minify,
      splitting: false,
      sourcemap: options.minify ? "external" : "inline",
      naming: {
        entry: "script.js",
        chunk: "[name]-[hash].js",
        asset: "[name].[ext]",
      },
      define: {
        "process.env.NODE_ENV": JSON.stringify(
          process.env.NODE_ENV || "development",
        ),
      },
      external: [],
    });

    if (result.success) {
      console.log("✅ JavaScript build completed");
      return result;
    } else {
      console.error("❌ JavaScript build failed");
      if (result.logs && result.logs.length > 0) {
        result.logs.forEach((log) => console.error(log));
      }
      throw new Error("JavaScript build failed");
    }
  } catch (error) {
    console.error("❌ JavaScript build error:", error);
    throw error;
  }
}

async function watchFiles(options: BuildOptions) {
  console.log("👀 Starting file watchers...");

  let isBuilding = false;
  const debounceTime = 100;
  let buildTimeout: Timer | null = null;

  const debouncedBuild = async (type: "js" | "css" | "both") => {
    if (buildTimeout) {
      clearTimeout(buildTimeout);
    }

    buildTimeout = setTimeout(async () => {
      console.log(`🔄 Rebuilding ${type.toUpperCase()}...`);

      try {
        if (type === "js" || type === "both") {
          await buildJS(options);
        }
        if (type === "css" || type === "both") {
          await buildCSS(options);
        }

        console.log(`✅ ${type.toUpperCase()} rebuild completed`);
      } catch (error) {
        console.log(`❌ ${type.toUpperCase()} rebuild failed:`, error);
      } finally {
        isBuilding = false;
      }
    }, debounceTime);
  };

  // Watch files
  const watchPaths = ["./src"];
  const jsWatchExtensions = [".js", ".ts"];
  const cssWatchExtensions = [".css", ".scss", ".tsx", ".jsx"];

  for (const watchPath of watchPaths) {
    if (existsSync(watchPath)) {
      try {
        const watcher = watch(watchPath, { recursive: true });
        console.log(`👀 Watching files in ${watchPath}`);

        (async () => {
          for await (const _event of watcher) {
            const jsFileChanged = jsWatchExtensions.some((ext) =>
              _event.filename?.endsWith(ext),
            );

            const cssFileChanged = cssWatchExtensions.some((ext) =>
              _event.filename?.endsWith(ext),
            );

            if (jsFileChanged && cssFileChanged) {
              debouncedBuild("both");
            } else if (jsFileChanged) {
              debouncedBuild("js");
            } else if (cssFileChanged) {
              debouncedBuild("css");
            }
          }
        })().catch(console.error);
      } catch (error) {
        console.warn(`Could not watch ${watchPath}:`, error);
      }
    }
  }
}

async function buildAssets(options: BuildOptions = {}) {
  console.log("🔨 Building assets...");

  if (options.watch) {
    console.log("👀 Starting watch mode...");

    // Initial build
    try {
      await Promise.all([buildJS(options), buildCSS(options)]);
      console.log("✅ Initial build completed");
    } catch (error) {
      console.error("❌ Initial build failed:", error);
      process.exit(1);
    }

    // Start watchers
    await watchFiles(options);
    console.log("✅ Watch mode started - files will rebuild on changes");

    // Keep the process alive
    const keepAlive = () => {
      return new Promise<never>(() => {
        // Thhis promise never resolves, keep the process alive
      });
    };

    // Handle cleanup
    const cleanup = () => {
      console.log("\n🛑 Stopping build watcher...");
      process.exit(0);
    };

    process.on("SIGINT", cleanup);
    process.on("SIGTERM", cleanup);

    // Keep process alive
    await keepAlive();
  } else {
    // Build both in parallel for production
    try {
      await Promise.all([buildJS(options), buildCSS(options)]);
      console.log("📦 All assets built successfully");
    } catch (error) {
      console.error("❌ Build failed:", error);
      process.exit(1);
    }
  }
}

// CLI handling
const args = process.argv.slice(2);
const watchMode = args.includes("--watch");
const minifyMode = args.includes("--minify");

if (import.meta.main) {
  await buildAssets({
    watch: watchMode,
    minify: minifyMode,
  });
}

export { buildAssets, buildCSS, buildJS };

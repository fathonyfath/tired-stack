const esbuild = require("esbuild");
const { readFileSync, writeFileSync, rmSync, mkdirSync } = require("fs");
const crypto = require("crypto");
const path = require("path");

const ASSET_LOADERS = Object.fromEntries(
  [
    "png",
    "jpg",
    "jpeg",
    "gif",
    "webp",
    "avif",
    "svg",
    "woff",
    "woff2",
    "ttf",
    "otf",
    "eot",
  ].map((ext) => [`.${ext}`, "file"])
);

async function runPostcss(css, input, plugins) {
  const postcss = require("postcss");
  const instances = plugins.map(({ name, options }) => {
    const mod = require(name);
    const plugin = mod.default ?? mod;
    return plugin(options);
  });
  const result = await postcss(instances).process(css, { from: input });
  return result.css;
}

async function build(input, outdir, meta, minify, plugins) {
  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  let css = readFileSync(input, "utf8");
  if (plugins.length) css = await runPostcss(css, input, plugins);

  /**
   * Resolves relative imports from the entry's folder.
   */
  const result = await esbuild.build({
    stdin: {
      contents: css,
      loader: "css",
      resolveDir: path.dirname(input),
      sourcefile: path.basename(input),
    },
    bundle: true,
    minify,
    sourcemap: minify ? false : "inline",
    outdir,
    assetNames: "[name]-[hash]",
    loader: ASSET_LOADERS,
    /**
     * Prefers a package's stylesheet over its JS entry.
     */
    conditions: ["style"],
    mainFields: ["style", "main"],
    write: false,
  });

  const stylesheet = result.outputFiles.find((f) => f.path.endsWith(".css"));
  for (const file of result.outputFiles) {
    if (file !== stylesheet) writeFileSync(file.path, file.contents);
  }

  const hash = crypto
    .createHash("md5")
    .update(stylesheet.contents)
    .digest("hex")
    .slice(0, 8)
    .toUpperCase();
  /**
   * Stable in development, so the watcher can replace it in place.
   */
  const base = path.basename(input, ".css");
  const outName = minify ? `${base}-${hash}.css` : `${base}.css`;
  writeFileSync(`${outdir}/${outName}`, stylesheet.contents);

  writeFileSync(meta, `${path.basename(input)}=${outName}`);
}

const { parseArgs } = require("util");

const { values } = parseArgs({
  options: {
    input: { type: "string" },
    outdir: { type: "string" },
    meta: { type: "string" },
    minify: { type: "boolean" },
    postcss: { type: "string" },
  },
});

const missing = ["input", "outdir", "meta"].filter((k) => !values[k]);
if (missing.length) {
  console.error(`Missing required args: ${missing.map((k) => `--${k}`).join(", ")}

Usage: build-css.js --input <file> --outdir <dir> --meta <file> [--minify] [--postcss <json>]

  --input    Path to the CSS entry file
  --outdir   Output directory for compiled CSS
  --meta     Path to write the output metadata (key=value)
  --minify   Minify output and skip sourcemaps
  --postcss  JSON array of PostCSS plugins to run first: [{"name": "...", "options": {...}}]`);
  process.exit(1);
}

const { input, outdir, meta, minify } = values;
const plugins = values.postcss ? JSON.parse(values.postcss) : [];

build(input, outdir, meta, minify, plugins).catch((error) => {
  console.error(error);
  process.exit(1);
});

const esbuild = require("esbuild");
const { writeFileSync, rmSync, mkdirSync } = require("fs");
const path = require("path");

function build(input, outdir, meta, minify) {
  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  const result = esbuild.buildSync({
    entryPoints: [input],
    bundle: true,
    minify,
    sourcemap: !minify,
    target: "es2020",
    outdir,
    entryNames: "[name]-[hash]",
    metafile: true,
  });

  const outputs = Object.keys(result.metafile.outputs);
  const jsFile = outputs.find((f) => f.endsWith(".js") && !f.endsWith(".map"));
  const jsBasename = path.basename(jsFile);

  writeFileSync(meta, `${path.basename(input)}=${jsBasename}`);
}

const { parseArgs } = require("util");

const { values } = parseArgs({
  options: {
    input: { type: "string" },
    outdir: { type: "string" },
    meta: { type: "string" },
    minify: { type: "boolean" },
  },
});

const missing = ["input", "outdir", "meta"].filter((k) => !values[k]);
if (missing.length) {
  console.error(`Missing required args: ${missing.map((k) => `--${k}`).join(", ")}

Usage: build-js.js --input <file> --outdir <dir> --meta <file> [--minify]

  --input   Path to the JS entry file
  --outdir  Output directory for compiled JS
  --meta    Path to write the output metadata (key=value)
  --minify  Minify output and skip sourcemaps`);
  process.exit(1);
}

const { input, outdir, meta, minify } = values;

build(input, outdir, meta, minify);

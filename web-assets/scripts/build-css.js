const postcss = require("postcss");
const { readFileSync, writeFileSync, mkdirSync, rmSync } = require("fs");
const crypto = require("crypto");
const path = require("path");

async function build(input, outdir, meta, minify) {
  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  const plugins = [
    require("@tailwindcss/postcss")(),
    require("postcss-lightningcss")({ minify }),
  ];

  const cssInput = readFileSync(input, "utf8");
  const filename = path.basename(input);
  const result = await postcss(plugins).process(cssInput, {
    from: input,
    to: `${outdir}/${filename}`,
    map: minify ? false : { inline: false },
  });

  const hash = crypto
    .createHash("md5")
    .update(result.css)
    .digest("hex")
    .slice(0, 8)
    .toUpperCase();
  const hashedName = `${path.basename(input, ".css")}-${hash}.css`;

  writeFileSync(`${outdir}/${hashedName}`, result.css);

  if (!minify && result.map) {
    writeFileSync(`${outdir}/${hashedName}.map`, result.map.toString());
  }

  writeFileSync(meta, `${path.basename(input)}=${hashedName}`);
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

Usage: build-css.js --input <file> --outdir <dir> --meta <file> [--minify]

  --input   Path to the CSS entry file
  --outdir  Output directory for compiled CSS
  --meta    Path to write the output metadata (key=value)
  --minify  Minify output and skip sourcemaps`);
  process.exit(1);
}

const { input, outdir, meta, minify } = values;

build(input, outdir, meta, minify);

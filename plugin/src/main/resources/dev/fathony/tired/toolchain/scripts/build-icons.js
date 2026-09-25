const {
  readFileSync,
  writeFileSync,
  existsSync,
  rmSync,
  mkdirSync,
} = require("fs");
const crypto = require("crypto");

const DROPPED_ATTRIBUTES = new Set(["xmlns", "width", "height", "class"]);

function toSymbol(name, source) {
  const match = source.match(/<svg\b([^>]*)>([\s\S]*)<\/svg>/);
  if (!match) throw new Error(`Not an SVG: ${name}`);

  const attributes = [...match[1].matchAll(/([\w:-]+)="([^"]*)"/g)]
    .filter(([, key]) => !DROPPED_ATTRIBUTES.has(key))
    .map(([, key, value]) => `${key}="${value}"`);
  const body = match[2]
    .trim()
    .replace(/>\s+</g, "><")
    .replace(/\s*\/>/g, "/>");

  return `<symbol id="${name}" ${attributes.join(" ")}>${body}</symbol>`;
}

function build(icons, outdir, meta, stable) {
  const file = (name) => `node_modules/lucide-static/icons/${name}.svg`;
  const missing = icons.filter((name) => !existsSync(file(name)));
  if (missing.length) {
    console.error(`Unknown icons: ${missing.join(", ")}`);
    process.exit(1);
  }

  const symbols = icons.map((name) =>
    toSymbol(name, readFileSync(file(name), "utf8"))
  );
  const sprite = `<svg xmlns="http://www.w3.org/2000/svg">${symbols.join("")}</svg>`;

  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  const hash = crypto
    .createHash("md5")
    .update(sprite)
    .digest("hex")
    .slice(0, 8)
    .toUpperCase();
  /**
   * Stable in development, like the stylesheet and script.
   */
  const name = stable ? "icons.svg" : `icons-${hash}.svg`;

  writeFileSync(`${outdir}/${name}`, sprite);
  writeFileSync(meta, `icons.svg=${name}`);
}

const { parseArgs } = require("util");

const { values } = parseArgs({
  options: {
    icons: { type: "string" },
    outdir: { type: "string" },
    meta: { type: "string" },
    stable: { type: "boolean" },
  },
});

const missing = ["icons", "outdir", "meta"].filter((k) => !values[k]);
if (missing.length) {
  console.error(`Missing required args: ${missing.map((k) => `--${k}`).join(", ")}

Usage: build-icons.js --icons <file> --outdir <dir> --meta <file> [--stable]

  --icons   File listing lucide icon names, one per line
  --outdir  Output directory for compiled SVG sprite
  --meta    Path to write the output metadata (key=value)
  --stable  Name the sprite icons.svg instead of hashing it`);
  process.exit(1);
}

const { icons, outdir, meta, stable } = values;

const names = readFileSync(icons, "utf8").split("\n").filter(Boolean);
build(names, outdir, meta, stable);

const { execSync } = require("child_process");
const {
  readFileSync,
  renameSync,
  writeFileSync,
  existsSync,
  rmSync,
  mkdirSync,
} = require("fs");
const crypto = require("crypto");
const path = require("path");

function build(icons, outdir, meta) {
  const missing = icons.filter(
    (name) => !existsSync(`node_modules/lucide-static/icons/${name}.svg`)
  );
  if (missing.length) {
    console.error(`Unknown icons: ${missing.join(", ")}`);
    process.exit(1);
  }

  const paths = icons
    .map((name) => `node_modules/lucide-static/icons/${name}.svg`)
    .join(" ");

  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  execSync(
    `npx svg-sprite --symbol --symbol-dest=${outdir} --symbol-sprite=icons.svg ${paths}`,
    { stdio: "inherit" }
  );

  const svgContent = readFileSync(`${outdir}/icons.svg`, "utf8");
  const hash = crypto
    .createHash("md5")
    .update(svgContent)
    .digest("hex")
    .slice(0, 8)
    .toLowerCase();
  const hashedName = `icons-${hash}.svg`;

  renameSync(`${outdir}/icons.svg`, `${outdir}/${hashedName}`);

  writeFileSync(meta, `icons.svg=${hashedName}`);
}

const { parseArgs } = require("util");

const { values } = parseArgs({
  options: {
    icons: { type: "string" },
    outdir: { type: "string" },
    meta: { type: "string" },
  },
});

const missing = ["icons", "outdir", "meta"].filter((k) => !values[k]);
if (missing.length) {
  console.error(`Missing required args: ${missing.map((k) => `--${k}`).join(", ")}

Usage: build-icons.js --icons <names> --outdir <dir> --meta <file>

  --icons   Comma-separated list of lucide icon names
  --outdir  Output directory for compiled SVG sprite
  --meta    Path to write the output metadata (key=value)`);
  process.exit(1);
}

const { icons, outdir, meta } = values;

build(icons.split(","), outdir, meta);

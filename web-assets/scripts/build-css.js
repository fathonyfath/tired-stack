const postcss = require("postcss");
const {
  readFileSync,
  writeFileSync,
  mkdirSync,
  rmSync,
  existsSync,
} = require("fs");
const crypto = require("crypto");
const path = require("path");
const config = require("../postcss.config");

const minify = process.argv.includes("--minify");
const outdirFlag = process.argv.indexOf("--outdir");
const outdir =
  outdirFlag !== -1 ? process.argv[outdirFlag + 1] : "dist/stylesheets";

const manifestPath = path.resolve(__dirname, "../dist/manifest.json");

async function build() {
  rmSync(outdir, { recursive: true, force: true });
  mkdirSync(outdir, { recursive: true });

  const input = readFileSync("src/stylesheet.css", "utf8");
  const result = await postcss(config.plugins).process(input, {
    from: "src/stylesheet.css",
    to: `${outdir}/stylesheet.css`,
    map: minify ? false : { inline: false },
  });

  const hash = crypto
    .createHash("md5")
    .update(result.css)
    .digest("hex")
    .slice(0, 8);
  const hashedName = `stylesheet-${hash}.css`;

  writeFileSync(`${outdir}/${hashedName}`, result.css);

  if (!minify && result.map) {
    writeFileSync(`${outdir}/${hashedName}.map`, result.map.toString());
  }

  const manifest = existsSync(manifestPath)
    ? JSON.parse(readFileSync(manifestPath, "utf8"))
    : {};
  manifest["stylesheet.css"] = hashedName;
  writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
}

build();

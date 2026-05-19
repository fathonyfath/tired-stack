const { execSync } = require("child_process");
const { readFileSync, renameSync, writeFileSync, existsSync } = require("fs");
const crypto = require("crypto");
const path = require("path");

const icons = process.argv[2].split(",");

const paths = icons
  .map((name) => `node_modules/lucide-static/icons/${name}.svg`)
  .join(" ");

execSync(
  `npx svg-sprite --symbol --symbol-dest=dist/icons --symbol-sprite=icons.svg ${paths}`,
  { stdio: "inherit" },
);

const svgContent = readFileSync("dist/icons/icons.svg", "utf8");
const hash = crypto
  .createHash("md5")
  .update(svgContent)
  .digest("hex")
  .slice(0, 8);
const hashedName = `icons-${hash}.svg`;

renameSync("dist/icons/icons.svg", `dist/icons/${hashedName}`);

const manifestPath = path.resolve(__dirname, "../dist/manifest.json");
const manifest = existsSync(manifestPath)
  ? JSON.parse(readFileSync(manifestPath, "utf8"))
  : {};
manifest["icons.svg"] = hashedName;
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

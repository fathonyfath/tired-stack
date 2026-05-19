const esbuild = require("esbuild");
const {
  writeFileSync,
  readFileSync,
  existsSync,
  rmSync,
  mkdirSync,
} = require("fs");
const path = require("path");

const minify = process.argv.includes("--minify");
const outdirFlag = process.argv.indexOf("--outdir");
const outdir =
  outdirFlag !== -1 ? process.argv[outdirFlag + 1] : "dist/scripts";

rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

const manifestPath = path.resolve(__dirname, "../dist/manifest.json");

const result = esbuild.buildSync({
  entryPoints: ["src/index.js"],
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

const manifest = existsSync(manifestPath)
  ? JSON.parse(readFileSync(manifestPath, "utf8"))
  : {};
manifest["index.js"] = jsBasename;
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

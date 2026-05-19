const esbuild = require("esbuild");

const minify = process.argv.includes("--minify");
const outdirFlag = process.argv.indexOf("--outdir");
const outdir =
  outdirFlag !== -1 ? process.argv[outdirFlag + 1] : "dist/scripts";

esbuild.buildSync({
  entryPoints: ["src/index.js"],
  bundle: true,
  minify,
  sourcemap: !minify,
  target: "es2020",
  outdir,
});

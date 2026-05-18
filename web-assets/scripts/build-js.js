const esbuild = require("esbuild");

const minify = process.argv.includes("--minify");

esbuild.buildSync({
    entryPoints: ["src/index.js"],
    bundle: true,
    minify,
    sourcemap: !minify,
    target: "es2020",
    outdir: "dist/scripts",
});

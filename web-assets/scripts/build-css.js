const postcss = require("postcss");
const { readFile, writeFile, mkdir, rm } = require("fs/promises");
const config = require("../postcss.config");

const minify = process.argv.includes("--minify");
const outdirFlag = process.argv.indexOf("--outdir");
const outdir = outdirFlag !== -1 ? process.argv[outdirFlag + 1] : "dist/stylesheets";

async function build() {
    await rm(outdir, { recursive: true, force: true });
    await mkdir(outdir, { recursive: true });

    const input = await readFile("src/stylesheet.css", "utf8");
    const result = await postcss(config.plugins).process(input, {
        from: "src/stylesheet.css",
        to: `${outdir}/stylesheet.css`,
        map: minify ? false : { inline: false },
    });

    await writeFile(`${outdir}/stylesheet.css`, result.css);

    if (!minify && result.map) {
        await writeFile(`${outdir}/stylesheet.css.map`, result.map.toString());
    }
}

build();

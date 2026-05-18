const postcss = require("postcss");
const { readFile, writeFile, mkdir, rm } = require("fs/promises");
const config = require("../postcss.config");

const minify = process.argv.includes("--minify");

async function build() {
    await rm("dist/stylesheets", { recursive: true, force: true });
    await mkdir("dist/stylesheets", { recursive: true });

    const input = await readFile("src/stylesheet.css", "utf8");
    const result = await postcss(config.plugins).process(input, {
        from: "src/stylesheet.css",
        to: "dist/stylesheets/stylesheet.css",
        map: minify ? false : { inline: false },
    });

    await writeFile("dist/stylesheets/stylesheet.css", result.css);

    if (!minify && result.map) {
        await writeFile("dist/stylesheets/stylesheet.css.map", result.map.toString());
    }
}

build();

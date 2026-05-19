const { execSync } = require("child_process");

const icons = process.argv[2].split(",");

const paths = icons
  .map((name) => `node_modules/lucide-static/icons/${name}.svg`)
  .join(" ");

execSync(
  `npx svg-sprite --symbol --symbol-dest=dist/icons --symbol-sprite=icons.svg ${paths}`,
  { stdio: "inherit" }
);

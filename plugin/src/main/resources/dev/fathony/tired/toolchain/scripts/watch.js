/**
 * Rebuilds assets into the served folder while `./gradlew run` is going.
 */
const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const { parseArgs } = require("util");

const { values } = parseArgs({
  options: {
    project: { type: "string" },
    copy: { type: "string" },
    watch: { type: "string" },
    scripts: { type: "string" },
    css: { type: "string" },
    js: { type: "string" },
    static: { type: "string" },
  },
});

const builds = [
  values.css && {
    name: "stylesheet",
    args: JSON.parse(values.css),
    dist: "dist/stylesheets",
  },
  values.js && {
    name: "script",
    args: JSON.parse(values.js),
    dist: "dist/scripts",
  },
].filter(Boolean);

function sync(relative) {
  const source = path.join(values.project, relative);
  const target = path.join(values.copy, relative);
  if (!fs.existsSync(source)) {
    fs.rmSync(target, { recursive: true, force: true });
  } else if (fs.statSync(source).isFile()) {
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.copyFileSync(source, target);
  }
}

function run(build) {
  const started = Date.now();
  const result = spawnSync(process.execPath, build.args, { stdio: "inherit" });
  if (result.status !== 0) {
    console.error(`[tired] ${build.name} build failed`);
    return;
  }
  for (const file of fs.readdirSync(build.dist)) {
    fs.copyFileSync(
      path.join(build.dist, file),
      path.join(values.static, file)
    );
  }
  console.log(`[tired] rebuilt ${build.name} in ${Date.now() - started}ms`);
}

const pending = new Set();
let timer;

function changed(relative) {
  sync(relative);
  const isScript =
    relative.startsWith(path.normalize(values.scripts) + path.sep) &&
    /\.[cm]?js$/.test(relative);
  for (const build of builds) {
    if (build.name === "stylesheet" || isScript) pending.add(build);
  }
  clearTimeout(timer);
  timer = setTimeout(() => {
    const due = [...pending];
    pending.clear();
    due.forEach(run);
  }, 100);
}

for (const folder of JSON.parse(values.watch)) {
  const absolute = path.join(values.project, folder);
  if (!fs.existsSync(absolute)) continue;
  fs.watch(absolute, { recursive: true }, (_, file) => {
    if (file) changed(path.join(folder, file));
  });
}

console.log("[tired] watching web assets; refresh the browser after a change");

const config: import("postcss-load-config").Config = {
  plugins: [require("@tailwindcss/postcss")(), require("postcss-import")()],
};

module.exports = config;

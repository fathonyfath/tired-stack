const config: import("postcss-load-config").Config = {
  plugins: [require("@tailwindcss/postcss")()],
};

module.exports = config;

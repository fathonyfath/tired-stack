import { file, serve } from "bun";
import Layout from "./Layout";
import HtmxTest from "./HtmxTest";
import AsyncJSX from "./AsyncJSX";
import { withMiddleware, jsx, htmx } from "@server";
import { logging } from "./middlewares/logging";

const server = serve({
  routes: {
    "/:fileName": (req) => new Response(file(`public/${req.params.fileName}`)),
    "/": withMiddleware(
      [logging],
      jsx(() => (
        <Layout name="Hello" js css>
          <h1>Hello world!</h1>
        </Layout>
      )),
    ),
    "/htmx-test": jsx(() => (
      <Layout name="HTMX" js>
        <HtmxTest />
      </Layout>
    )),
    "/htmx": withMiddleware([htmx], (req, ser, ctx) => {
      const response = new Response(JSON.stringify(ctx.htmx, null, 4));
      if (ctx.htmx.isHTMX()) {
        ctx.htmx.reswap(response, "innerHTML");
      }
      return response;
    }),
    "/jsx-suspense": jsx(() => AsyncJSX),
  },
});

const cleanup = async () => {
  try {
    console.log("⏳ Requesting to stop the server...");
    await server.stop();
    console.log("✅ Server stopped successfully");
  } catch (error) {
    console.log("❌ Server failed to close gracefully, error:", error);
  } finally {
    process.exit(0);
  }
};

process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

console.log(`🚀 Server is running at ${server.url}`);

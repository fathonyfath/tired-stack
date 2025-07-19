import { file, serve } from "bun";
import { router } from "@server/routes";
import { jsx } from "@server/jsx";
import { htmx } from "@server/htmx";
import Layout from "./Layout";
import Skeleton from "./Skeleton";
import SimpeBarExample from "./SimpeBarExample";
import HtmxTest from "./HtmxTest";

const htmxDecoration = htmx();

const server = serve({
  routes: router({
    "/:fileName": (p) =>
      p.handle((c) => {
        return new Response(file(`public/${c.request.params.fileName}`));
      }),
    "/": (p) =>
      p.handle(
        jsx(() => (
          <Layout name="Hello" js css>
            <Skeleton />
          </Layout>
        )),
      ),
    "/simplebar": (p) =>
      p.handle(
        jsx(() => (
          <Layout name="SimpleBar Example" js css>
            <SimpeBarExample />
          </Layout>
        )),
      ),
    "/htmx-test": (p) =>
      p.handle(
        jsx(() => (
          <Layout name="HTMX" js>
            <HtmxTest />
          </Layout>
        )),
      ),
    "/htmx": (p) =>
      p.decorate(htmxDecoration).handle((context) => {
        const response = new Response(JSON.stringify(context.htmx, null, 4));
        if (context.htmx.isHTMX()) {
          context.htmx.reswap(response, "innerHTML");
        }
        return response;
      }),
  }),
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

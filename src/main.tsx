import { file, serve } from "bun";
import { handle } from "@server/handle";
import { jsx } from "@server/jsx";
import Layout from "./Layout";
import Skeleton from "./Skeleton";
import SimpeBarExample from "./SimpeBarExample";

const server = serve({
  routes: {
    "/:fileName": handle((req) => {
      return new Response(file(`public/${req.params.fileName}`));
    }),
    "/": handle(
      jsx(() => (
        <Layout name="Hello">
          <Skeleton />
        </Layout>
      )),
    ),
    "/example": handle(
      jsx(() => (
        <Layout name="SimpleBar Example">
          <SimpeBarExample />
        </Layout>
      )),
    ),
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

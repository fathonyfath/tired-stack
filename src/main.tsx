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

console.log(`🚀 Server is running at ${server.url}`);

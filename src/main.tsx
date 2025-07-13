import { Elysia } from "elysia";
import { html } from "@elysiajs/html";
import { Html } from "@kitajs/html";
import staticPlugin from "@elysiajs/static";
import Layout from "./Layout";
import SimpleBarExample from "./SimpeBarExample";
import Skeleton from "./Skeleton";

const app = new Elysia()
  .use(
    staticPlugin({
      assets: "public",
      prefix: "",
      noCache: true,
    }),
  )
  .use(html())
  .get("/", () => (
    <Layout name="Hello">
      <Skeleton />
    </Layout>
  ))
  .get("/example", () => (
    <Layout name="SimpleBar Example">
      <SimpleBarExample />
    </Layout>
  ))
  .listen(3000);

console.log(
  `🦊 Elysia is running at ${app.server?.hostname}:${app.server?.port}`,
);

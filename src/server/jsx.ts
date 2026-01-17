import { renderToStream } from "@kitajs/html/suspense";
import type { BunRequest, Server } from "bun";
import { Readable } from "node:stream";
import { adapter } from "./adapter";

type SuspendableJSX = (rid: number | string) => JSX.Element;
type JSX = JSX.Element | SuspendableJSX;

/**
 * Creates a Bun request handler that renders JSX components, supporting
 * both immediate JSX elements and suspendable (asynchronous) JSX functions.
 * It uses the `adapter` to ensure a Response object is always returned
 * and sets the correct `Content-Type` header for HTML.
 *
 * For suspendable JSX, it leverages `@kitajs/html/suspense` to stream
 * the rendering output, improving perceived performance for clients.
 *
 * @template Path The type of the path parameter in the BunRequest.
 * @template WS The type of the WebSocket server.
 * @template Ctx The type of the context object.
 *
 * @param {function(BunRequest<Path>, Server<WS>, Ctx): JSX} component
 *   A function that takes a BunRequest, a Bun Server, and a context object,
 *   and returns either a JSX.Element or a function that returns a JSX.Element
 *   (for suspendable components).
 * @returns {ReturnType<typeof adapter<Path, WS, Ctx>>}
 *   A Bun-compatible request handler that renders the JSX component to an HTML Response.
 */
export function jsx<Path extends string = string, WS = unknown, Ctx = any>(
  component: (req: BunRequest<Path>, server: Server<WS>, ctx: Ctx) => JSX,
) {
  return adapter<Path, WS, Ctx>(async (req, server, ctx) => {
    const response = await component(req, server, ctx);
    const init: ResponseInit = {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
      },
    };
    if (typeof response == "function") {
      const stream = renderToStream(response);
      return new Response(Readable.toWeb(stream) as any, init);
    } else {
      return new Response(response, init);
    }
  });
}

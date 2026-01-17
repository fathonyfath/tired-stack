import type { BunRequest, MaybePromise, Serve, Server } from "bun";

type UniversalHandler<Req extends Request, S, Ctx, Res> = (
  request: Req,
  server: S,
  context?: Ctx,
) => MaybePromise<Res>;

type BodyInput =
  | string
  | Blob
  | ArrayBuffer
  | ReadableStream
  | Response
  | null
  | undefined;

/**
 * Creates a universal handler for Bun requests that ensures a Response object is returned.
 * This adapter is useful for wrapping functions that may return various types of content
 * (string, Blob, ArrayBuffer, ReadableStream, Response, null, undefined) and
 * automatically converting them into a Bun Response object if they are not already one.
 *
 * @template Path The type of the path parameter in the BunRequest.
 * @template WS The type of the WebSocket server.
 * @template Ctx The type of the context object passed to the generator.
 *
 * @param {function(BunRequest<Path>, Server<WS>, Ctx): MaybePromise<BodyInput>} generator
 *   A function that takes a BunRequest, a Bun Server, and a context object,
 *   and returns a Promise resolving to content that can be used to create a Response,
 *   or a Response object itself.
 * @returns {UniversalHandler<BunRequest<Path>, Server<WS>, Ctx, Response>}
 *   A Bun-compatible request handler that always returns a Promise resolving to a Response object.
 *
 * @example
 * // How to create a custom adapter for JSON responses:
 * import { serve } from 'bun';
 * import { adapter } from './adapter'; // Assuming adapter.ts is in the same directory
 *
 * type JsonBody = object | any[] | string | number | boolean | null;
 *
 * export function json<Path extends string = string, WS = unknown, Ctx = any>(
 *   handler: (req: BunRequest<Path>, server: Server<WS>, ctx: Ctx) => JsonBody,
 * ) {
 *   return adapter<Path, WS, Ctx>(async (req, server, ctx) => {
 *     const data = await handler(req, server, ctx);
 *     return new Response(JSON.stringify(data), {
 *       headers: {
 *         'Content-Type': 'application/json; charset=utf-8',
 *       },
 *     });
 *   });
 * }
 *
 * // Then use it in your Bun server:
 * // serve({
 * //   routes: {
 * //     '/api/data': json(() => ({ message: 'Hello from JSON adapter!' }))
 * //   }
 * // });
 */
export function adapter<Path extends string = string, WS = unknown, Ctx = any>(
  generator: (
    req: BunRequest<Path>,
    server: Server<WS>,
    ctx: Ctx,
  ) => MaybePromise<BodyInput>,
): UniversalHandler<BunRequest<Path>, Server<WS>, Ctx, Response> {
  return async (request, server, context) => {
    const safeCtx = (context || {}) as Ctx;

    const content = await generator(request, server, safeCtx);

    if (content instanceof Response) {
      return content;
    }

    return new Response(content);
  };
}

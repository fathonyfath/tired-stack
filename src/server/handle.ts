import { BunRequest, Server } from "bun";

/**
 * A utility type that represents a value which can be either synchronous (`T`)
 * or asynchronous (`Promise<T>`).
 */
export type Awaitable<T> = Promise<T> | T;

/**
 * Defines the signature of the `next` function passed to middleware.
 * Calling this function passes control to the next middleware or handler in the chain.
 * @template T A string literal type that specifies the request shape.
 * @param request The incoming request object.
 * @param server The Bun server instance.
 * @returns An awaitable `Response` object.
 */
export type Next<T extends string> = (
  request: BunRequest<T>,
  server: Server,
) => Awaitable<Response>;

/**
 * Defines the signature for a middleware function. Middlewares are intermediary
 * steps that process a request before the final handler.
 * @template T The specific request string type for the chain.
 * @param request The incoming request object.
 * @param server The Bun server instance.
 * @param next The function to call to pass control to the next function in the chain.
 * @returns An awaitable `Response` object.
 * @note For full type-safety across the chain, `request` could be typed as `BunRequest<T>`.
 */
export type Middleware<T extends string> = (
  request: BunRequest,
  server: Server,
  next: Next<T>,
) => Awaitable<Response>;

/**
 * Defines the signature for the final handler function in a chain. This function
 * is the ultimate destination of the request and generates the final response.
 * @template T The specific request string type.
 * @param request The incoming request object.
 * @param server The Bun server instance.
 * @returns An awaitable `Response` object.
 */
export type Handler<T extends string> = (
  request: BunRequest<T>,
  server: Server,
) => Awaitable<Response>;

/**
 * A generic utility type defining a function for adapting or transforming
 * a request into a different data structure or value.
 * @template T The specific request string type.
 * @template R The generic return type of the adapter.
 * @param request The incoming request object.
 * @param server The Bun server instance.
 * @returns A value of type `R`.
 */
export type Adapter<T extends string, R> = (
  request: BunRequest<T>,
  server: Server,
) => R;

/**
 * A higher-order function that composes a sequence of middlewares and a final
 * handler into a single, executable `Handler` function.
 * @template T The specific request string type for the entire chain.
 * @template M An array of `Middleware<T>` types.
 * @param {...Middleware<T>} args A sequence of zero or more middlewares,
 * followed by exactly one final handler.
 * @returns A new `Handler<T>` function that, when called, will execute the
 * entire middleware chain in order.
 * @example
 * const apiRoute = handle<'GET /api/status'>(
 *   // Middleware for logging
 *   async (request, server, next) => {
 *     console.log(`Accessing ${request.url}...`);
 *     return await next(request, server);
 *   },
 *   // Final Handler
 *   (request, server) => {
 *     return new Response(JSON.stringify({ status: "ok" }));
 *   }
 * );
 * // `apiRoute` can now be used as a fetch handler in Bun.
 */
export function handle<T extends string, M extends Middleware<T>[]>(
  ...args: [...M, Handler<T>]
): Handler<T> {
  return (request, server) => {
    let currentIndex = 0;
    const next: Next<T> = () => {
      return args[currentIndex++](request, server, next);
    };
    return next(request, server);
  };
}

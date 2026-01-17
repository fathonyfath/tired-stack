import type { BunRequest, MaybePromise, Serve, Server } from "bun";

type Collector<Req extends Request, S, Out> = (
  request: Req,
  server: S,
) => MaybePromise<Out | Response>;

type Interceptor<Req extends Request, S> = (
  request: Req,
  server: S,
  next: () => Promise<Response>,
) => MaybePromise<Response>;

type AnyMiddleware<Req extends Request, S> =
  | Collector<Req, S, any>
  | Interceptor<Req, S>;

type GetOutput<T> = T extends Collector<any, any, infer O> ? O : {};

type MergeContexts<T extends any[]> = T extends [infer Head, ...infer Tail]
  ? GetOutput<Head> & MergeContexts<Tail>
  : {};

export type ContextHandler<Req extends Request, S, Ctx, Res> = (
  request: Req,
  server: S,
  context: Ctx,
) => MaybePromise<Res>;

/**
 * Creates a request handler that applies a chain of middleware to a Bun request.
 * Middleware can either be "collectors" that enrich a context object, or "interceptors"
 * that can modify the request/response flow or short-circuit the chain.
 *
 * The context object is dynamically typed based on the output of the collector middleware,
 * providing strong type safety for the final handler function.
 *
 * @template Path The type of the path parameter in the BunRequest.
 * @template WS The type of the WebSocket server.
 * @template Res The expected return type of the final handler.
 * @template Ms A tuple of middleware functions.
 *
 * @param {Ms} middlewares An array of middleware functions (collectors or interceptors).
 * @param {ContextHandler<BunRequest<Path>, Server<WS>, MergeContexts<[...Ms]>, Res>} handler
 *   The final handler function that receives the request, server, and the
 *   merged context object from the collectors.
 * @returns {Serve.Handler<BunRequest<Path>, Server<WS>, Res>}
 *   A Bun-compatible request handler that executes the middleware chain
 *   and then the final handler.
 */
export function withMiddleware<
  Path extends string,
  WS = unknown,
  Res = Response,
  const Ms extends readonly AnyMiddleware<BunRequest<Path>, Server<WS>>[] = [],
>(
  middlewares: Ms,
  handler: ContextHandler<
    BunRequest<Path>,
    Server<WS>,
    MergeContexts<[...Ms]>,
    Res
  >,
): Serve.Handler<BunRequest<Path>, Server<WS>, Res> {
  return async (request, server) => {
    const context: any = {};
    const chain = [...middlewares];

    const dispatch = async (i: number): Promise<any> => {
      const fn = chain[i];

      // End of chain: Run Handler
      if (!fn) {
        return handler(request, server, context);
      }

      // Case A: Interceptor (3 args)
      if (fn.length === 3) {
        const interceptor = fn as Interceptor<BunRequest<Path>, Server<WS>>;
        return interceptor(
          request,
          server,
          () => dispatch(i + 1), // Next function
        );
      }

      // Case B: Collector (2 args)
      const collector = fn as Collector<BunRequest<Path>, Server<WS>, any>;
      const result = await collector(request, server);

      // Short-circuit
      if (result instanceof Response) {
        return result;
      }

      // Merge
      if (result && typeof result === "object") {
        Object.assign(context, result);
      }

      return dispatch(i + 1);
    };

    // Cast the result to satisfy the generic signature
    return dispatch(0) as Promise<Res>;
  };
}

/**
 * A helper function to create a collector middleware with proper type inference.
 * Collector middleware functions are responsible for gathering data and enriching
 * the context object that is passed down the middleware chain to the final handler.
 * They can also short-circuit the request by returning a Response.
 *
 * @template WS The type of the WebSocket server.
 * @template Out The type of the output that this collector adds to the context.
 * @param {Collector<BunRequest<any>, Server<WS>, Out>} fn The collector function.
 * @returns {Collector<BunRequest<any>, Server<WS>, Out>} The typed collector function.
 */
export function createCollector<WS = unknown, Out = any>(
  fn: Collector<BunRequest<any>, Server<WS>, Out>,
): Collector<BunRequest<any>, Server<WS>, Out> {
  return fn;
}

/**
 * A helper function to create an interceptor middleware with proper type inference.
 * Interceptor middleware functions are similar to traditional middleware; they can
 * inspect and modify the request, or generate a response directly. They receive
 * a `next` function to call the subsequent middleware or the final handler in the chain.
 *
 * @template WS The type of the WebSocket server.
 * @param {Interceptor<BunRequest<any>, Server<WS>>} fn The interceptor function.
 * @returns {Interceptor<BunRequest<any>, Server<WS>>} The typed interceptor function.
 */
export function createInterceptor<WS = unknown>(
  fn: Interceptor<BunRequest<any>, Server<WS>>,
): Interceptor<BunRequest<any>, Server<WS>> {
  return fn;
}

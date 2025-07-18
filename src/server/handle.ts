import { BunRequest, Server } from "bun";
import { isContext } from "vm";

export type Awaitable<T> = Promise<T> | T;

export interface Context<T extends string = string> {
  request: BunRequest<T>;
  server: Server;
}

export type Next = () => Awaitable<Response>;

export type Middleware<C_In extends Context> = (
  ctx: C_In,
  next: Next,
) => Awaitable<Response>;

export type ContextProvider<C_In extends Context, C_Out> = (
  ctx: C_In & C_Out,
  next: Next,
) => Awaitable<Response>;

export class Pipeline<C extends Context> {
  private middlewares: Middleware<any>[];

  constructor(middlewares: Middleware<any>[] = []) {
    this.middlewares = middlewares;
  }

  use<P_Out>(provider: ContextProvider<C, P_Out>): Pipeline<C & P_Out>;

  use(middleware: Middleware<C>): Pipeline<C>;

  use(middleware: Middleware<C>): Pipeline<C> {
    // Return a new pipeline instance to ensure type immutability
    return new Pipeline([...this.middlewares, middleware]);
  }

  handle(
    handler: (ctx: C) => Awaitable<Response>,
  ): (request: BunRequest, server: Server) => Awaitable<Response> {
    const finalChain = [...this.middlewares, handler];

    return (request, server) => {
      const ctx: Context = { request, server };

      let index = 0;
      const next: Next = () => {
        const fn = finalChain[index++];
        return fn(ctx, next);
      };

      return next();
    };
  }
}

export function createPipeline() {
  return new Pipeline();
}

type Htmx = { htmx: { id: number; name: string } };

const htmxHandler: ContextProvider<Context, Htmx> = async (context, next) => {
  context.htmx = { id: 5, name: "User" };
  return await next();
};

type FooBar = { user: { id: number; name: string } };

const authHandler: ContextProvider<Context, FooBar> = async (context, next) => {
  context.user = { id: 5, name: "User" };
  return await next();
};

const handler = createPipeline()
  .use(authHandler)
  .use(async (context, next) => await next())
  .use(htmxHandler)
  .handle((context) => new Response());

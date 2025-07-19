import { BunRequest, Server } from "bun";

type Awaitable<T> = T | Promise<T>;
type IsNever<T> = [T] extends [never] ? true : false;
type ConflictingKeys<T> = {
  [K in keyof T]: IsNever<T[K]> extends true ? K : never;
}[keyof T];

type Next = () => Awaitable<Response>;

export interface Context<T extends string = string> {
  request: BunRequest<T>;
  server: Server;
}

export type Middleware<C extends Context> = (
  context: C,
  next: Next,
) => Awaitable<Response>;

export type Decorator<C extends Context, D> = (context: C) => Awaitable<D>;

export type Adapter<T extends string, R> = (context: Context<T>) => R;

export type Handler<T extends string> = (
  context: Context<T>,
) => Awaitable<Response>;

export class Pipeline<R extends string, C extends Context<R>> {
  private middlewares: Middleware<any>[];

  constructor(middlewares: Middleware<any>[] = []) {
    this.middlewares = middlewares;
  }

  use(middleware: Middleware<C>): Pipeline<R, C> {
    return new Pipeline([...this.middlewares, middleware]);
  }

  decorate<D>(
    decorator: [ConflictingKeys<C & D>] extends [never]
      ? Decorator<C, D>
      : {
          "Error: Decorator adds conflicting properties": ConflictingKeys<
            C & D
          >;
        },
  ): Pipeline<R, C & D>;
  decorate<D>(decorator: Decorator<C, D>): Pipeline<R, C & D> {
    const decoratorMiddleware: Middleware<C> = async (context, next) => {
      const newData = await decorator(context);
      Object.assign(context, newData);
      return next();
    };
    return new Pipeline([...this.middlewares, decoratorMiddleware]) as any;
  }

  handle(
    handler: (context: C) => Awaitable<Response>,
  ): (request: BunRequest<R>, server: Server) => Awaitable<Response> {
    const finalChain = [...this.middlewares, handler];

    return (request, server) => {
      const context: Context<R> = { request, server };
      let index = 0;
      const next: Next = () => {
        const fn = finalChain[index++];
        return fn(context, next);
      };
      return next();
    };
  }
}

export function pipeline<R extends string>(): Pipeline<R, Context<R>> {
  return new Pipeline();
}

export function createDecorator<C extends Context, D>(
  fn: (context: C) => Awaitable<D>,
): Decorator<C, D> {
  return fn;
}

import { BunRequest, Server } from "bun";

export type Awaitable<T> = Promise<T> | T;

export type Next<T extends string> = (
  request: BunRequest<T>,
  server: Server,
) => Awaitable<Response>;

export type Middleware<T extends string> = (
  request: BunRequest,
  server: Server,
  next: Next<T>,
) => Awaitable<Response>;

export type Handler<T extends string> = (
  request: BunRequest<T>,
  server: Server,
) => Awaitable<Response>;

export type Adapter<T extends string, R> = (
  request: BunRequest<T>,
  server: Server,
) => R;

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

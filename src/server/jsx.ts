import { Adapter, Awaitable, Handler } from "./handle";

type JSX<T extends string> = Adapter<T, JSX.Element>;

export function jsx<T extends string>(handler: JSX<T>): Handler<T> {
  return async (request, server) => {
    const response = await handler(request, server);
    return new Response(response, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
      },
    });
  };
}

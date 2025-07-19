import { Adapter, Handler } from "./pipeline";

type JSX<T extends string> = Adapter<T, JSX.Element>;

export function jsx<T extends string>(handler: JSX<T>): Handler<T> {
  return async (context) => {
    const response = await handler(context);
    return new Response(response, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
      },
    });
  };
}

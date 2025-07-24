import { renderToStream } from "@kitajs/html/suspense";
import type { Adapter, Context } from "./pipeline";
import { Readable } from "node:stream";

type SuspendableJSX = (rid: number | string) => JSX.Element;

type JSX = JSX.Element | SuspendableJSX;

export function jsx<C extends Context>(
  adapter: Adapter.Input<C, JSX>,
): Adapter.Output<C> {
  return async (context) => {
    const response = await adapter(context);
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
  };
}

import type { Adapter, Context } from "./pipeline";

export function jsx<C extends Context>(
  adapter: Adapter.Input<C, JSX.Element>,
): Adapter.Output<C> {
  return async (context) => {
    const response = await adapter(context);
    return new Response(response, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
      },
    });
  };
}

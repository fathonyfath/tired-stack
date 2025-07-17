import { Adapter, Handler } from "./handle";

/**
 * Defines the signature for a simplified handler that returns a JSX element.
 * This type is used as the input for the `jsx` helper function.
 * @template T The specific request string type.
 * @param request The incoming `BunRequest` object.
 * @param server The `Server` instance.
 * @returns An awaitable `JSX.Element` to be rendered as a response.
 */
type JSX<T extends string> = Adapter<T, JSX.Element>;

/**
 * A higher-order function that wraps a JSX-returning handler and converts it
 * into a full `Handler`. It automatically sets the `Content-Type` header
 * to `text/html`.
 *
 * This function relies on KitaJS HTML library to provide a type of `JSX.Element`.
 *
 * @template T The specific request string type for the handler.
 * @param handler A function that returns a `JSX.Element`.
 * @returns A full `Handler<T>` function that can be used by the main `handle` system.
 * @example
 * const myComponent = () => <h1>Hello, World!</h1>;
 *
 * // Use the jsx helper to create a route handler
 * const myRoute = handle(
 *   jsx<'GET /'>(myComponent)
 * );
 *
 * // myRoute is now a standard handler that returns an HTML response.
 */
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

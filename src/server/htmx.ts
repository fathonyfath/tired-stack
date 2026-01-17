import type { BunRequest, Server } from "bun";
import { createCollector } from "./withMiddleware";

/**
 * Defines the possible values for the `HX-Reswap` header.
 * @see https://htmx.org/headers/hx-reswap/
 */
export type HxSwap =
  | "innerHTML"
  | "outerHTML"
  | "textContent"
  | "beforebegin"
  | "afterbegin"
  | "beforeend"
  | "afterend"
  | "delete"
  | "none";

/**
 * A generic type for functions that update HTMX-specific response headers.
 * If the value type `T` is `void`, the function takes only a `Response` object.
 * Otherwise, it takes a `Response` object and a `value` of type `T`.
 */
type HtmxResponseHeaderUpdater<T> = [T] extends [void]
  ? (response: Response) => void
  : (response: Response, value: T) => void;

/**
 * A utility function to set a header on a Response object.
 * @param response The Response object to modify.
 * @param key The name of the header.
 * @param value The value of the header.
 */
function updateResponseHeader(response: Response, key: string, value: string) {
  response.headers.set(key, value);
}

/**
 * The shape of the `htmx` context object that will be injected into the handler.
 */
export type HtmxContext = {
  htmx: {
    /** `true` if the request is from a boosted element. */
    boosted: boolean;
    /** The current URL of the browser. */
    currentURL: string;
    /** `true` if the request is for history restoration. */
    historyRestoreRequest: boolean;
    /** The user response to an `HX-Prompt` header. */
    prompt: string;
    /** `true` if it is an HTMX request. */
    request: boolean;
    /** The `id` of the target element. */
    target: string;
    /** The `name` of the element that triggered the request. */
    triggerName: string;
    /** The `id` of the element that triggered the request. */
    trigger: string;
    /** Sets the `HX-Location` response header. */
    setLocation: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Push-Url` response header. */
    pushURL: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Redirect` response header. */
    redirect: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Refresh` response header to "true". */
    refresh: HtmxResponseHeaderUpdater<void>;
    /** Sets the `HX-Replace-Url` response header. */
    replaceURL: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Reswap` response header. */
    reswap: HtmxResponseHeaderUpdater<HxSwap>;
    /** Sets the `HX-Retarget` response header. */
    retarget: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Reselect` response header. */
    reselect: HtmxResponseHeaderUpdater<string>;
    /** Sets the `HX-Trigger` response header. */
    triggerEvent: HtmxResponseHeaderUpdater<string | Record<string, unknown>>;
    /** Sets the `HX-Trigger-After-Settle` response header. */
    triggerEventAfterSettle: HtmxResponseHeaderUpdater<void>;
    /** Sets the `HX-Trigger-After-Swap` response header. */
    triggerEventAfterSwap: HtmxResponseHeaderUpdater<void>;
    /** A utility function to check if the request is from HTMX. */
    isHTMX: () => boolean;
    /**
     * Returns a new Response with status 286 to stop polling.
     * @see https://htmx.org/docs/#stopping-polling
     */
    stopPolling: (response: Response) => Response;
  };
};

/**
 * Creates a collector that parses HTMX headers from a request and provides
 * a context object with HTMX-specific details and helper functions.
 *
 * @example
 * ```ts
 * import { withMiddleware } from './withMiddleware';
 * import { htmx } from './htmx';
 *
 * const route = withMiddleware([htmx()], (req, server, ctx) => {
 *   if (ctx.htmx.request) {
 *     // It's an HTMX request!
 *     const response = new Response('<p>New Content</p>');
 *     ctx.htmx.reswap(response, 'outerHTML'); // Modify response headers
 *     return response;
 *   }
 *   return new Response('Hello, World!');
 * });
 * ```
 */
export const htmx = createCollector(
  (request: BunRequest): HtmxContext => ({
    htmx: {
      boosted: request.headers.get("HX-Boosted") === "true",
      currentURL: request.headers.get("HX-Current-URL") ?? "",
      historyRestoreRequest:
        request.headers.get("HX-History-Restore-Request") === "true",
      prompt: request.headers.get("HX-Prompt") ?? "",
      request: request.headers.get("HX-Request") === "true",
      target: request.headers.get("HX-Target") ?? "",
      triggerName: request.headers.get("HX-Trigger-Name") ?? "",
      trigger: request.headers.get("HX-Trigger") ?? "",
      setLocation: (response, value) =>
        updateResponseHeader(response, "HX-Location", value),
      pushURL: (response, value) =>
        updateResponseHeader(response, "HX-Push-Url", value),
      redirect: (response, value) =>
        updateResponseHeader(response, "HX-Redirect", value),
      refresh: (response) =>
        updateResponseHeader(response, "HX-Refresh", "true"),
      replaceURL: (response, value) =>
        updateResponseHeader(response, "HX-Replace-Url", value),
      reswap: (response, value) =>
        updateResponseHeader(response, "HX-Reswap", value),
      retarget: (response, value) =>
        updateResponseHeader(response, "HX-Retarget", value),
      reselect: (response, value) =>
        updateResponseHeader(response, "HX-Reselect", value),
      triggerEvent: (response, value) =>
        updateResponseHeader(
          response,
          "HX-Trigger",
          typeof value === "string" ? value : JSON.stringify(value),
        ),
      triggerEventAfterSettle: (response) =>
        updateResponseHeader(response, "HX-Trigger-After-Settle", "true"),
      triggerEventAfterSwap: (response) =>
        updateResponseHeader(response, "HX-Trigger-After-Swap", "true"),
      isHTMX: () =>
        request.headers.get("HX-Request") === "true" ||
        request.headers.get("HX-Boosted") === "true",
      stopPolling: (response) =>
        new Response(response.body, {
          status: 286,
          statusText: response.statusText,
          headers: response.headers,
        }),
    },
  }),
);

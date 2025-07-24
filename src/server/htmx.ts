import { type Context, type Decorator, createDecorator } from "./pipeline";

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

type HtmxResponseHeaderUpdater<T> = [T] extends [void]
  ? (response: Response) => void
  : (response: Response, value: T) => void;

function updateResponseHeader(response: Response, key: string, value: string) {
  response.headers.set(key, value);
}

export type Htmx = {
  htmx: {
    boosted: boolean;
    currentURL: string;
    historyRestoreRequest: boolean;
    prompt: string;
    request: boolean;
    target: string;
    triggerName: string;
    trigger: string;
    setLocation: HtmxResponseHeaderUpdater<string>;
    pushURL: HtmxResponseHeaderUpdater<string>;
    redirect: HtmxResponseHeaderUpdater<string>;
    refresh: HtmxResponseHeaderUpdater<void>;
    replaceURL: HtmxResponseHeaderUpdater<string>;
    reswap: HtmxResponseHeaderUpdater<HxSwap>;
    retarget: HtmxResponseHeaderUpdater<string>;
    reselect: HtmxResponseHeaderUpdater<string>;
    triggerEvent: HtmxResponseHeaderUpdater<string | Record<string, unknown>>;
    triggerEventAfterSettle: HtmxResponseHeaderUpdater<void>;
    triggerEventAfterSwap: HtmxResponseHeaderUpdater<void>;
    isHTMX: () => boolean;
    stopPolling: (response: Response) => Response;
  };
};

export function htmx(): Decorator<Context<string>, Htmx> {
  return createDecorator(({ request }) => ({
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
  }));
}

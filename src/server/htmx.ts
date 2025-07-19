import { Context, createDecorator, Decorator } from "./pipeline";

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
    },
  }));
}

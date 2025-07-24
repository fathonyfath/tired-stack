import type { HTMLBundle, RouterTypes } from "bun";
import { type Context, pipeline, Pipeline } from "./pipeline";

function isHTMLBundle(value: any): value is HTMLBundle {
  return (
    value !== null &&
    typeof value === "object" &&
    typeof (value as HTMLBundle).index === "string"
  );
}

export type PipelineFactory<T extends string> = (
  p: Pipeline<T, Context<T>>,
) => RouterTypes.RouteHandler<T>;

export type HTTPMethodsPipelineFactory<T extends string> = {
  [K in RouterTypes.HTTPMethod]?: PipelineFactory<T>;
};

export type RouteValue<T extends string> =
  | Response
  | false
  | PipelineFactory<T>
  | HTTPMethodsPipelineFactory<T>
  | HTMLBundle;

export type RouterConfig<P> = {
  [K in keyof P]: RouteValue<Extract<K, string>>;
};

export type Routes<P> = {
  [K in keyof P]: RouterTypes.RouteValue<Extract<K, string>>;
};

export function config<const P extends RouterConfig<P>>(config: P): P {
  return config;
}

export function router<P extends RouterConfig<P>>(config: P): Routes<P> {
  const routes = {} as Routes<P>;

  for (const path in config) {
    const key = path as keyof P & string;
    const routeConfig = config[key];

    if (routeConfig instanceof Response || isHTMLBundle(routeConfig)) {
      routes[key] = routeConfig;
      continue;
    }

    if (routeConfig === false) {
      routes[key] = false;
      continue;
    }

    if (typeof routeConfig === "function") {
      const p = pipeline<typeof key>();
      routes[key] = routeConfig(p);
      continue;
    }

    if (typeof routeConfig === "object") {
      const perMethodHandler: RouterTypes.RouteHandlerObject<typeof key> = {};
      for (const method in routeConfig) {
        const methodKey = method as RouterTypes.HTTPMethod;
        const factory = routeConfig[methodKey];
        if (factory) {
          const p = pipeline<typeof key>();
          perMethodHandler[methodKey] = factory(p);
        }
      }
      routes[key] = perMethodHandler;
      continue;
    }
  }

  return routes;
}

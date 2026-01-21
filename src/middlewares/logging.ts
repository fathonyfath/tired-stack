import { createInterceptor } from "@/server";

export const logging = createInterceptor(async (req, _serv, next) => {
  const start = Date.now();
  console.log(`➡️  [${new Date().toISOString()}] ${req.method} ${req.url}`);
  const response = await next();
  const duration = Date.now() - start;
  console.log(
    `⬅️  [${new Date().toISOString()}] ${req.method} ${req.url} - ${duration}ms`,
  );
  return response;
});

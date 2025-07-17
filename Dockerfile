FROM oven/bun:debian AS build

WORKDIR /app

ADD ["./", "./"]

RUN bun install

RUN bun run build

FROM gcr.io/distroless/base

WORKDIR /app

COPY --from=build ["/app/server", "server"]
COPY --from=build ["/app/public", "./public"]

EXPOSE 3000

ENV NODE_ENV=production

CMD ["./server"]

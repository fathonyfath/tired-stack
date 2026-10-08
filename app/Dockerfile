# Wraps the binary that `./gradlew nativeCompile` builds; it needs glibc and zlib from the base image.
FROM debian:12-slim
COPY build/native/nativeCompile/app /app/tired-stack-sample
USER 65534
EXPOSE 3000
ENTRYPOINT ["/app/tired-stack-sample"]

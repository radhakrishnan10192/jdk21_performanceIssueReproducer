package org.reproducer;

import io.netty.handler.codec.http.QueryStringDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;
import java.util.Optional;

public class DownStreamService {

    public static DisposableServer disposableServer = null;

    public static void main(String[] arg) {
        startDownStreamService(8000);
    }

    public static void startDownStreamService(int port) {
        System.out.println("RN Server started successfully");

        HttpServer httpServer = HttpServer.create()
                .host("localhost")
                .port(port)
                .route(routes ->
                        routes.post("/downStream?delay={delay}", (req, resp) -> {
                            final Flux<String> baseRespBody = req.receive().asString();

                            final Flux<String> actualRespBody =
                                    Optional.ofNullable(new QueryStringDecoder(req.uri()).parameters())
                                            .flatMap(params -> Optional.ofNullable(params.get("delay")))
                                            .flatMap(list -> list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0)))
                                            .map(delayStr -> {
                                                final Duration delay = Duration.ofMillis(Integer.parseInt(delayStr));
                                                return Mono.delay(delay).flatMapMany(i -> baseRespBody);
                                            }).orElse(baseRespBody);

                            return copyHeaderIfExists(
                                    "Content-Length",
                                    req,
                                    copyHeaderIfExists("Content-Type", req, resp)
                            ).status(201).sendString(actualRespBody);
                        })
                ).maxKeepAliveRequests(100);
        disposableServer = httpServer.bindNow();
        disposableServer.onDispose().block();
    }


    private static HttpServerResponse copyHeaderIfExists(
            final String headerName,
            final HttpServerRequest req,
            final HttpServerResponse resp
    ) {
        return Optional.ofNullable(req.requestHeaders().get(headerName))
                .map(v -> resp.addHeader(headerName, v))
                .orElse(resp);
    }
}

package org.reproducer;


import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.jboss.resteasy.client.jaxrs.engines.ReactorNettyClientHttpEngine;
import org.jboss.resteasy.reactor.MonoRxInvoker;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    private static boolean isExecutable(String command, String[] args) {
        return args != null  && args.length > 0 && Arrays.stream(args).anyMatch(cc -> cc.startsWith(command));
    }

    public static void main(String[] args) throws Exception {
        if(isExecutable("downStreamServer", args)) {
            System.out.println("Starting RN downstream service");
            DownStreamService.startDownStreamService(8000);
        } else if(isExecutable("mainServer", args)) {
            System.out.println("Starting server");
            ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        } else {
            System.out.println("Starting RN downstream service");
            CompletableFuture.runAsync(() -> DownStreamService.startDownStreamService(8000));
            System.out.println("Starting server");
            Thread.sleep(2000);
            ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        }
    }

    @Path("/echo")
    @Component
    public class Echo {
        private WorstEverPojo worstEverPojo = new WorstEverPojo(Integer.parseInt(System.getProperty("dataSize", "100000")));

        private WebTarget target = getClient("http://localhost:8000").target("/downStream");

        private int numberOfCalls = Integer.parseInt(System.getProperty("callCount", "5"));
        private int callWait = Integer.parseInt(System.getProperty("callWait", "10"));

        /**
         * Simple get endpoint transforms ~100kb data to downstream service and get the response ~500KB
         * data in total and writes them in async mode
         * @return
         */
        @GET
        @Produces({MediaType.APPLICATION_JSON})
        public Mono<List<WorstEverPojo>> echo2() {
            return pojoList(numberOfCalls, Optional.ofNullable(callWait), r -> r.header("header", "header"));
        }

        private Mono<List<WorstEverPojo>> pojoList(
                final int numCalls,
                final Optional<Integer> delayMs,
                final UnaryOperator<Invocation.Builder> modifyReq
        ) {
            return mkCalls(numCalls, delayMs, modifyReq,
                    rx -> rx.post(Entity.json(worstEverPojo)))
                    .map(tup -> tup.getT2().readEntity(WorstEverPojo.class))
                    .collectList();
        }

        private Flux<Tuple2<Integer, Response>> mkCalls(
                final int numCalls,
                final Optional<Integer> delayMs,
                final UnaryOperator<Invocation.Builder> modifyReq,
                final Function<MonoRxInvoker, Mono<Response>> processRx
        ) {
            return Flux.range(0, numCalls)
                    .map(i -> Tuples.of(i, call(delayMs, modifyReq, processRx)))
                    .flatMap(t -> t.getT2().map(r -> Tuples.of(t.getT1(), r)))
                    .map(t -> {
                        final Response resp = t.getT2();
                        final int status = resp.getStatus();
                        if (status != 201) {
                            throw new RuntimeException("Got a " + status + " from downstream, expected a 201.");
                        }
                        return t;
                    });
        }

        private Mono<Response> call(
                final Optional<Integer> delayMs,
                final UnaryOperator<Invocation.Builder> modifyReq,
                final Function<MonoRxInvoker, Mono<Response>> processRx
        ) {
            final WebTarget actualTarget = target.queryParam("delay", delayMs.get());

            return processRx.apply(modifyReq.apply(actualTarget.request()).rx(MonoRxInvoker.class))
                    .map(r -> {
                        if (r.getStatus() != 201) {
                            throw new RuntimeException("Got a " + r.getStatus() + " from downstream, expected a 201.");
                        }
                        // how to stream?
                        return r;
                    });
        }
    }

    private static Client getClient(final String host) {
        ClientBuilder builder = ClientBuilder.newBuilder();
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("conn")
                .maxConnections(1000)
                .pendingAcquireTimeout(Duration.ofMillis(100))
                .maxIdleTime(Duration.ofMillis(60_000))
                .build();

        final ChannelGroup channelGroup = new DefaultChannelGroup(new DefaultEventExecutor());
        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .keepAlive(true)
                .baseUrl(host);

        ((ResteasyClientBuilder) builder).httpEngine(new ReactorNettyClientHttpEngine(
                httpClient,
                channelGroup,
                connectionProvider,
                Duration.ofMillis(10000),
                false));

        return builder.build();
    }
}

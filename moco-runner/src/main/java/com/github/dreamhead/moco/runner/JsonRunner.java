package com.github.dreamhead.moco.runner;

import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.MocoConfig;
import com.github.dreamhead.moco.internal.ActualHttpServer;
import com.github.dreamhead.moco.parser.HttpServerParser;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;

import static com.github.dreamhead.moco.Moco.*;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.toArray;

public class JsonRunner implements Runner {

    private final HttpServerParser httpServerParser = new HttpServerParser();
    private final StandaloneRunner runner = new StandaloneRunner();
    private final HttpServer httpServer;

    private JsonRunner(Iterable<? extends RunnerSetting> settings, Optional<Integer> port) {
        this.httpServer = createHttpServer(settings, port);
    }

    public void run() {
        runner.run(httpServer);
    }

    public void stop() {
        runner.stop();
    }

    private HttpServer createHttpServer(Iterable<? extends RunnerSetting> settings, Optional<Integer> port) {
        HttpServer server = createBaseHttpServer(settings, port);
        server.request(by(uri("/favicon.ico"))).response(with(pathResource("favicon.png")), header("Content-Type", "image/png"));
        return server;
    }

    private HttpServer createBaseHttpServer(Iterable<? extends RunnerSetting> settings, Optional<Integer> port) {
        HttpServer server = ActualHttpServer.createLogServer(port);

        for (RunnerSetting setting : settings) {
            HttpServer parsedServer = httpServerParser.parseServer(setting.getStream(), port, toConfigs(setting));
            server = mergeServer(server, parsedServer);
        }

        return server;
    }

    private MocoConfig[] toConfigs(RunnerSetting setting) {
        ImmutableList.Builder<MocoConfig> builder = ImmutableList.builder();

        addConfig(builder, setting.context());
        addConfig(builder, setting.fileRoot());
        addConfig(builder, setting.response());

        return toArray(builder.build(), MocoConfig.class);
    }

    private void addConfig(ImmutableList.Builder<MocoConfig> builder, Optional<MocoConfig> config) {
        if (config.isPresent()) {
            builder.add(config.get());
        }
    }

    private HttpServer mergeServer(HttpServer server, HttpServer parsedServer) {
        ActualHttpServer thisServer = (ActualHttpServer) server;
        return thisServer.mergeHttpServer((ActualHttpServer)parsedServer);
    }

    public static JsonRunner newJsonRunnerWithStreams(Iterable<? extends InputStream> streams, Optional<Integer> port) {
        return newJsonRunnerWithSetting(from(streams).transform(toRunnerSetting()), port);
    }

    private static Function<InputStream, RunnerSetting> toRunnerSetting() {
        return new Function<InputStream, RunnerSetting>() {
            @Override
            public RunnerSetting apply(InputStream input) {
                return new RunnerSetting(input, null, null, null);
            }
        };
    }

    public static JsonRunner newJsonRunnerWithSetting(Iterable<? extends RunnerSetting> settings, Optional<Integer> port) {
        return new JsonRunner(settings, port);
    }
}

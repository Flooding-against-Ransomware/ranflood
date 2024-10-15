package org.ranflood.daemon.binders;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.VersionCommand;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.commands.FloodCommandImpl;
import org.ranflood.daemon.commands.SnapshotCommandImpl;
import org.ranflood.daemon.commands.VersionCommandImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;
import static org.ranflood.daemon.utils.BindCmdToImpl.bindToImpl;

public class HttpServer {

    public static class CommandHandler implements HttpHandler {

        public CommandHandler() {}

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                UUID id = UUID.randomUUID();

                Ranflood.daemon().executeCommand(() -> {
                    log("HTTP server received [" + request + "]");
                    try {
                        Command< ? > command = bindToImpl( JSONTranscoder.fromJson( request ) );

                        if ( command.isAsync() ) {
                            Ranflood.daemon().executeCommand( () -> {
                                Object result = command.execute(id);
                                if ( result instanceof CommandResult.Successful ) {
                                    log( ( ( CommandResult.Successful ) result ).message());
                                } else {
                                    error( ( ( CommandResult.Failed ) result ).message());
                                }
                            } );
                            sendResponse( exchange, 200, "{\"id\": \"" + id.toString() + "\"}");
                        } else {
                            if ( command instanceof  VersionCommand.Get) {
                                String version = ( ( VersionCommandImpl.Get ) command ).execute(id);
                                sendResponse( exchange, 200, version);
                            }
                            else {
                                List< ? extends RanfloodType> l =
                                        ( command instanceof SnapshotCommand.List ) ?
                                                ( ( SnapshotCommandImpl.List ) command ).execute(id)
                                                : ( ( FloodCommandImpl.List ) command ).execute(id);
                                sendResponse( exchange, 200, JSONTranscoder.wrapListRanfloodType( l ));
                            }
                        }
                    } catch (ParseException e) {
                        error(e.getMessage());
                        try {
                            sendResponse(exchange, 400, JSONTranscoder.wrapError(e.getMessage()));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } catch (Exception e) {
                        error(e.getMessage());
                        try {
                            sendResponse(exchange, 500, e.getMessage());
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        private void handleCors(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}

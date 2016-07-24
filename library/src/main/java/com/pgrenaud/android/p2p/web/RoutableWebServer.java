package com.pgrenaud.android.p2p.web;

import java.util.UUID;
import java.util.regex.Pattern;

public class RoutableWebServer extends BasicWebServer {

    public static final Pattern FILE_REQUEST_URL_PATTERN = Pattern.compile("^/api/v1/file/([a-zA-Z0-9-]+)$");
    public static final Pattern PEER_POLLING_URL_PATTERN = Pattern.compile("^/api/v1/polling/([a-zA-Z0-9-]+)$");

    private final RequestHandler handler;

    public RoutableWebServer(int port, RequestHandler handler) {
        super(port);

        this.handler = handler;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();

        if (method.equals(Method.GET)) {
            if (uri.equals("/api/v1/debug")) {
                return sendDebug(session);
            } else if (uri.equals("/api/v1/error")) {
                return sendServerError("An error occurred.");
            } else if (uri.equals("/api/v1/empty")) {
                return sendEmpty();
            } else if (uri.equals("/api/v1/timeout")) {
                return sendTimeout();
            } else if (uri.equals("/api/v1/ping")) {
                return handlePing();
            } else if (uri.startsWith("/api/v1/polling") && PEER_POLLING_URL_PATTERN.matcher(uri).matches()) {
                return handlePolling(session);
            } else if (uri.equals("/api/v1/files")) {
                return handleFileList();
            } else if (uri.startsWith("/api/v1/file/") && FILE_REQUEST_URL_PATTERN.matcher(uri).matches()) {
                return handleFileRequest(session);
            } else {
                return sendError("Invalid URI (unknown API endpoint).");
            }
        } if (method.equals(Method.POST)) {
            if (uri.equals("/api/v1/debug")) {
                return sendDebug(session);
            } else {
                return sendError("Invalid URI (unknown API endpoint).");
            }
        } else {
            return sendError("Invalid HTTP method (expected GET or POST).");
        }
    }

    private Response handlePing() {
        return sendOk("pong");
    }

    private Response handlePolling(IHTTPSession session) {
        String param = getRouteParam(session, PEER_POLLING_URL_PATTERN, 1);

        if (param != null) {
            try {
                UUID uuid = UUID.fromString(param);

                return handler.handlePolling(uuid);
            } catch (IllegalArgumentException e) {
                return sendError("Invalid UUID.");
            }
        }

        return sendError("Invalid URI (UUID is missing).");
    }

    private Response handleFileList() {
        return handler.handleFileList();
    }

    private Response handleFileRequest(IHTTPSession session) {
        String param = getRouteParam(session, FILE_REQUEST_URL_PATTERN, 1);

        if (param != null) {
            try {
                UUID uuid = UUID.fromString(param);

                return handler.handleFileRequest(uuid);
            } catch (IllegalArgumentException e) {
                return sendError("Invalid UUID.");
            }
        }

        return sendError("Invalid URI (UUID is missing).");
    }
}

package com.pgrenaud.android.p2p.web;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pgrenaud.android.p2p.entity.EventEntity;

import fi.iki.elonen.NanoHTTPD;

public class BasicWebServer extends NanoHTTPD {

    public static final String MIME_JSON = "application/json";

    public BasicWebServer(int port) {
        super(port);
    }

    public BasicWebServer(String hostname, int port) {
        super(hostname, port);
    }

    public static Response sendOk(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, json.toString());
        } catch (JSONException e) {
            return sendServerError("SERVER INTERNAL ERROR: JSONException: " + e.getMessage());
        }
    }

    public static Response sendJSON(String json) {
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, json);
    }

    public static Response sendStream(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);

            return newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(file.getName()), fis, file.length());
        } catch (FileNotFoundException e) {
            return sendServerError("SERVER INTERNAL ERROR: FileNotFoundException: " + e.getMessage());
        }
    }

    public static Response sendEvent(EventEntity event) {
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, event.encode());
    }

    public static Response sendTimeout() {
        try {
            JSONObject json = new JSONObject();
            json.put("message", "Request timeout.");
            return newFixedLengthResponse(Response.Status.REQUEST_TIMEOUT, MIME_JSON, json.toString());
        } catch (JSONException e) {
            return sendServerError("SERVER INTERNAL ERROR: JSONException: " + e.getMessage());
        }
    }

    public static Response sendError(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_JSON, json.toString());
        } catch (JSONException e) {
            return sendServerError("SERVER INTERNAL ERROR: JSONException: " + e.getMessage());
        }
    }

    public static Response sendServerError(String message) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, message);
    }

    public static Response sendEmpty() {
        return newFixedLengthResponse(Response.Status.NO_CONTENT, null, null);
    }

    public static Response sendDebug(IHTTPSession session) {
        String body;
        try {
            body = getBody(session);
        } catch (IOException ioe) {
            return sendServerError("SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        } catch (ResponseException re) {
            return newFixedLengthResponse(re.getStatus(), NanoHTTPD.MIME_PLAINTEXT, re.getMessage());
        }

        String str = "";

        str += "Hostname: " + session.getRemoteHostName() + "\n";
        str += "IPv4Addr: " + session.getRemoteIpAddress() + "\n";

        if (isValidJson(body)) {
            str += "JSONBody: Body is a valid JSON string" + "\n";
        } else  {
            str += "JSONBody: Body is NOT a valid JSON string" + "\n";
        }

        str += "\n";
        str += session.getMethod() + " " + session.getUri()+ "\n";

        for (Map.Entry<String, String> header : session.getHeaders().entrySet()) {
            str += header.getKey() + ": " + header.getValue() + "\n";
        }

        str += "\n";
        str += body + "\n";

        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, str);
    }

    public static String getBody(IHTTPSession session) throws IOException, ResponseException {
        Method method = session.getMethod();
        Map<String, String> files = new HashMap<>();
        String body = "";

        if (method.equals(Method.PUT) || method.equals(Method.POST)) {
            session.parseBody(files);

            if (files.containsKey("postData")) {
                body = files.get("postData");
                Log.d("BasicWebServer", "body: " + body);
            }
        }

        return body;
    }

    public static boolean isValidJson(String str) {
        try {
            JSONObject json = new JSONObject(str);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static String getRouteParam(IHTTPSession session, Pattern pattern, int param) {
        Matcher m = pattern.matcher(session.getUri());

        if (m.find()) {
            if (param > 0 && param <= m.groupCount()) {
                return m.group(param);
            }
        }

        return null;
    }
}

package com.appdynamics.apmgame;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.StringReader;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonString;
import javax.json.JsonArray;

import java.util.List;

import java.util.concurrent.ThreadLocalRandom;

import javax.json.Json;
import javax.json.JsonReader;

import org.eclipse.jetty.client.HttpClient;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JavaNode extends AbstractHandler
{

protected JsonObject config;
protected JsonObject endpoints;

protected String processCall(String call) throws HttpException {
        if(call.startsWith("sleep")) {
                int timeout = Integer.parseInt(call.split(",")[1]);
                try {
                        Thread.sleep(timeout);
                } catch (InterruptedException e) {

                }
                return "Slept for " + timeout;
        }

        if(call.startsWith("http://")) {
          try {
          HttpClient client = new HttpClient();
          client.start();
          String response = client.GET(call).getContentAsString();
          client.stop();
          return response;
          } catch (Exception e) {
            return e.getMessage();
          }
        }

        if(call.startsWith("error")) {
                throw new HttpException(500, "error");
        }
        return ":" + call + " is not supported";
}

protected String preProcessCall(JsonValue call) throws HttpException {
        if(call.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray arr = (JsonArray) call;
                int index = ThreadLocalRandom.current().nextInt(arr.size());
                call = arr.get(index);
        }
        if(call.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = (JsonObject) call;
                double probability = obj.getJsonNumber("probability").doubleValue();
                call = obj.getJsonString("call");
                if(probability * 100 < ThreadLocalRandom.current().nextInt(100)) {
                    return call + " was not probable";
                }
        }
        return this.processCall(((JsonString)call).getString());
}

public void handleEndpoint(HttpServletResponse response, JsonArray endpoint) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);

        for (JsonValue entry : endpoint) {
                response.getWriter().println(this.preProcessCall(entry));
        }
}

public void handle(String target,
                   Request baseRequest,
                   HttpServletRequest request,
                   HttpServletResponse response)
throws IOException, ServletException
{
        String endpoint = request.getRequestURI().toString();

        response.setContentType("text/html;charset=utf-8");

        try {
                if(endpoints.containsKey(endpoint)) {
                        this.handleEndpoint(response, endpoints.getJsonArray(endpoint));
                } else if(endpoints.containsKey(endpoint.substring(1))) {
                        this.handleEndpoint(response, endpoints.getJsonArray(endpoint.substring(1)));
                } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().println(404);
                }
        } catch (HttpException e) {
                response.setStatus(e.getCode());
                response.getWriter().println(e.getMessage());
        }

        baseRequest.setHandled(true);

}

public JavaNode(JsonObject config) {
        this.config = config;
        this.endpoints = config.getJsonObject("endpoints").getJsonObject("http");
}

public static void main(String[] args) throws Exception
{

        int port = 8080;

        if(args.length > 0) {
                port = Integer.parseInt(args[0]);
        }

        JsonReader jsonReader = Json.createReader(new StringReader(System.getenv("APP_CONFIG")));
        JsonObject config = jsonReader.readObject();

        Server server = new Server(port);
        server.setHandler(new JavaNode(config));

        server.start();
        server.join();
}
}

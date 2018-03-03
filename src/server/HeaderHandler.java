package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeaderHandler implements HttpHandler {


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        headers.set("Content-Type", "text/plain");
        Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
        String response = "";
        for(Map.Entry<String, List<String>> entry : entries){
            response += entry.toString() + "\n";

        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}

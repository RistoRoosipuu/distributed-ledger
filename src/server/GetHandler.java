package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GetHandler implements HttpHandler {


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        URI requestedURI = exchange.getRequestURI();
        String query = requestedURI.getRawQuery();
        RootHandler.parseQuery(query,parameters);

        //send response
        String response = "";
        for (String key : parameters.keySet()){
            response += key + " = " + parameters.get(key) + "\n";
            System.out.println(response);
        }

        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();

    }
}

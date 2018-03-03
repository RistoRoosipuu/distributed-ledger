package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddrPostHandler implements HttpHandler {
    private Node peer;

    public AddrPostHandler(Node peer) {
        this.peer = peer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();
        RootHandler.parseQuery(query, parameters);

        //send response
        String response = "";
        for (String key : parameters.keySet()){
            response += key + " = " + parameters.get(key) + "\n";
            System.out.println(response);
            peer.getPeerSet().add(key);
            System.out.println("Key added");
        }
        exchange.sendResponseHeaders(200, response.length());
        System.out.println(parameters);
        System.out.println(query);
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}

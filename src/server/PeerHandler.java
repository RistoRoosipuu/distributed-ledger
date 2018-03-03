package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Just adds all IP's from Set to Server String Response.
 */
public class PeerHandler implements HttpHandler {

    private Node peer;

    public PeerHandler(Node peer) {
        this.peer = peer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "";

        for(String IP : peer.getPeerSet()){
            response += IP + "\n";
        }

        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());

        os.close();
    }
}

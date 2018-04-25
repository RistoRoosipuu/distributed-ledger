package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;

public class NodeInformationHandler implements HttpHandler {
    private Node node;

    public NodeInformationHandler(Node node) {
        this.node = node;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "<h1>" + node.getHostIP() + " account information </h1>" + "\n";
        response = response + "Public Key: " + node.getPublicKey() + "\n";
        response = response + "Account balance: " + node.getAccountBalance();


        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());

        os.close();
    }
}

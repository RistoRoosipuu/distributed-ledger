package server.transaction.transfer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;

public class TransactionSender implements HttpHandler {
    private Node node;

    public TransactionSender(Node node) {

        this.node = node;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Send Handler Works");

        String response = "<h1>A Transaction is being created and sent </h1>";

        this.node.sendTransaction();


        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();


    }
}

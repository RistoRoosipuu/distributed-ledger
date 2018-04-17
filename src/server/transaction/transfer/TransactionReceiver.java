package server.transaction.transfer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TransactionReceiver implements HttpHandler {

    private Node node;

    public TransactionReceiver(Node node) {
        this.node = node;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();


        System.out.println("TransactionReceiver Query: " + query);
        String response = "";

        try {
            if (this.node.checkIfTransactionExists(query)) {
                response = "From Handler: This peer already has this Transaction";
            } else {
                response = "From Handler: This peer didn't have this Transaction";
                this.node.sendTransactionToAllPeers(query.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

}

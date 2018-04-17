package server.transaction.transfer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class TransactionSender implements HttpHandler {
    private Node node;

    public TransactionSender(Node node) {

        this.node = node;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Send Handler Works");
        URI requestedURI = exchange.getRequestURI();
        String query = requestedURI.getRawQuery();

        //send response
        String response;
        if (query == null) {
            response = "Please input the public key of the person receiving the transaction and its sum";
        } else {
            String[] hashGetKeyValue = query.split("=");
            response = "<h1>A Transaction is being created and sent </h1>";
            try {
                this.node.sendTransaction(hashGetKeyValue[0], hashGetKeyValue[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();


        }
    }

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
        String response = "";
        if (query == null) {
            response = "Please input the public key of the person receiving the transaction and its sum";
        } else {
            String[] hashGetKeyValue = query.split("=");
            String receiverKey = hashGetKeyValue[0];
            String sum = hashGetKeyValue[1];


            try {
                if (receiverKey.equals(this.node.getPublicKey())) {
                    response = "<h1>Please use 0 as the public key if you wish to send to yourself</h1>";
                } else if ((receiverKey.equals("0"))) {
                    response = "<h1>A Transaction is being created and sent to yourself!!! </h1>";
                    this.node.sendTransaction(receiverKey, sum);
                } else {
                    if (this.node.checkIfTransactionSumIsValid(Integer.parseInt(sum))) {
                        response = "<h1>A Transaction is being created and sent </h1>";
                        this.node.sendTransaction(receiverKey, sum);
                    } else {
                        response = "<h1>The sum is more than what the account holds</h1>";
                    }

                }
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("Could not send Transaction to known Peer");
            }


            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        }
    }
}

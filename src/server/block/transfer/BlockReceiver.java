package server.block.transfer;

import block.Block;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BlockReceiver implements HttpHandler {

    private Node node;
    private Gson gson = new Gson();

    public BlockReceiver(Node node) {
        this.node = node;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();
        //Consider moving gson to Node
        Block receivedBlock = gson.fromJson(query, Block.class);

        System.out.println("BlockReceiver Query: " + query);
        String response;
        if (this.node.checkIfBlockExists(receivedBlock)) {
            response = "From Handler: This peer already has this block";
        } else {
            response = "From Handler: This peer didn't have this block";
            this.node.sendMessageToAllPeers(query.getBytes(StandardCharsets.UTF_8));
        }


        //!DOES NOT REACH HERE
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        //Placing this after close kind of helps, but not 100%
        //this.node.sendMessageToAllPeers(query.getBytes(StandardCharsets.UTF_8));
    }
}

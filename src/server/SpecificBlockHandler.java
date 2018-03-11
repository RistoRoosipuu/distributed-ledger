package server;

import block.Block;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class SpecificBlockHandler implements HttpHandler {
    private Node node;
    public SpecificBlockHandler(Node node) {
        this.node = node;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI requestedURI = exchange.getRequestURI();
        String query = requestedURI.getRawQuery();

        //send response
        String response = "";
        if (query == null) {
            response += "Please choose a specific Block: getdata?blockId=specificHash";
        } else {
            //It can't get a response if the get URL is screwed up
            String[] hashGetKeyValue = query.split("=");

            if (containsName(node.getKnownBlocks(), hashGetKeyValue[1])) {
                //Streaming twice?
                Optional<Block> blockOptional = node.getKnownBlocks().stream().filter(block1 -> block1.hash().equals(hashGetKeyValue[1])).findFirst();
                Block block = blockOptional.get();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(block);

                response += json;
            } else {
                response += "No such Hash found";
            }


        }


        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public boolean containsName(List<Block> list, String hash) {
        return list.stream().filter(block -> block.hash().equals(hash)).findFirst().isPresent();
    }
}

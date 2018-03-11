package server;

import block.Block;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import node.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

public class BlockHandler implements HttpHandler {
    private Node node;

    public BlockHandler(Node node) {
        this.node = node;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI requestedURI = exchange.getRequestURI();
        String query = requestedURI.getRawQuery();

        //send response
        String response = "";
        if (query == null) {
            for (Block block : node.getKnownBlocks()) {
                response += block.hash() + "\n";
            }
        } else {
            String[] hashGetKeyValue = query.split("=");

            if (containsName(node.getKnownBlocks(), hashGetKeyValue[1])) {
                int index = IntStream.range(0, node.getKnownBlocks().size())
                        .filter(block -> node.getKnownBlocks().get(block).hash().equals(hashGetKeyValue[1]))
                        .findFirst().getAsInt();

                for (int i = index; i < node.getKnownBlocks().size(); i++) {
                    response += node.getKnownBlocks().get(i).hash() + "\n";
                }
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

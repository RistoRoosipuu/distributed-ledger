import block.Block;
import block.Transaction;
import com.google.gson.Gson;
import node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        // write your code here
        Scanner reader = new Scanner(System.in);
        System.out.println("If you wish to start a Standalone Network, type y");
        System.out.println("If you wish to start a Network that connects to a specific IP, type n");
        String answer = reader.next();

        if (answer.equals("y")) {
            System.out.println("Stand alone network. Enter port");
            int port = reader.nextInt();

            reader.close();

            new Node(port);
        } else if (answer.equals("n")) {
            System.out.println("Please enter the URL(Local) you want to connect to: x.x.x.x:yyyy");
            String url = reader.next();
            System.out.println("Please enter your port");
            int port = reader.nextInt();

            reader.close();

            new Node(url, port);
        }
    }
}

import node.Node;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        // write your code here


        //Block genesisBlock = new Block("Hi im the first block", "0");
        //System.out.println("Hash for block 1: " + genesisBlock.getHash());

        //Block secondBlock = new Block("Yo im the second block", genesisBlock.getHash());
        //System.out.println("Hash for block 2: "  + secondBlock.getHash());

        //Block thirdBlock = new Block("I am the third block", secondBlock.getHash());
        //System.out.println("Hash for block 3: " + thirdBlock.getHash());
        Scanner reader = new Scanner(System.in);
        System.out.println("Would you like to start a Standalone Network or would you like to " +
                "connect to a specific IP/Port:");
        System.out.println("Write y for Standalone OR n for global connection");

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

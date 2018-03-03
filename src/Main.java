import node.Node;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        // write your code here
        Scanner reader = new Scanner(System.in);
        System.out.println("Would you like to start a Network y/n:");
        String answer = reader.next();

        if (answer.equals("y")) {
            System.out.println("Stand alone network. Enter port");
            int port = reader.nextInt();

            reader.close();

            new Node(port);
        } else if (answer.equals("n")) {
            //System.out.println("URL is currently hard-coded into the Node class. Write whatever");
            System.out.println("Please enter the URL(Local) you want to connect to: x.x.x.x:yyyy");
            String url = reader.next();
            System.out.println("Please enter your port");
            int port = reader.nextInt();

            reader.close();

            new Node(url, port);
        }
    }
}

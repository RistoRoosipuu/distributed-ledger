package node;

import com.sun.net.httpserver.HttpServer;
import server.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Node {

    private int port;
    private String hostIP;
    private Set<String> peerSet;
    // Can't use Global IP without Port Forwarding.
    private String testIp = "192.168.1.121:9000";
    private ScheduledExecutorService refreshPeersExecutor = Executors.newSingleThreadScheduledExecutor();
    private boolean canRun = true;
    private InetAddress localAddr = InetAddress.getLocalHost();

    public Node(int port) throws IOException {
        this.port = port;
        this.peerSet = new HashSet<>();

        System.out.println("Stand alone Peer");


        connectingInternally();
        //findNodeHostIPAddress();

        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        refreshPeerList();

    }

    public Node(String peerAddr, int port) throws Exception {
        this.port = port;
        this.peerSet = new HashSet<>();

        System.out.println("Peer thats trying to connect when started");


        //findNodeHostIPAddress();

        connectingInternally();


        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        connectToServer(peerAddr);

        refreshPeerList();

    }

    private void connectingInternally() {

        hostIP = localAddr.getHostAddress() + ":" + this.port;

        peerSet.add(hostIP);
    }

    private void refreshPeerList() {

        refreshPeersExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("Refrehing peers for: " + hostIP);

                for (String peerUrl : peerSet) {
                    //Prevent it from connecting to itself
                    if (!peerUrl.equals(hostIP)) {
                        try {
                            connectToServer(peerUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println("After connecting to known Peers for " + hostIP);
                System.out.println("Refreshed PeerList is: " + peerSet);

                //restart the executioner here
                refreshPeerList();
            }
        }, 30, TimeUnit.SECONDS);


    }
    /*
     NB connecToServer and Post (down below) have different type of URL Connections
     */

    private void connectToServer(String url) throws Exception {
        System.out.println(this.hostIP + " Trying to connect to: " + url);

        //System.out.println(this.port + " known PeerSet: " + peerSet);

        URL oracle;
        try {
            oracle = new URL("http://" + url + "/getPeers");

            sendPost(url);


            URLConnection yc = oracle.openConnection();
            yc.setReadTimeout(10000);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    yc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                //System.out.println(inputLine);
                peerSet.add(inputLine);
            in.close();
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Unable to connect to given URL " + url + ". Please check the validity.");
        }


    }


    public void startNodeClientAndServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Server started at " + server.getAddress());
            //InetAddress i = InetAddress.getLocalHost();
            System.out.println(localAddr.getHostAddress());
            server.createContext("/", new RootHandler());
            server.createContext("/echoHeader", new HeaderHandler());
            server.createContext("/echoGet", new GetHandler());
            server.createContext("/echoPost", new PostHandler());
            server.createContext("/getPeers", new PeerHandler(this));
            server.createContext("/postIP", new AddrPostHandler(this));
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void findNodeHostIPAddress() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            hostIP = inputLine;
        System.out.println("This Host IP: " + hostIP + " and port: " + port);
        in.close();

        //add this specific Node's IP and Port to known Peer Set
        this.hostIP = hostIP + ":" + port;

        getPeerSet().add(this.hostIP);
    }

    /**
     * Open Static known Peer file and add them to a Set
     */
    public void populatePeerSetFromStaticFile() {
        //Get the file reference
        //NB! Refactor this to find it within the Node or another package
        Path path = Paths.get("C:\\Users\\Risto\\IdeaProjects\\Distributed_Ledger\\src\\node\\staticPeerList.txt");

        //Add to Set
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> peerSet.add(line));
        } catch (IOException e) {
            //error happened
        }

        System.out.println(peerSet);
    }

    public synchronized Set<String> getPeerSet() {
        return peerSet;
    }

    /*
    Connect to an URL and open up /echoPost
    Send connecting clients ip as POST
    Then AddrPostHandler will receive the Post content(currently only the localAdds and adds it
    to the Server peerlist
    Then sends back a response,
    We return to the sendPost method, and it finishes.
     */
    // HTTP POST request
    private void sendPost(String addr) throws Exception {

        String url = "http://" + addr + "/postIP";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParameters = this.hostIP;

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);
        //this.peerSet.add(urlParameters);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result

        System.out.println("Response? " + response.toString());

    }


}

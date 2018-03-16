package node;

import block.Block;
import block.Transaction;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import server.*;
import server.block.transfer.BlockReceiver;
import server.block.transfer.BlockSender;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Node {

    private int port;
    private String hostIP;
    private Set<String> peerSet;
    private ScheduledExecutorService refreshPeersExecutor = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService serverExecutor = Executors.newFixedThreadPool(5);
    //cachedTP can be dangerous
    //private ExecutorService connectionThreads = Executors.newCachedThreadPool();

    private InetAddress localAddr = InetAddress.getLocalHost();
    private List<Transaction> knownTransactions = new ArrayList<>();
    private List<Block> knownBlocks = Collections.synchronizedList(new ArrayList<>());
    //NB! Remember to remove Gson from BlockReceiver
    private Gson gson = new Gson();

    public Node(int port) throws IOException {
        this.port = port;
        this.peerSet = new HashSet<>();

        System.out.println("Stand alone Peer");


        connectingInternally();
        //findNodeHostIPAddress();

        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        getBlocksFromHardCopy();
        //Shutting executor down for Block transfer testing
        //refreshPeerList();


        System.out.println(getKnownBlocks());


    }

    public Node(String peerAddr, int port) throws Exception {
        this.port = port;
        this.peerSet = new HashSet<>();

        System.out.println("Peer thats trying to connect when started");


        //findNodeHostIPAddress();

        connectingInternally();


        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        getBlocksFromHardCopy();

        connectToServer(peerAddr);
        ////Shutting executor down for Block transfer testing
        //refreshPeerList();

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

    /**
     * REWRITE TO URL CONNECTION????????++
     */

    private void connectToServer(String url) throws Exception {
        System.out.println(this.hostIP + " Trying to connect to: " + url);

        //System.out.println(this.port + " known PeerSet: " + peerSet);

        URL oracle;
        try {
            oracle = new URL("http://" + url + "/getPeers");

            //sendPost(url);


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

    /**
     * DELETE AND REMOVE UNUSED HANDLERS
     */
    public void startNodeClientAndServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Server started at " + server.getAddress());
            //InetAddress i = InetAddress.getLocalHost();
            System.out.println(localAddr.getHostAddress());
            server.createContext("/", new RootHandler());
            //server.createContext("/echoHeader", new HeaderHandler());
            //server.createContext("/echoGet", new GetHandler());
            //server.createContext("/echoPost", new PostHandler());
            server.createContext("/getPeers", new PeerHandler(this));
            //Maybe i should not create a secondary Context here and just throw the IP into getPeers
            //Validate with regex or something
            server.createContext("/postIP", new AddrPostHandler(this));
            server.createContext("/getblocks", new BlockHandler(this));
            server.createContext("/getdata", new SpecificBlockHandler(this));
            server.createContext("/block", new BlockSender(this));
            server.createContext("/receiveBlock", new BlockReceiver(this));
            server.setExecutor(serverExecutor);
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
        Path path = Paths.get("src\\node\\staticPeerList.txt");

        //Add to Set
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> peerSet.add(line));
        } catch (IOException e) {
            //error happened
        }

        System.out.println(peerSet);
    }

    public Set<String> getPeerSet() {
        return peerSet;
    }

    /**
     * REFACTOR TO A SINGLE POST METHOD
     */
    // HTTP POST request
    private void sendPost(String addr) throws Exception {

        String url = "http://" + addr + "/postIP";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
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


    private void getBlocksFromHardCopy() {

        Path path = Paths.get("src\\node\\copyOfBlocks.txt");
        System.out.println("Block method");
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(this::convertToObjectAndAddToList);
        } catch (IOException e) {
            //error happened
        }

    }

    private void convertToObjectAndAddToList(String line) {
        Block convertedFromJson = gson.fromJson(line, Block.class);
        this.getKnownBlocks().add(convertedFromJson);
        System.out.println(line);
        System.out.println(convertedFromJson);
    }


    public List<Block> getKnownBlocks() {
        return knownBlocks;
    }

    public void sendBlock() throws IOException {
        System.out.println("SEND BLOCK METHOD");
        byte[] message = createBlock().getBytes(StandardCharsets.UTF_8);
        sendMessageToAllPeers(message);

    }

    //If this is public, would that be bad?
    public void sendMessageToAllPeers(byte[] message) throws IOException {
        System.out.println("SEND ALL MESSAGE METHOD");
        for (String peer : getPeerSet()) {
            if (!peer.equals(hostIP)) {

                postData(peer, message);
                /**
                 connectionThreads.execute(new Runnable() {
                @Override public void run() {
                try {
                postData(peer, message);
                } catch (IOException e) {
                e.printStackTrace();
                }
                }
                });

                 **/
                /**
                 new Thread(() -> {
                 try {
                 postData(peer, message);
                 } catch (IOException e) {
                 e.printStackTrace();
                 }
                 }).start();

                 **/
            }

        }
    }

    //Dummy data
    private String createBlock() {
        System.out.println("CREATE STRING METHOD");
        //So, Transaction shouldn't have a previous hash right? I'm thinking of using the Factory pattern
        Transaction secondBlock = new Transaction("0", "hello whats my name");
        Transaction thirdBlock = new Transaction("1", "last Transaction");
        List<Transaction> list2 = new ArrayList<>();
        list2.add(secondBlock);
        list2.add(thirdBlock);
        //Way too long, method?
        //Get the last Blocks hash = prevHash; list content always the same
        Block block = new Block(getKnownBlocks().get(getKnownBlocks().size() - 1).hash(), list2);
        Gson gson = new Gson();
        String blockAsJsonString = gson.toJson(block);

        this.getKnownBlocks().add(block);


        return blockAsJsonString;
    }

    /**
     * Note to Self: Get rid of the other(the ip one) Post function, we can combine them into one.
     */
    private void postData(String url, byte[] message) throws IOException {
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            System.out.println("Trying to Open Connection");
            connection = (HttpURLConnection) new URL("http://" + url + "/receiveBlock").openConnection();
            System.out.println(connection.getURL());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);


            out = connection.getOutputStream();
            out.write(message);
            out.close();

            in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            in.close();
        } finally {
            if (connection != null) connection.disconnect();
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }


    public boolean checkIfBlockExists(Block block) throws IOException {
        if (this.getKnownBlocks().contains(block)) {
            System.out.println("Node(" + this.hostIP + "): Contains it: " + this.getKnownBlocks());
            return true;
        } else {
            System.out.println("Node(" + this.hostIP + "): Don't have this one");
            this.getKnownBlocks().add(block);
            System.out.println("Added it: " + this.getKnownBlocks());
            //this.sendMessageToAllPeers("testo".getBytes(StandardCharsets.UTF_8));

            return false;
        }
    }
}

package node;

import block.Block;
import block.Transaction;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import encryption.PublicPrivateGenerator;
import server.*;
import server.block.transfer.BlockReceiver;
import server.block.transfer.BlockSender;
import server.transaction.transfer.TransactionReceiver;
import server.transaction.transfer.TransactionSender;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Node {

    private int port;
    private String hostIP;
    private Set<String> peerSet = Collections.synchronizedSet(new HashSet<>());
    private ScheduledExecutorService refreshPeersExecutor = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService serverExecutor = Executors.newFixedThreadPool(10);
    private InetAddress localAddr = InetAddress.getLocalHost();
    private List<Transaction> knownTransactions = Collections.synchronizedList(new ArrayList<>());
    private List<Block> knownBlocks = Collections.synchronizedList(new ArrayList<>());
    //NB! Remember to remove Gson from BlockReceiver
    private Gson gson = new Gson();
    private PublicPrivateGenerator generator = new PublicPrivateGenerator();


    public Node(int port) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        System.out.println("Stand alone Peer");

        connectingInternally();

        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        getBlocksFromHardCopy();

        displayPublicKeyString();

        refreshPeerList();

    }


    public Node(String peerAddr, int port) throws Exception {
        this.port = port;

        System.out.println("This peer connects to a specific IP");

        connectingInternally();

        startNodeClientAndServer(port);

        populatePeerSetFromStaticFile();

        getBlocksFromHardCopy();

        connectToServer(peerAddr);


        refreshPeerList();

    }

    private void connectingInternally() {

        hostIP = localAddr.getHostAddress() + ":" + this.port;

        peerSet.add(hostIP);
    }

    private void refreshPeerList() {
        refreshPeersExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                try {
                    System.out.println("Refreshing peers for: " + hostIP);
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

                } catch (Exception e) {
                    System.out.println("Something bad happened");
                    refreshPeerList();
                }

            }
        }, 30, 30, TimeUnit.SECONDS);

    }

    private void connectToServer(String url) {
        System.out.println(this.hostIP + " Trying to connect to: " + url);


        URL oracle;
        try {
            oracle = new URL("http://" + url + "/getPeers");
            String postIPAddress = "http://" + url + "/postIP";

            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    yc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                peerSet.add(inputLine);
            in.close();
            postData(postIPAddress, this.getHostIP().getBytes(StandardCharsets.UTF_8));
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
            System.out.println(localAddr.getHostAddress());
            server.createContext("/", new RootHandler());
            server.createContext("/getPeers", new PeerHandler(this));
            server.createContext("/postIP", new AddrPostHandler(this));
            server.createContext("/getblocks", new BlockHandler(this));
            server.createContext("/getdata", new SpecificBlockHandler(this));
            server.createContext("/block", new BlockSender(this));
            server.createContext("/receiveBlock", new BlockReceiver(this));
            server.createContext("/transaction", new TransactionSender(this));
            server.createContext("/receiveTransaction", new TransactionReceiver(this));
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
        sendBlockToAllPeers(message);

    }

    //If this is public, would that be bad?
    public void sendBlockToAllPeers(byte[] message) {
        System.out.println("SEND ALL MESSAGE METHOD");
        for (String peer : getPeerSet()) {
            if (!peer.equals(hostIP)) {


                try {
                    postData("http://" + peer + "/receiveBlock", message);
                } catch (IOException e) {
                    //e.printStackTrace();
                    //If connection fails, remove Peer from Set
                    this.peerSet.remove(peer);
                }

            }

        }
    }

    //Dummy data
    private String createBlock() throws IOException {
        System.out.println("CREATE STRING METHOD");

        Block block = new Block(getKnownBlocks().get(getKnownBlocks().size() - 1).hash(), this.getKnownTransactions());
        String blockAsJsonString = gson.toJson(block);

        this.getKnownBlocks().add(block);

        checkIfFileContainsBlockJson(blockAsJsonString);


        return blockAsJsonString;
    }


    private void postData(String url, byte[] message) throws IOException {
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            System.out.println("Trying to Open Connection");
            connection = (HttpURLConnection) new URL(url).openConnection();
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


    public boolean checkIfBlockExists(Block block) {
        if (this.getKnownBlocks().contains(block)) {
            System.out.println("Node(" + this.hostIP + "): Contains it: " + this.getKnownBlocks());
            return true;
        } else {
            System.out.println("Node(" + this.hostIP + "): Don't have this one");
            this.getKnownBlocks().add(block);
            System.out.println("Added it: " + this.getKnownBlocks());

            return false;
        }
    }

    public String getHostIP() {
        return hostIP;
    }

    public List<Transaction> getKnownTransactions() {
        return knownTransactions;
    }

    //Could just combine Block and Transaction sending.
    public void sendTransaction(String receiver, String amount) throws Exception {
        System.out.println("SEND Transaction METHOD");
        byte[] message = createTransaction(receiver, amount).getBytes(StandardCharsets.UTF_8);
        sendTransactionToAllPeers(message);
    }

    private String createTransaction(String receiver, String amount) throws Exception {

        Transaction transaction = Transaction.getNewTransaction(this.getKnownTransactions(),
                generator.getStringFromPublicKey(generator.getPublicKey()),
                receiver, amount);

        String signature = generator.encrypt(generator.getPrivateKey(), transaction.hash());

        transaction.setSignature(signature);

        String transactionString = gson.toJson(transaction);

        this.getKnownTransactions().add(transaction);

        return transactionString;
    }

    public void sendTransactionToAllPeers(byte[] message) {
        System.out.println("SEND ALL MESSAGE METHOD");
        for (String peer : getPeerSet()) {
            if (!peer.equals(hostIP)) {

                try {
                    postData("http://" + peer + "/receiveTransaction", message);
                } catch (IOException e) {
                    //e.printStackTrace();
                    //If connection fails, remove Peer from Set
                    this.peerSet.remove(peer);
                }

            }

        }
    }

    public synchronized boolean checkIfTransactionExists(String transactionAsJson) throws Exception {

        Transaction transaction = gson.fromJson(transactionAsJson, Transaction.class);
        if (this.getKnownTransactions().contains(transaction)) {
            System.out.println("Node(" + this.hostIP + "): Contains it: " + this.getKnownTransactions());
            return true;
        } else {
            System.out.println("Node(" + this.hostIP + "): Don't have this one");
            this.getKnownTransactions().add(transaction);
            System.out.println("Added it: " + this.getKnownTransactions());


            return false;
        }
    }

    public boolean checkIfSignatureIsValid(Transaction transaction) throws Exception {
        String hash = transaction.hash();
        PublicKey publicKey = generator.getPublicKeyFromString(transaction.getFromPublicKey());
        String decryptedString = generator.decrypt(publicKey, transaction.getSignature());

        if (hash.equals(decryptedString)) {
            return true;
        } else {
            return false;
        }
    }

    public void checkIfFileContainsBlockJson(String blockAsJson) throws IOException {
        Path path = Paths.get("src\\node\\copyOfBlocks.txt");

        try (Stream<String> lines = Files.lines(path)) {
            Optional<String> hasBlock = lines.filter(s -> s.equals(blockAsJson)).findFirst();
            if (hasBlock.isPresent()) {
                System.out.println("Block is already in the file");
            } else {
                writeJsonToFile(path, blockAsJson);
            }
        }

    }

    private void writeJsonToFile(Path path, String blockString) {

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(String.valueOf(path), true);
            BufferedWriter bufferFileWriter = new BufferedWriter(fileWriter);
            bufferFileWriter.append(blockString);
            bufferFileWriter.newLine();
            bufferFileWriter.close();
        } catch (IOException ex) {
            System.out.println("Error writing into File");
        }
    }

    public void displayPublicKeyString() {
        try {
            String publicKey = generator.getStringFromPublicKey(generator.getPublicKey());
            System.out.println("This Node: " + publicKey);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


}

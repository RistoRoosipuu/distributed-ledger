package node;

import block.Block;
import block.Transaction;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import encryption.PublicPrivateGenerator;
import hashing.HashToHex;
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
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Node {

    private int port;
    private String hostIP;
    private Set<String> peerSet = Collections.synchronizedSet(new HashSet<>());
    private ScheduledExecutorService refreshPeersExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService mineBlock = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService serverExecutor = Executors.newFixedThreadPool(10);
    private InetAddress localAddr = InetAddress.getLocalHost();
    private List<Transaction> knownTransactions = Collections.synchronizedList(new ArrayList<>());
    private List<Block> knownBlocks = Collections.synchronizedList(new ArrayList<>());
    //NB! Remember to remove Gson from BlockReceiver
    private Gson gson = new Gson();
    private PublicPrivateGenerator generator = new PublicPrivateGenerator();
    private List<Transaction> unUsedTransactions = Collections.synchronizedList(new ArrayList<>());
    private String publicKey;
    private int accountBalance;
    private List<Block> blocksThatShouldBeRemoved = Collections.synchronizedList(new ArrayList<>());

    public Node(int port) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        this.accountBalance = 100;
        System.out.println("Stand alone Peer");
        connectingInternally();
        startNodeClientAndServer(port);
        populatePeerSetFromStaticFile();
        getBlocksFromHardCopy();
        displayPublicKeyString();
        refreshPeerList();
        mineBlock();
        findAndChooseCorrectBlocks();
    }

    public Node(String peerAddr, int port) throws Exception {
        this.port = port;
        this.accountBalance = 100;
        System.out.println("This peer connects to a specific IP");
        connectingInternally();
        startNodeClientAndServer(port);
        populatePeerSetFromStaticFile();
        getBlocksFromHardCopy();
        connectToServer(peerAddr);
        refreshPeerList();
        mineBlock();
        findAndChooseCorrectBlocks();
    }

    private void connectingInternally() {
        hostIP = localAddr.getHostAddress() + ":" + this.port;
        System.out.println("HostLocation: " + hostIP);
        peerSet.add(hostIP);
    }

    private void mineBlock() {
        mineBlock.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                findUnUsedTransactions();

                if (!unUsedTransactions.isEmpty()) {
                    try {
                        sendBlock();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }, 10, 10, TimeUnit.MINUTES);

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
            server.createContext("/account", new NodeInformationHandler(this));
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
        //this.hostIP = hostIP + ":" + port;
        //getPeerSet().add(this.hostIP);
    }

    /**
     * Open Static known Peer file and add them to a Set
     */
    public void populatePeerSetFromStaticFile() {
        Path path = Paths.get("src\\node\\staticPeerList.txt");
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

    private String createBlock() throws IOException {
        System.out.println("CREATE STRING METHOD");
        Block block = new Block(getKnownBlocks().get(getKnownBlocks().size() - 1).hash(), this.unUsedTransactions);
        block.setCount(this.unUsedTransactions.size());
        int nextNumber = this.getKnownBlocks().get(getKnownBlocks().size() - 1).getNumber();
        block.setNumber(nextNumber + 1);

        boolean isNonceValid = false;
        while (!isNonceValid) {
            String nonce = generateNonceString(5, "abcdefghijklmnopqrstyvwxyz");
            String hashAndNonce = block.hash() + nonce;
            String hashOfNonce = generateNonceHash(hashAndNonce);
            String subStringOfHash = hashOfNonce.substring(0, 4);
            if (subStringOfHash.equals("0000")) {
                isNonceValid = true;
                System.out.println("VALID!!!!!!!!!!!!!!");
                System.out.println("Nonce is: " + nonce);
                System.out.println("Nonce Hash is: " + hashOfNonce);
                block.setCreator(this.getPublicKey());
                block.setNonce(nonce);
                block.setHash(hashOfNonce);
            }
        }
        block.setMerkle_root(findMerkleStringRoot(unUsedTransactions));
        String blockAsJsonString = gson.toJson(block);
        this.unUsedTransactions.clear();
        this.getKnownBlocks().add(block);
        this.removeTheseTransactions(block);
        checkIfFileContainsBlockJson(blockAsJsonString);
        return blockAsJsonString;
    }

    private String findMerkleStringRoot(List<Transaction> transactions) {
        String result = "";
        boolean mustStillCombine = true;
        List<String> tempList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            tempList.add(gson.toJson(transaction));
        }
        while (mustStillCombine) {
            if (tempList.size() == 1) {
                result = onlyOneTransaction(tempList.get(0));
                mustStillCombine = false;
            } else if (tempList.size() % 2 == 0) {
                tempList = evenNumberOfTransactions(tempList);
            } else {
                tempList = oddNumberOfTransactions(tempList);
            }
        }
        return result;
    }

    private String onlyOneTransaction(String transactionHash) {
        return HashToHex.sha256ToHex(transactionHash + transactionHash);
    }

    private List<String> evenNumberOfTransactions(List<String> tempList) {
        List<String> evenPairs = new ArrayList<>();
        for (int i = 0; i < tempList.size(); i = i + 2) {
            String firstPair = tempList.get(i);
            String secondPair = tempList.get(i + 1);
            evenPairs.add(HashToHex.sha256ToHex(firstPair + secondPair));
        }
        return evenPairs;
    }

    private List<String> oddNumberOfTransactions(List<String> tempList) {
        List<String> allPairs = new ArrayList<>();
        List<String> oddStringsResult;
        for (int i = 0; i < tempList.size() - 1; i++) {
            allPairs.add(tempList.get(i));
        }
        oddStringsResult = evenNumberOfTransactions(allPairs);
        oddStringsResult.add(onlyOneTransaction(tempList.get(tempList.size() - 1)));
        return oddStringsResult;
    }

    private String generateNonceString(int length, String from) {
        List<Character> temp = from.chars()
                .mapToObj(i -> (char) i)
                .collect(Collectors.toList());
        Collections.shuffle(temp, new SecureRandom());
        return temp.stream()
                .map(Object::toString)
                .limit(length)
                .collect(Collectors.joining());
    }

    private String generateNonceHash(String nonce) {
        return HashToHex.sha256ToHex(nonce);
    }

    public void findUnUsedTransactions() {
        boolean canAdd;
        for (Transaction transaction : knownTransactions) {
            canAdd = true;
            for (Block block : getKnownBlocks()) {
                if (block.getTransactions().contains(transaction)) {
                    canAdd = false;
                    break;
                }
            }
            if (canAdd) {
                unUsedTransactions.add(transaction);
            }
        }
        System.out.println("All: " + knownTransactions);
        System.out.println("Unused: " + unUsedTransactions);
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
            this.removeTheseTransactions(block);
            System.out.println("Added it: " + this.getKnownBlocks());
            return false;
        }
    }

    private void removeTheseTransactions(Block block) {
        for (Transaction transaction : block.getTransactions()) {
            this.getKnownTransactions().remove(transaction);
        }
        System.out.println("Removed Used Transactions");
    }

    public String getHostIP() {
        return hostIP;
    }

    public List<Transaction> getKnownTransactions() {
        return knownTransactions;
    }

    public void sendTransaction(String receiver, String amount) throws Exception {
        System.out.println("SEND Transaction METHOD");
        byte[] message = createTransaction(receiver, amount).getBytes(StandardCharsets.UTF_8);
        sendTransactionToAllPeers(message);
    }

    private String createTransaction(String receiver, String amount) throws Exception {
        Transaction transaction = Transaction.getNewTransaction(this.getKnownTransactions(),
                generator.getStringFromPublicKey(generator.getPublicKey()),
                receiver, amount);
        if (!amount.equals("0")) {
            this.accountBalance = this.accountBalance - Integer.parseInt(amount);
        }

        String signature = generator.encrypt(generator.getPrivateKey(), transaction.hash());

        transaction.setSignature(signature);

        String transactionString = gson.toJson(transaction);

        this.getKnownTransactions().add(transaction);
        System.out.println("CreateTrans: " + this.accountBalance);
        return transactionString;
    }

    public void sendTransactionToAllPeers(byte[] message) {
        System.out.println("SEND ALL MESSAGE METHOD");
        for (String peer : getPeerSet()) {
            if (!peer.equals(hostIP)) {
                try {
                    postData("http://" + peer + "/receiveTransaction", message);
                } catch (IOException e) {
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

            dealWithNewTransaction(transaction);

            return false;
        }
    }

    public void dealWithNewTransaction(Transaction transaction) throws Exception {
        if (checkIfSignatureIsValid(transaction)) {
            this.getKnownTransactions().add(transaction);
            System.out.println("Added it: " + this.getKnownTransactions());
            if (transaction.getToPublicKey().equals(this.getPublicKey())) {
                System.out.println("Correct Receiver " + this.port);
                System.out.println("Current balance is: " + this.accountBalance);
                this.accountBalance = this.accountBalance + Integer.parseInt(transaction.getSum());
                System.out.println(this.port + " new balance is: " + this.accountBalance);
            }
        }
    }

    public boolean checkIfSignatureIsValid(Transaction transaction) throws Exception {
        String hash = transaction.hash();
        PublicKey publicKey = generator.getPublicKeyFromString(transaction.getFromPublicKey());
        String decryptedString = generator.decrypt(publicKey, transaction.getSignature());
        return hash.equals(decryptedString);
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

    public boolean checkIfTransactionSumIsValid(int sum) {
        return this.accountBalance >= sum;
    }

    public synchronized void findAndChooseCorrectBlocks() {
        if (knownBlocks.size() > 1) {
            System.out.println("Size: " + knownBlocks.size());
            for (int i = 0; i < knownBlocks.size(); i++) {
                Block firstBlock = knownBlocks.get(i);
                //System.out.println("First Block: " + i + " nr: " + firstBlock.getNumber());
                for (int j = i + 1; j < knownBlocks.size(); j++) {
                    Block secondBlock = knownBlocks.get(j);
                    //System.out.println("Second Block: " + j + " nr: " + secondBlock.getNumber());
                    if (firstBlock.getNumber() == secondBlock.getNumber()) {
                        chooseAndRemoveCorrectBlock(firstBlock, secondBlock);
                    }
                }
            }
        } else {
            System.out.println("There is only the Genesis Block, everything is in order");
        }

        if (!blocksThatShouldBeRemoved.isEmpty()) {
            System.out.println("Removing blocks from the internal list as well");
            System.out.println("Old size: " + knownBlocks.size());
            this.knownBlocks.removeAll(this.blocksThatShouldBeRemoved);
            System.out.println("New size: " + knownBlocks.size());
        }

        this.blocksThatShouldBeRemoved.clear();

    }

    private synchronized void chooseAndRemoveCorrectBlock(Block firstBlock, Block secondBlock) {
        //System.out.println("We have a problem");
        //System.out.println("First Block is: " + firstBlock.getNumber() + " " + firstBlock.getCount() + " " + firstBlock.getTimeStamp());
        //System.out.println("Second Block is: " + secondBlock.getNumber() + " " + secondBlock.getCount() + " " + secondBlock.getTimeStamp());
        Block blockToBeRemoved = findBlockToRemove(firstBlock, secondBlock);

        System.out.println("Block to remove is: " + blockToBeRemoved.getNumber() + " " + blockToBeRemoved.getCount() + " " + blockToBeRemoved.getTimeStamp());
        //this.getKnownBlocks().remove(blockToBeRemoved);
        blocksThatShouldBeRemoved.add(blockToBeRemoved);
        try {
            this.removeBlockStringFromFile(blockToBeRemoved);
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("Block List: " + getKnownBlocks().size());
    }

    private Block findBlockToRemove(Block firstBlock, Block secondBlock) {
        if (firstBlock.getCount() > secondBlock.getCount()) {
            return secondBlock;
        } else if (firstBlock.getCount() < secondBlock.getCount()) {
            return firstBlock;
        } else if (firstBlock.getTimeStamp().compareTo(secondBlock.getTimeStamp()) < 0) {
            return secondBlock;
        } else if (firstBlock.getTimeStamp().compareTo(secondBlock.getTimeStamp()) > 0) {
            return firstBlock;
        } else if (firstBlock.hash().compareTo(secondBlock.hash()) < 0) {
            return secondBlock;
        } else if (firstBlock.hash().compareTo(secondBlock.hash()) > 0) {
            return firstBlock;
        } else {
            //Just remove the newer block;
            return secondBlock;
        }
    }

    private synchronized void removeBlockStringFromFile(Block block) throws IOException {
        //Path path = Paths.get("src\\node\\copyOfBlocks.txt");
        String blockAsString = gson.toJson(block);
        String path = "src\\node\\copyOfBlocks.txt";

        File file = new File(path);

        synchronized (file.getCanonicalPath().intern()) {
            List<String> out = Files.lines(Paths.get(path))
                    .filter(line -> !line.equals(blockAsString))
                    .collect(Collectors.toList());
            Files.write(Paths.get(path), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Removing from file: nr: " + block.getNumber() + " nonce: " + block.getNonce());
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
            publicKey = generator.getStringFromPublicKey(generator.getPublicKey());
            System.out.println("This Node: " + publicKey);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<Transaction> getUnUsedTransactions() {
        return unUsedTransactions;
    }

    public int getAccountBalance() {
        return accountBalance;
    }
}

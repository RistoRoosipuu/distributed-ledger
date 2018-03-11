package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.util.Date;
import java.util.List;

public class Block implements Hashable{

    private String prevHash;
    private List<Transaction> transactions;
    private long timeStamp;
    public Block(String prevHash, List<Transaction> transactions) {
        this.prevHash = prevHash;
        this.transactions = transactions;
        this.timeStamp = new Date().getTime();
    }

    @Override
    public String hash() {
        StringBuilder sb = new StringBuilder();
        sb.append(prevHash);
        sb.append("|");
        sb.append(timeStamp);
        for(Transaction transaction : transactions) {
            sb.append("|");
            sb.append(transaction.hash());
        }
        return HashToHex.sha256ToHex(sb.toString());
    }

    @Override
    public String toString() {
        return "Block hash: " + this.hash();
    }

    public String getPrevHash() {
        return prevHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}

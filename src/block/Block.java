package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.time.Instant;
import java.util.List;

public class Block implements Hashable {

    private int number;
    private String prevHash;
    private int count;
    private String nonce;
    private String hash;
    private String creator;
    private String merkle_root;
    private List<Transaction> transactions;
    private String timeStamp;

    public Block(String prevHash, List<Transaction> transactions) {
        this.prevHash = prevHash;
        this.transactions = transactions;
        this.timeStamp = Instant.now().toString();
    }

    @Override
    public String hash() {
        StringBuilder sb = new StringBuilder();
        sb.append(prevHash);
        sb.append("|");
        sb.append(timeStamp);
        for (Transaction transaction : transactions) {
            sb.append("|");
            sb.append(transaction.hash());
        }
        return HashToHex.sha256ToHex(sb.toString());
    }

    @Override
    public String toString() {
        return "Block hash: " + this.hash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (prevHash != null ? !prevHash.equals(block.prevHash) : block.prevHash != null) return false;
        if (transactions != null ? !transactions.equals(block.transactions) : block.transactions != null) return false;
        return timeStamp != null ? timeStamp.equals(block.timeStamp) : block.timeStamp == null;
    }

    @Override
    public int hashCode() {
        int result = prevHash != null ? prevHash.hashCode() : 0;
        result = 31 * result + (transactions != null ? transactions.hashCode() : 0);
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        return result;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public String getCreator() {
        return creator;
    }

    public String getMerkle_root() {
        return merkle_root;
    }

    public void setMerkle_root(String merkle_root) {
        this.merkle_root = merkle_root;
    }
}

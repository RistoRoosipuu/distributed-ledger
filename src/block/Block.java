package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.time.Instant;
import java.util.List;

public class Block implements Hashable {

    private String prevHash;
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
}

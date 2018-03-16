package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.util.Date;
import java.util.List;

public class Block implements Hashable {

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

        if (timeStamp != block.timeStamp) return false;
        if (prevHash != null ? !prevHash.equals(block.prevHash) : block.prevHash != null) return false;
        return transactions != null ? transactions.equals(block.transactions) : block.transactions == null;
    }

    @Override
    public int hashCode() {
        int result = prevHash != null ? prevHash.hashCode() : 0;
        result = 31 * result + (transactions != null ? transactions.hashCode() : 0);
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        return result;
    }
}

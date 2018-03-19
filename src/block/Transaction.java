package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.util.Date;
import java.util.List;

public class Transaction implements Hashable {

    private String prevHash;
    private String data;
    private long timeStamp;


    //If known Transactions are empty, create Genesis Transaction, else lastKnownHash = prevHash
    public static Transaction getNewTransaction(List<Transaction> knownTransaction, String data) {
        if (knownTransaction.size() == 0) {
            return new Transaction(data);
        } else {
            return new Transaction(knownTransaction.get(knownTransaction.size() - 1).hash(), data);
        }
    }

    //so called Genesis Transaction?
    private Transaction(String data) {
        this.data = data;
        this.timeStamp = new Date().getTime();
    }

    private Transaction(String prevHash, String data) {
        this.prevHash = prevHash;
        this.data = data;
        this.timeStamp = new Date().getTime();
    }


    @Override
    public String hash() {
        StringBuilder sb = new StringBuilder();
        sb.append(prevHash);
        sb.append("|");
        sb.append(data);
        sb.append("|");
        sb.append(timeStamp);

        return HashToHex.sha256ToHex(sb.toString());
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getData() {
        return data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;

        if (timeStamp != that.timeStamp) return false;
        if (prevHash != null ? !prevHash.equals(that.prevHash) : that.prevHash != null) return false;
        return data != null ? data.equals(that.data) : that.data == null;
    }

    @Override
    public int hashCode() {
        int result = prevHash != null ? prevHash.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        return result;
    }
}

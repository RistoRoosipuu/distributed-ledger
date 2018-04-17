package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.security.PublicKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Transaction implements Hashable {

    private String signature;
    private String prevHash;
    private String fromPublicKey;
    private String toPublicKey;
    private String sum;
    private String timeStamp;

    //If known Transactions are empty, create Genesis Transaction, else lastKnownHash = prevHash
    public static Transaction getNewTransaction(List<Transaction> knownTransaction, String fromPublicKey, String toPublicKey, String sum) {
        if (knownTransaction.size() == 0) {
            return new Transaction(fromPublicKey, toPublicKey, sum);
        } else {
            return new Transaction(knownTransaction.get(knownTransaction.size() - 1).hash(), fromPublicKey, toPublicKey, sum);
        }
    }

    //so called Genesis Transaction?
    private Transaction(String fromPublicKey, String toPublicKey, String sum) {
        this.prevHash = "0";
        this.fromPublicKey = fromPublicKey;
        this.toPublicKey = toPublicKey;
        this.sum = sum;
        this.timeStamp = Instant.now().toString();
    }

    private Transaction(String prevHash, String fromPublicKey, String toPublicKey, String sum) {
        this.prevHash = prevHash;
        this.fromPublicKey = fromPublicKey;
        this.toPublicKey = toPublicKey;
        this.sum = sum;
        this.timeStamp = Instant.now().toString();
    }



    @Override
    public String hash() {
        StringBuilder sb = new StringBuilder();
        sb.append(prevHash);
        sb.append("|");
        sb.append(fromPublicKey);
        sb.append("|");
        sb.append(toPublicKey);
        sb.append("|");
        sb.append(sum);
        sb.append("|");
        sb.append(timeStamp);

        return HashToHex.sha256ToHex(sb.toString());
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getFromPublicKey() {
        return fromPublicKey;
    }

    public String getToPublicKey() {
        return toPublicKey;
    }

    public String getSum() {
        return sum;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;

        if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;
        if (prevHash != null ? !prevHash.equals(that.prevHash) : that.prevHash != null) return false;
        if (fromPublicKey != null ? !fromPublicKey.equals(that.fromPublicKey) : that.fromPublicKey != null)
            return false;
        if (toPublicKey != null ? !toPublicKey.equals(that.toPublicKey) : that.toPublicKey != null) return false;
        if (sum != null ? !sum.equals(that.sum) : that.sum != null) return false;
        return timeStamp != null ? timeStamp.equals(that.timeStamp) : that.timeStamp == null;
    }

    @Override
    public int hashCode() {
        int result = signature != null ? signature.hashCode() : 0;
        result = 31 * result + (prevHash != null ? prevHash.hashCode() : 0);
        result = 31 * result + (fromPublicKey != null ? fromPublicKey.hashCode() : 0);
        result = 31 * result + (toPublicKey != null ? toPublicKey.hashCode() : 0);
        result = 31 * result + (sum != null ? sum.hashCode() : 0);
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        return result;
    }
}

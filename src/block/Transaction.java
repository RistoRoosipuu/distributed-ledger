package block;

import hashing.HashToHex;
import hashing.Hashable;

import java.util.Date;

public class Transaction implements Hashable {

    private String prevHash;
    private String data;
    private long timeStamp;

    public Transaction(String prevHash, String data) {
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
}

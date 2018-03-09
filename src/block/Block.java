package block;

import java.security.MessageDigest;
import java.util.Date;

public class Block {

    private String hash;
    private String prevHash;
    private String data;
    private long timeStamp;

    public Block(String data, String prevHash) {
        this.data = data;
        this.prevHash = prevHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    private String calculateHash() {
        String calculatedHash = applySha256AndConvertToHex(prevHash + Long.toString(timeStamp) + data);

        return calculatedHash;
    }

    private String applySha256AndConvertToHex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes("UTF-8"));

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < encodedHash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedHash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public String getHash() {
        return hash;
    }
}

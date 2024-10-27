package es.jaf.mfa_authenticator;

import android.net.Uri;
import org.apache.commons.codec.binary.Base32;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

public class AccountStruc {
    public static final String JSON_LABEL = "label";
    public static final String JSON_ACCOUNT = "account";
    public static final String JSON_ALGORITHM = "algorithm";
    public static final String JSON_ISSUER = "issuer";
    public static final String JSON_PERIOD = "period";
    public static final String JSON_DIGITS = "digits";
    public static final String JSON_LOCKED = "locked";
    public static final String JSON_SECRET = "secret";

    private String label;
    private String account;
    private String algorithm;
    private String issuer;
    private int period;
    private int digits;
    private boolean favourite;
    private byte[] secret;
    private String currentOTP;

    public AccountStruc(String contents) throws Exception {
        contents = contents.replaceFirst("otpauth", "http");
        Uri uri = Uri.parse(contents);
        URL url = new URL(contents);

        if(!url.getProtocol().equals("http")){
            throw new Exception("Invalid Protocol");
        }

        if(!url.getHost().equals("totp")){
            throw new Exception();
        }

        String tmp = uri.getQueryParameter("secret");
        if (tmp == null || tmp.isEmpty()) {
            throw new Exception("Invalid Secret.");
        }
        this.secret = new Base32().decode(tmp.toUpperCase());

        algorithm = uri.getQueryParameter("algorithm");
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = "SHA1"; //throw new Exception("Invalid algorithm.");
        }

        String path = uri.getPath();
        if (path != null){
            path = path.substring(1);
            int pos = path.indexOf(":");
            if (pos >= 0) {
                this.label = path.substring(0, pos);
                this.account = path.substring(pos + 1);
            } else {
                this.label = "";
                this.account = path;
            }
        }

        issuer = uri.getQueryParameter("issuer");
        issuer = (issuer == null) ? "" : issuer;

        tmp = uri.getQueryParameter("period");
        try {
            period = Integer.parseInt(tmp);
        } catch (Exception e) {
            period = 30;
        }

        tmp = uri.getQueryParameter("digits");
        try {
            digits = Integer.parseInt(tmp);
        } catch (Exception e) {
            digits = 6;
        }
        favourite = false;
        currentOTP = DataHelper.OTP_NONE;
    }

    public AccountStruc(JSONObject jsonObj ) throws JSONException {
        this.setLabel(jsonObj.getString(JSON_LABEL));
        this.setAccount(jsonObj.getString(JSON_ACCOUNT));
        this.setAlgorithm(jsonObj.getString(JSON_ALGORITHM));
        this.setIssuer(jsonObj.getString(JSON_ISSUER));
        try {
            this.setPeriod(Integer.parseInt(jsonObj.getString(JSON_PERIOD)));
        } catch (Exception e) {
            this.setPeriod(30);
        }
        try {
            this.setDigits(Integer.parseInt(jsonObj.getString(JSON_DIGITS)));
        } catch (Exception e) {
            this.setDigits(6);
        }
        this.setFavourite("true".equals(jsonObj.getString(JSON_LOCKED)));
        this.setSecret(new Base32().decode(jsonObj.getString(JSON_SECRET)));
        this.currentOTP = DataHelper.OTP_NONE;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(JSON_LABEL, getLabel());
        jsonObj.put(JSON_ACCOUNT, getAccount());
        jsonObj.put(JSON_ALGORITHM, getAlgorithm());
        jsonObj.put(JSON_ISSUER, getIssuer());
        jsonObj.put(JSON_PERIOD, getPeriod());
        jsonObj.put(JSON_DIGITS, getDigits());
        jsonObj.put(JSON_LOCKED, isFavourite());
        jsonObj.put(JSON_SECRET, new String(new Base32().encode(getSecret())));

        return jsonObj;
    }

    public String getAccount() {
        return (account == null ? "" : account);
    }
    public void setAccount(String value) {
        account = value;
    }

    public String getAlgorithm() {
        return (algorithm == null ? "" : algorithm);
    }
    public void setAlgorithm(String value) {
        algorithm = value;
    }

    public String getIssuer() {
        return (issuer == null ? "" : issuer);
    }
    public void setIssuer(String value) {
        issuer = value;
    }

    public int getPeriod() {
        return period;
    }
    public void setPeriod(int value) {
        period = value;
    }

    public int getDigits() {
        return digits;
    }
    public void setDigits(int value) {
        digits = value;
    }

    public boolean isFavourite() {
        return favourite;
    }
    public void setFavourite(boolean value) {
        favourite = value;
    }

    public byte[] getSecret() {
        return secret;
    }
    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public String getCurrentOTP() {
        return currentOTP;
    }
    public void setCurrentOTP(String currentOTP) {
        this.currentOTP = currentOTP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountStruc account = (AccountStruc) o;

        if (!Arrays.equals(secret, account.secret)) return false;
        return Objects.equals(this.account, account.account);

    }

    @Override
    public int hashCode() {
        int result = secret != null ? Arrays.hashCode(secret) : 0;
        result = 31 * result + (account != null ? account.hashCode() : 0);
        return result;
    }
}

package es.jaf.mfa_authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class DataHelper {
    public static final String OTP_NONE = "------";

    public static void store(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts) throws Exception{
        SharedPreferences encryptedPrefs = Utils.getEncryptedPrefs(context.getApplicationContext());
        JSONArray arr = new JSONArray();
        for (Pair<Integer, AccountStruc> pair : accounts) {
            try {
                arr.put(pair.second.toJSON());
            } catch (Exception e) {
                Utils.saveException("Converting '" + pair.second + "' to json " + e, e);
            }
        }
        encryptedPrefs.edit().putString("values", arr.toString()).apply();
    }

    public static ArrayList<Pair<Integer, AccountStruc>> load(Context context) throws Exception {
        SharedPreferences encryptedPrefs = Utils.getEncryptedPrefs(context.getApplicationContext());
        String str = encryptedPrefs.getString("values", null);
        ArrayList<Pair<Integer, AccountStruc>> accounts = new ArrayList<>();
        if (str != null) {
            JSONArray arr = new JSONArray(str);
            for (int i = 0; i < arr.length(); i++) {
                AccountStruc elem = new AccountStruc(arr.getJSONObject(i));
                accounts.add(new Pair<>(i, elem));
            }
        }
        return accounts;
    }

    public static void exportFile(ArrayList<Pair<Integer, AccountStruc>> accounts, OutputStream os, String password) throws Exception{
        JSONArray arr = new JSONArray();
        for(Pair<Integer, AccountStruc> pair: accounts){
            AccountStruc acc = pair.second;
            try {
                arr.put(acc.toJSON());
            } catch (JSONException e1) {
                Utils.saveException("Converting '" + pair.second + "' to json " + acc, e1);
            }
        }

        byte[] data = arr.toString().getBytes(StandardCharsets.UTF_8);

        if (password != null && password.length() > 0 && data != null) {
            data = Utils.encrypt(data, password);
        }
        Utils.writeFully(os, data);
    }

    public static ArrayList<Pair<Integer, AccountStruc>> importFile(InputStream is, String password) throws Exception {
        byte[] data = null;
        if (is != null) {
            data = Utils.readFully(is);
        }

        if (password != null && password.length() > 0 && data != null) {
            data = Utils.decrypt(data, password);
        }

        ArrayList<Pair<Integer, AccountStruc>> accounts = new ArrayList<>();
        if (data != null) {
            JSONArray arr = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                accounts.add(new Pair<>(i, new AccountStruc(arr.getJSONObject(i))));
            }
        }
        return accounts;
    }
}
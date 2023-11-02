package es.jaf.mfa_authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DataHelper {
    public static final String KEY_FILE = "otp.key";
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

    public static void exportFile(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts, OutputStream os) throws Exception{
        JSONArray a = new JSONArray();
        for(Pair<Integer, AccountStruc> p: accounts){
            AccountStruc acc = p.second;
            try {
                a.put(acc.toJSON());
            } catch (JSONException e1) {
                Utils.saveException("Converting '" + p.second + "' to json " + acc, e1);
            }
        }
        String pwd = Utils.getEncryptedPrefs(context.getApplicationContext()).getString("pwd", null);

        byte[] data = a.toString().getBytes();

        SecretKey key = EncryptionHelper.loadOrGenerateKeys(context, pwd, new File(context.getFilesDir() + "/" + KEY_FILE));
        data = EncryptionHelper.encrypt(key,data);
        data = android.util.Base64.encode(data, android.util.Base64.NO_WRAP);

        Utils.writeFully(os, data);
    }

    public static ArrayList<Pair<Integer, AccountStruc>> importFile(Context context, InputStream is) throws Exception {
        byte[] data = null;
        if (is != null) {
            data = Utils.readFully(is);
            data = android.util.Base64.decode(data, android.util.Base64.NO_WRAP);
        }

        String pwd = Utils.getEncryptedPrefs(context.getApplicationContext()).getString("pwd", null);

        SecretKey key = EncryptionHelper.loadOrGenerateKeys(context, pwd, new File(context.getFilesDir() + "/" + KEY_FILE));
        if (data != null) {
            data = EncryptionHelper.decrypt(key, data);
        }

        ArrayList<Pair<Integer, AccountStruc>> accounts = new ArrayList<>();
        if (data != null) {
            JSONArray arr = new JSONArray(new String(data));
            for (int i = 0; i < arr.length(); i++) {
                accounts.add(new Pair<>(i, new AccountStruc(arr.getJSONObject(i))));
            }
        }

        return accounts;
    }
}
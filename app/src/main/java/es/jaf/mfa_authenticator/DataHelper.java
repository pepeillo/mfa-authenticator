package es.jaf.mfa_authenticator;

import android.content.Context;
import android.os.Build;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;

import javax.crypto.SecretKey;
import java.io.*;
import java.util.ArrayList;

public class DataHelper {
    public static final String KEY_FILE = "otp.key";
    public static final String DATA_FILE = "secrets.dat";
    public static final String OTP_NONE = "------";

    public static void store(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts) throws Exception{
        save(context, accounts, new File(context.getFilesDir() + "/" + DATA_FILE));
    }

    public static ArrayList<Pair<Integer, AccountStruc>> load(Context context) throws Exception {
        return read(context, new File(context.getFilesDir() + "/" + DATA_FILE));
    }

    public static void save(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts, File file) throws Exception{
        OutputStream os = new FileOutputStream(file);
        exportFile(context, accounts, os);
    }

    public static ArrayList<Pair<Integer, AccountStruc>> read(Context context, File file) throws Exception {
        FileInputStream is = null;

        if (file.exists()) {
            is = new FileInputStream(file);
        }
        return importFile(context, is);
    }

    public static void exportFile(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts, OutputStream os) throws Exception{
        exportFile(context, accounts, os,false);
    }
    public static void exportFile(Context context, ArrayList<Pair<Integer, AccountStruc>> accounts, OutputStream os, boolean base64) throws Exception{
        JSONArray a = new JSONArray();

        for(Pair<Integer, AccountStruc> p: accounts){
            AccountStruc e = p.second;
            try {
                a.put(e.toJSON());
            } catch (JSONException e1) {
                Utils.saveException("Converting to json " + e, e1);
            }
        }
        String pwd = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("pwd", null);

        byte[] data = a.toString().getBytes();

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            SecretKey key = EncryptionHelper.loadOrGenerateKeys(context, pwd, new File(context.getFilesDir() + "/" + KEY_FILE));
            data = EncryptionHelper.encrypt(key,data);
        }
        if (base64) {
            data = android.util.Base64.encode(data, android.util.Base64.NO_WRAP);
        }
        Utils.writeFully(os, data);
    }

    public static ArrayList<Pair<Integer, AccountStruc>> importFile(Context context, InputStream is) throws Exception {
        return importFile(context, is, false);
    }
    public static ArrayList<Pair<Integer, AccountStruc>> importFile(Context context, InputStream is, boolean base64) throws Exception {
        ArrayList<Pair<Integer, AccountStruc>> accounts = new ArrayList<>();

        byte[] data = null;
        if (is != null) {
            data = Utils.readFully(is);
            if (base64) {
                data = android.util.Base64.decode(data, android.util.Base64.NO_WRAP);
            }
        }

        String pwd = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("pwd", null);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            SecretKey key = EncryptionHelper.loadOrGenerateKeys(context, pwd, new File(context.getFilesDir() + "/" + KEY_FILE));
            if (data != null) {
                data = EncryptionHelper.decrypt(key, data);
            }
        }
        if (data != null) {
            JSONArray a = new JSONArray(new String(data));
            for (int i = 0; i < a.length(); i++) {
                accounts.add(new Pair<>(i, new AccountStruc(a.getJSONObject(i))));
            }
        }

        return accounts;
    }
}
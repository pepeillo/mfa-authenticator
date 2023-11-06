package es.jaf.mfa_authenticator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import org.json.JSONArray;

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
}
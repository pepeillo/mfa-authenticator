package es.jaf.mfa_authenticator;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MyApplication extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        appContext = getApplicationContext();
    }

    protected static SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        // on below line initializing our encrypted shared preferences and passing our key to it.
        return  EncryptedSharedPreferences.create(
                BuildConfig.APPLICATION_ID,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

}

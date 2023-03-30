package es.jaf.mfa_authenticator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private boolean hasBiometric;
    private boolean pwdSaved;
    private SharedPreferences encryptedPrefs = null;

    // Se deja la clase pero sin funcionalidad.
 // Cuando tenga ganas, implementaré la conexión con password y la autenticación biométrica
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Button cmdLogin = findViewById(R.id.cmdLogin);
        cmdLogin.setOnClickListener(view -> {
            String pwd = ((EditText) findViewById(R.id.password)).getText().toString();
            boolean canValidatePwd = false;
            if (pwdSaved) {
                canValidatePwd = true;
            } else {
                String pwd2 = ((EditText) findViewById(R.id.password2)).getText().toString();
                if (pwd.length() > 0 && pwd.equals(pwd2)) {
                    canValidatePwd = true;
                } else {
                    AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);

                    dlg.setTitle(R.string.app_name)
                            .setMessage(R.string.pwd_not_match)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                dialog.dismiss();
                                ((EditText)findViewById(R.id.password)).setText("");
                                ((EditText)findViewById(R.id.password2)).setText("");
                            });
                    dlg.show();
                }
            }
            if (canValidatePwd) {
                new MainActivity.BackTask(pwd).execute();
            }
        });
        EditText txtPwd = findViewById(R.id.password);
        EditText txtPwd2 = findViewById(R.id.password2);
        ImageView cmdHideShow = findViewById(R.id.cmdHideShow);
        ImageView cmdHideShow2 = findViewById(R.id.cmdHideShow2);

        cmdHideShow.setOnClickListener(view -> {
            if (txtPwd.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.show);
            } else {
                txtPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.hide);
            }
        });
        cmdHideShow2.setOnClickListener(view -> {
            if (txtPwd2.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPwd2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShow2.setImageResource(R.drawable.show);
            } else {
                txtPwd2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShow2.setImageResource(R.drawable.hide);
            }
        });
        findViewById(R.id.cmdBiometric).setOnClickListener(view -> withBiometric());

        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        hasBiometric = (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS);

        pwdSaved = false;

        try {
            encryptedPrefs = MyApplication.getEncryptedPrefs();
        } catch (Exception e) {
            cmdLogin.setEnabled(false);
        }
        try {
            String myPwd = encryptedPrefs.getString("pwd", null);
            pwdSaved = (myPwd != null && myPwd.length() > 0);
        } catch (Exception e) {/**/}

        findViewById(R.id.secondPwd).setVisibility(pwdSaved ? View.GONE : View.VISIBLE);
        ((TextView)findViewById(R.id.lblHelp)).setText(pwdSaved ? R.string.enter_pwd : R.string.enter_pwd_new);
        if (hasBiometric && pwdSaved) {
            if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("use_biometric", false)) {
                findViewById(R.id.cmdBiometric).setVisibility(View.VISIBLE);
                withBiometric();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasBiometric && pwdSaved) {
            if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("use_biometric", false)) {
                findViewById(R.id.cmdBiometric).setVisibility(View.VISIBLE);
                withBiometric();
            }
        }
    }

    private void withBiometric() {
        //init bio metric
        Executor executor = ContextCompat.getMainExecutor(this);
        //error authenticating, stop tasks that requires auth
        //authentication succeed, continue tasts that requires auth
        //failed authenticating, stop tasks that requires auth
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                //error authenticating, stop tasks that requires auth
                if (!getString(R.string.use_password).equals(errString.toString())) {
                    Toast.makeText(MainActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                //authentication succeed, continue tasts that requires auth
                try {
                    String key = null;
                    if (hasBiometric) {
                        key = encryptedPrefs.getString("pwd", null);
                    }
                    if (key != null) {
                        new MainActivity.BackTask(key).execute();
                    }
                } catch (Exception e) {/**/}
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                //failed authenticating, stop tasks that requires auth
                Toast.makeText(MainActivity.this, "Authentication failed...!", Toast.LENGTH_SHORT).show();
            }
        });

        //setup title,description on auth dialog
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.bioAuth))
                .setSubtitle(getString(R.string.bioAuthSub))
                .setNegativeButtonText(getString(R.string.use_password))
                .build();

        biometricPrompt.authenticate(promptInfo);

    }

    private class BackTask {
        private final String pwd;
        private final AlertDialog.Builder dialog;
        boolean pwdOk = false;

        public BackTask(String pass) {
            dialog = new AlertDialog.Builder(MainActivity.this);
            this.pwd = pass;
        }

        public void execute() {
            new Thread(() -> {
                try {
                    if (pwdSaved) {
                        String pwdReaded = encryptedPrefs.getString("pwd", "");
                        pwdOk = pwdReaded.equals(this.pwd);
                    } else {
                        pwdOk = true;
                    }
                } catch (Exception e) {
                    //nothing
                }

                runOnUiThread(() -> {
                    if (pwdOk) {
                        try {
                            SharedPreferences.Editor editor = encryptedPrefs.edit();
                            editor.putString("pwd", pwd);
                            editor.apply();
                            editor.commit();
                        } catch (Exception e) {/**/}

                        startActivity(new Intent(getApplicationContext(), AccountsActivity.class));
                        finish();
                    } else {
                        dialog.setTitle(R.string.app_name)
                                .setMessage(R.string.login_failed)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, (dlg, id) -> {
                                    dlg.dismiss();
                                    ((EditText)findViewById(R.id.password)).setText("");
                                });
                        dialog.show();
                    }
                });
            }).start();
        }
    }
}

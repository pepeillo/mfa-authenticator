package es.jaf.mfa_authenticator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    // Se deja la clase pero sin funcionalidad.
 // Cuando tenga ganas, implementaré la conexión con password y la autenticación biométrica
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (!(canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)) {
            Toast.makeText(this, "No tienes autenticación biométrica.", Toast.LENGTH_LONG).show();
            return;
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED ) {
            Toast.makeText(this, "No tiene los permisios necesarios.", Toast.LENGTH_LONG).show();
            //android.os.Process.killProcess(Process.myPid());
            //finish();
            return;
        }

        withBiometric();
    }

    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(Process.myPid());
        finish();
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
                if (getString(R.string.use_password).equals(errString.toString())) {
                    onBackPressed();
                } else {
                    Toast.makeText(MainActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                //authentication succeed, continue tasts that requires auth
                try {
                    new MainActivity.BackTask().execute();
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
        public BackTask() {
        }

        public void execute() {
            new Thread(() -> runOnUiThread(() -> {
                startActivity(new Intent(getApplicationContext(), AccountsActivity.class));
                finish();
            })).start();
        }
    }
}

package es.jaf.mfa_authenticator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.snackbar.Snackbar;

import java.io.OutputStream;
import java.util.ArrayList;

public class ExportActivity extends AppCompatActivity {
    private static final int ACTION_EXPORT = 123;

    private View cmdExport;
    private TextView txtFile;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        txtPassword = findViewById(R.id.txtpassword);
        txtFile = findViewById(R.id.txtFile);
        cmdExport = findViewById(R.id.cmdExport);

        findViewById(R.id.cmdSelect).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                Snackbar.make(cmdExport, "No hay permisos asignados.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_CREATE_DOCUMENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), ACTION_EXPORT);
        });
        /*
        cmdHideShow.setOnClickListener(view -> {
            if (txtPwd.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.show);
            } else {
                txtPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.hide);
            }
        });
         */

        cmdExport.setOnClickListener(v -> {
            String path = txtFile.getText().toString();
            if (path.length() > 0) {
                String password = txtPassword.getText().toString();
                if (password.length() == 0) {
                    new AlertDialog.Builder(ExportActivity.this).setTitle(R.string.app_name)
                            .setMessage(R.string.export_to_encrypted)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                dialog.dismiss();
                                ExportActivity.this.performExport(path, password);
                            }).show();
                } else {
                    performExport(path, password);
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK && requestCode == ACTION_EXPORT) {
            Uri uri = (intent == null) ? null : intent.getData();
            if (uri != null) {
                txtFile.setText(uri.toString());
                cmdExport.setEnabled(true);
            }
        }
    }

    private void performExport(String path, String password) {
        try {
            ExportActivity.this.exportFile(path, password);

            AlertDialog.Builder dialog = new AlertDialog.Builder(ExportActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(ExportActivity.this.getString(R.string.process_ok))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                        dialog12.dismiss();
                        ExportActivity.this.finish();
                    });
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialog.Builder dialog = new AlertDialog.Builder(ExportActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_process_err)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog1, id) -> dialog1.dismiss());
            dialog.show();
        }
    }

    private void exportFile(String path, String password) {
        try (OutputStream os = getContentResolver().openOutputStream(Uri.parse(path), "wt")) {
            ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
            DataHelper.exportFile(this, accounts, os, password);
        } catch (Exception e) {
            Utils.saveException("Exporting file", e);
            Toast.makeText(this, "Error exporting file. " + e, Toast.LENGTH_SHORT).show();
        }
    }
}
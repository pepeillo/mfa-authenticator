package es.jaf.mfa_authenticator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.util.ArrayList;

public class ImportActivity extends AppCompatActivity {
    private static final int ACTION_IMPORT = 124;

    private View cmdImport;
    private TextView txtFile;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        txtPassword = findViewById(R.id.txtpassword);
        txtFile = findViewById(R.id.txtFile);
        cmdImport = findViewById(R.id.cmdImport);

        findViewById(R.id.cmdSelect).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                Snackbar.make(cmdImport, "No hay permisos asignados.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), ACTION_IMPORT);
        });

        cmdImport.setOnClickListener(v -> {
            String path = txtFile.getText().toString();
            if (path.length() > 0) {
                String password = txtPassword.getText().toString();
                if (password.length() == 0) {
                    new AlertDialog.Builder(ImportActivity.this).setTitle(R.string.app_name)
                            .setMessage(R.string.import_from_encrypted)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                dialog.dismiss();
                                ImportActivity.this.performImport(path, password);
                            }).show();
                } else {
                    performImport(path, password);
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK && requestCode == ACTION_IMPORT) {
            Uri uri = (intent == null) ? null : intent.getData();
            if (uri != null) {
                txtFile.setText(uri.toString());
                cmdImport.setEnabled(true);
            }
        }
    }

    private void performImport(String path, String password) {
        try {
            ImportActivity.this.importFile(path, password);

            AlertDialog.Builder dialog = new AlertDialog.Builder(ImportActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(ImportActivity.this.getString(R.string.process_ok))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                        dialog12.dismiss();
                        ImportActivity.this.finish();
                    });
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialog.Builder dialog = new AlertDialog.Builder(ImportActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_process_err)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog1, id) -> dialog1.dismiss());
            dialog.show();
        }
    }

    private void importFile(String path, String password) {
        try (InputStream is = getContentResolver().openInputStream(Uri.parse(path))) {
            ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
            accounts.addAll(DataHelper.importFile(this, is));
            DataHelper.store(this, accounts);
        } catch (Exception e) {
            Utils.saveException("Importing file", e);
            Toast.makeText(this, "Error importing file. " + e, Toast.LENGTH_SHORT).show();
        }
    }
}
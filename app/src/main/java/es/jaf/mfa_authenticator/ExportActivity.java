package es.jaf.mfa_authenticator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ExportActivity extends Activity {
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, Utils.FILE_OR_FOLDER_PICKER_CODE);
        });

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

        final ImageView cmdHideShow = findViewById(R.id.cmdHideShow);
        cmdHideShow.setOnClickListener(view -> {
            if (txtPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.show);
            } else {
                txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShow.setImageResource(R.drawable.hide);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Utils.FILE_OR_FOLDER_PICKER_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = intent.getData();
            try {
                String path = FileUtils.getFolderPath(ExportActivity.this, uri);
                txtFile.setText(path);
                cmdExport.setEnabled(true);

            } catch (Exception e) {
                e.printStackTrace();
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
            Utils.saveException("Error", e);
            AlertDialog.Builder dialog = new AlertDialog.Builder(ExportActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_process_err)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog1, id) -> dialog1.dismiss());
            dialog.show();
        }
    }

    private void exportFile(String path, String password) throws Exception {
        boolean hasPassword = password != null && password.length() > 0;

        JSONArray arr = new JSONArray();
        ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
        for (Pair<Integer, AccountStruc> pair : accounts) {
            AccountStruc acc = pair.second;
            try {
                arr.put(acc.toJSON());
            } catch (JSONException e1) {
                Utils.saveException("Converting '" + pair.second + "' to json " + acc, e1);
            }
        }

        byte[] data = arr.toString().getBytes(StandardCharsets.UTF_8);

        String fileName = "backup_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File zFile = new File(Uri.parse(path).getPath(), fileName + ".zip");

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(hasPassword);
        zipParameters.setCompressionLevel(CompressionLevel.HIGHER);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        zipParameters.setFileNameInZip("data.json");

        ZipFile zipFile;
        if (hasPassword) {
            zipFile = new ZipFile(zFile, password.toCharArray());
        } else {
            zipFile = new ZipFile(zFile);
        }
        InputStream stream = new ByteArrayInputStream(data);
        zipFile.addStream(stream, zipParameters);
    }
}
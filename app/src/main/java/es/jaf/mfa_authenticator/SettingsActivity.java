package es.jaf.mfa_authenticator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SettingsActivity extends AppCompatActivity {
    private View cmdExport;
    private TextView txtFolder;
    private EditText txtPasswordExport;
    private View cmdImport;
    private TextView txtFile;
    private EditText txtPasswordImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean copyToClipboard =  pref.getBoolean("copy_clipboard", false);

        Switch swCopy = findViewById(R.id.swCopy);
        swCopy.setChecked(copyToClipboard);
        swCopy.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences pref1 = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
            SharedPreferences.Editor edit = pref1.edit();
            edit.putBoolean("copy_clipboard", b);
            edit.apply();
            edit.commit();
        });

        txtPasswordExport = findViewById(R.id.txtpasswordExport);
        txtFolder = findViewById(R.id.txtFolder);
        cmdExport = findViewById(R.id.cmdExport);

        findViewById(R.id.cmdSelectFolder).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, Utils.FOLDER_PICKER_CODE);
        });

        cmdExport.setOnClickListener(v -> {
            String path = txtFolder.getText().toString();
            if (!path.isEmpty()) {
                String password = txtPasswordExport.getText().toString();
                if (password.isEmpty()) {
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.app_name)
                            .setMessage(R.string.export_to_encrypted)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                dialog.dismiss();
                                SettingsActivity.this.performExport(path, password);
                            }).show();
                } else {
                    performExport(path, password);
                }
            }
        });

        ImageView cmdHideShowExport = findViewById(R.id.cmdHideShowExport);
        cmdHideShowExport.setOnClickListener(view -> {
            if (txtPasswordExport.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPasswordExport.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShowExport.setImageResource(R.drawable.show);
            } else {
                txtPasswordExport.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShowExport.setImageResource(R.drawable.hide);
            }
        });

        txtPasswordImport = findViewById(R.id.txtpasswordImport);
        txtFile = findViewById(R.id.txtFile);
        cmdImport = findViewById(R.id.cmdImport);

        findViewById(R.id.cmdSelectFile).setOnClickListener(v -> {
            Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, SettingsActivity.this.getString(R.string.select_a_file)), Utils.FILE_PICKER_CODE);
        });

        cmdImport.setOnClickListener(v -> {
            String path = txtFile.getText().toString();
            if (!path.isEmpty()) {
                String password = txtPasswordImport.getText().toString();
                if (password.isEmpty()) {
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.app_name)
                            .setMessage(R.string.import_from_encrypted)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                dialog.dismiss();
                                SettingsActivity.this.performImport(path, password);
                            }).show();
                } else {
                    performImport(path, password);
                }
            }
        });

        ImageView cmdHideShowImport = findViewById(R.id.cmdHideShowImport);
        cmdHideShowImport.setOnClickListener(view -> {
            if (txtPasswordImport.getTransformationMethod() instanceof PasswordTransformationMethod) {
                txtPasswordImport.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                cmdHideShowImport.setImageResource(R.drawable.show);
            } else {
                txtPasswordImport.setTransformationMethod(PasswordTransformationMethod.getInstance());
                cmdHideShowImport.setImageResource(R.drawable.hide);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = intent.getData();
            try {
                if (requestCode == Utils.FOLDER_PICKER_CODE) {
                    String path = FileUtils.getFolderPath(SettingsActivity.this, uri);
                    txtFolder.setText(path == null ? "" : path);
                    cmdExport.setEnabled(true);
                } else if (requestCode == Utils.FILE_PICKER_CODE) {
                    String path = FileUtils.getFilePath(SettingsActivity.this, uri);
                    txtFile.setText(path == null ? "" : path);
                    cmdImport.setEnabled(true);
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private void performExport(String path, String password) {
        try {
            exportFile(path, password);

            AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(SettingsActivity.this.getString(R.string.process_ok))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                        dialog12.dismiss();
                        SettingsActivity.this.finish();
                    });
            dialog.show();

        } catch (Exception e) {
            Utils.saveException("Error", e);
            AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_process_err)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog1, id) -> dialog1.dismiss());
            dialog.show();
        }
    }

    private void exportFile(String path, String password) throws Exception {
        boolean hasPassword = password != null && !password.isEmpty();

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

    private void performImport(String path, String password) {
        try {
            importFile(path, password);

            AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(SettingsActivity.this.getString(R.string.process_ok))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                        dialog12.dismiss();
                        SettingsActivity.this.finish();
                    });
            dialog.show();

        } catch (Exception e) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
            dialog.setTitle(R.string.app_name)
                    .setMessage(R.string.prompt_process_err)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog1, id) -> dialog1.dismiss());
            dialog.show();
        }
    }

    private void importFile(String path, String password) {
        File tmpFile = new File(Uri.parse(path).getPath());
        ZipInputStream zis = null;
        try {
            ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
            if (password == null || password.isEmpty()) {
                zis = new ZipInputStream(new FileInputStream(tmpFile));
            } else {
                zis = new ZipInputStream(new FileInputStream(tmpFile), password.toCharArray());
            }
            while (zis.getNextEntry() != null) {
                byte[] bytes;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }

                    bytes = baos.toByteArray();
                }

                try (BufferedReader bR = new BufferedReader(  new InputStreamReader(new ByteArrayInputStream(bytes)))) {
                    String line;
                    StringBuilder responseStrBuilder = new StringBuilder();
                    while((line =  bR.readLine()) != null){
                        responseStrBuilder.append(line);
                    }

                    JSONArray jsArray = new JSONArray(responseStrBuilder.toString());
                    for (int i = 0; i < jsArray.length(); i++) {
                        JSONObject ae = (JSONObject) jsArray.get(i);
                        accounts.add(new Pair<>(accounts.size(), new AccountStruc(ae)));
                    }
                }
            }
            DataHelper.store(SettingsActivity.this, accounts);
        } catch (Exception e) {
            Utils.saveException("Error", e);
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {/**/}
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK);
        finish();
    }
}
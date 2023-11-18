package es.jaf.mfa_authenticator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

public class ImportActivity extends Activity {
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
            Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, ImportActivity.this.getString(R.string.select_a_file)), Utils.FILE_OR_FOLDER_PICKER_CODE);
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

            String path = null;
            try {
                path = FileUtils.getFilePath(ImportActivity.this, uri);
            } catch (Exception e) {
                Log.e("xxxxxxxxxx","aaaaaaaaaaaa", e);
                e.printStackTrace();
            }

            txtFile.setText(path == null ? "" : path);
            cmdImport.setEnabled(true);
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
        File tmpFile = new File(Uri.parse(path).getPath());
        ZipInputStream zis = null;
        try {
            ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
            if (password == null || password.length() == 0) {
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
            DataHelper.store(ImportActivity.this, accounts);
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
}
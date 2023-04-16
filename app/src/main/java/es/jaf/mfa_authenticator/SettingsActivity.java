package es.jaf.mfa_authenticator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    private static final int ACTION_EXPORT = 123;
    private static final int ACTION_IMPORT = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        SettingsActivity.this.setResult(Activity.RESULT_OK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == ACTION_EXPORT) {
            Uri uri = data.getData();
            if (uri != null) {
                OutputStream os = null;
                try {
                    os = getContentResolver().openOutputStream(uri);
                    ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
                    DataHelper.exportFile(this, accounts, os, true);
                } catch (Exception e) {
                    Utils.saveException("Exporting file", e);
                    Toast.makeText(this, "Error exporting file. " + e, Toast.LENGTH_SHORT).show();
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            //Nothing
                        }
                    }
                }
            }
            return;
        }

        if (resultCode == RESULT_OK && requestCode == ACTION_IMPORT) {
            Uri uri = data.getData();
            if (uri != null) {
                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                    ArrayList<Pair<Integer, AccountStruc>> accounts = DataHelper.load(this);
                    accounts.addAll(DataHelper.importFile(this, is, true));
                    DataHelper.store(this, accounts);
                } catch (Exception e) {
                    Utils.saveException("Importing file", e);
                    Toast.makeText(this, "Error importing file. " + e, Toast.LENGTH_SHORT).show();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            //Nothing
                        }
                    }
                }
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    public void cmdExport(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(view, "No hay permisos asignados.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.app_name)
                .setMessage(R.string.export_to_encrypted)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    dialog.dismiss();
                    Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_CREATE_DOCUMENT);
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), 123);
                }).show();
    }

    public void cmdImport(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(view, "No hay permisos asignados.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.app_name)
                .setMessage(R.string.import_from_encrypted)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    dialog.dismiss();
                    Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), 124);
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            SettingsActivity.this.setResult(Activity.RESULT_OK);
            SettingsActivity.this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        SettingsActivity.this.setResult(Activity.RESULT_OK);
        SettingsActivity.this.finish();
    }
}
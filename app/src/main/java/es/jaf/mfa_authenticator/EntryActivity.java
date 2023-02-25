package es.jaf.mfa_authenticator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EntryActivity extends AppCompatActivity implements  ActionMode.Callback {

    private EditText txtLabel;
    private EditText txtAccount;
    private EditText txtIssuer;
    private Spinner spnAlgorithm;
    private Spinner spnPeriod;
    private EditText txtDigits;
    private Switch chkFavourite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String label = bundle.getString("label");
        String account = bundle.getString("account");
        String issuer = bundle.getString("issuer");
        int algorithm = bundle.getInt("algorithm");
        int period = bundle.getInt("period");
        int digits = bundle.getInt("digits");
        boolean locked = bundle.getBoolean("locked");
        String secret = bundle.getString("secret");

        txtLabel = findViewById(R.id.txtLabel);
        txtAccount = findViewById(R.id.txtAccount);
        txtIssuer = findViewById(R.id.txtIssuer);
        spnAlgorithm = findViewById(R.id.spnAlgorithm);
        spnPeriod = findViewById(R.id.spnPeriod);
        txtDigits = findViewById(R.id.txtDigits);
        chkFavourite = findViewById(R.id.chkFavourite);
        chkFavourite.setOnCheckedChangeListener((compoundButton, b) -> {
            txtLabel.setEnabled(!b);
            txtAccount.setEnabled(!b);
            txtIssuer.setEnabled(!b);
            spnAlgorithm.setEnabled(!b);
            spnPeriod.setEnabled(!b);
            txtDigits.setEnabled(!b);
        });
        TextView txtSecret = findViewById(R.id.txtSecret);

        txtLabel.setText(label);
        txtAccount.setText(account);
        txtIssuer.setText(issuer);
        spnAlgorithm.setSelection(algorithm);
        spnPeriod.setSelection(period);
        txtDigits.setText("" + digits);
        chkFavourite.setChecked(locked);
        txtSecret.setText(secret);

        Bitmap bitmap = loadImage(label, account, issuer, spnPeriod.getSelectedItem().toString(), digits, secret);
        ((ImageView)findViewById(R.id.imgQR)).setImageBitmap(bitmap);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        startActionMode(EntryActivity.this);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        EntryActivity.this.setResult(Activity.RESULT_CANCELED);
        EntryActivity.this.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {// todo: goto back activity from here

            EntryActivity.this.setResult(Activity.RESULT_CANCELED);
            EntryActivity.this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.menu_entry, menu);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.action_save) {
            int digits ;
            try {
                digits = Integer.parseInt(String.valueOf(txtDigits.getText()));
            } catch (Exception e) {
                digits = 6;
            }
            Intent intent = new Intent();
            intent.putExtra("label", txtLabel.getText().toString());
            intent.putExtra("account", txtAccount.getText().toString());
            intent.putExtra("issuer", txtIssuer.getText().toString());
            intent.putExtra("algorithm", spnAlgorithm.getSelectedItem().toString());
            intent.putExtra("period", Integer.parseInt(spnPeriod.getSelectedItem().toString()));
            intent.putExtra("digits", digits);
            intent.putExtra("locked", chkFavourite.isChecked());

            EntryActivity.this.setResult(Activity.RESULT_OK, intent);
            EntryActivity.this.finish();

            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        EntryActivity.this.setResult(Activity.RESULT_CANCELED);
        EntryActivity.this.finish();
    }


    private Bitmap loadImage(String label, String account, String issuer, String period, int digits, String secret) {
        int width = 250;
        int height = 250;

        try {
            String data = "otpauth://totp/" + URLEncoder.encode(label, StandardCharsets.UTF_8.name())
                    + (label.length() > 0 ? ":" : "")
                    + URLEncoder.encode(account, StandardCharsets.UTF_8.name()) + "?"
                    + "issuer=" + issuer
                    + "&algorithm=" + URLEncoder.encode(spnAlgorithm.getSelectedItem().toString(), StandardCharsets.UTF_8.name())
                    + "&period=" + period
                    + "&digits=" + digits
                    + "&secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8.name());

            com.google.zxing.Writer writer = new QRCodeWriter();
            BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, width, height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < width; i++) {// width
                for (int j = 0; j < height; j++) {// height
                    bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            Utils.saveException("Loading QR image", e);
        }
        return null;
    }
}
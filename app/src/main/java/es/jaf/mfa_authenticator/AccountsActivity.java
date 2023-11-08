package es.jaf.mfa_authenticator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;
import org.apache.commons.codec.binary.Base32;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AccountsActivity extends AppCompatActivity implements  ActionMode.Callback, IAdapterEvents {
    private static final int PERMISSIONS_REQUEST_CAMERA = 42;
    private static final int ACTION_NEW = 101;
    private static final int ACTION_EDIT = 102;
    private static final int ACTION_SETTINGS = 103;

    private ArrayList<Pair<Integer, AccountStruc>> accounts;
    private AccountsListAdapter adapter;
    private FloatingActionButton floatingButton;
    private Pair<Integer, AccountStruc> nextSelection = null;

    private DragListView listView;
    private ActionMode actionMode;
    private View viewLongClicked;
    private Handler handlerRow;
    private MyRunnable handlerTaskRow;

    private void doScanQRCode() {
        new IntentIntegrator(AccountsActivity.this)
                .setCaptureActivity(CaptureActivityAnyOrientation.class)
                .setOrientationLocked(false)
                .initiateScan();
    }

    private void scanQRCode() {
        // check Android 6 permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            doScanQRCode();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                doScanQRCode();
            } else {
                Snackbar.make(floatingButton, R.string.msg_camera_permission, Snackbar.LENGTH_LONG).setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                    }
                }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.accounts_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        floatingButton = findViewById(R.id.action_scan);
        floatingButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(AccountsActivity.this);
            builder.setTitle(R.string.prompt_options)
                    .setCancelable(true)
                    .setItems(R.array.create_options, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(AccountsActivity.this, AccountEditActivity.class);
                            intent.setAction("NEW");
                            intent.putExtra("algorithm", 0);
                            intent.putExtra("period", 1);
                            intent.putExtra("digits", 6);
                            intent.putExtra("locked", false);
                            startActivityForResult(intent, ACTION_NEW);
                        } else {
                            AccountsActivity.this.scanQRCode();
                        }
                    });
            Dialog dlg = builder.create();
            Window window = dlg.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wlp);
            dlg.show();
        });

        try {
            accounts = DataHelper.load(this);
        } catch (Exception e) {
            Utils.saveException("Loading accounts.", e);
            Snackbar.make(floatingButton, R.string.err_loading_accounts, Snackbar.LENGTH_LONG).show();
        }

        listView = findViewById(R.id.listView);
        listView.getRecyclerView().setVerticalScrollBarEnabled(true);
        listView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
                    try {
                        DataHelper.store(AccountsActivity.this, accounts);
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Utils.saveException("Dropping account", e);
                        Snackbar.make(floatingButton, R.string.err_dropping_account, Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

        setupListRecyclerView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == ACTION_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                saveExistingAccount(intent);
            }
            return;
        }

        if (requestCode == ACTION_NEW) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    saveFromParameters(intent);
                } catch (Exception e) {
                    Utils.saveException("Editing account", e);
                    Snackbar.make(floatingButton, R.string.err_editing_account, Snackbar.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
            return;
        }

        if (requestCode == ACTION_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    accounts = DataHelper.load(this);
                } catch (Exception e) {
                    Utils.saveException("Importing accounts", e);
                    Snackbar.make(floatingButton, R.string.err_importing_accounts, Snackbar.LENGTH_LONG).show();
                }
                adapter.setItemList(accounts);
                adapter.notifyDataSetChanged();
            }
        }

        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                AccountStruc e = new AccountStruc(intent.getStringExtra(Intents.Scan.RESULT));
                e.setCurrentOTP(DataHelper.OTP_NONE);
                accounts.add(new Pair<>(accounts.size(), e));
                DataHelper.store(this, accounts);

                adapter.notifyDataSetChanged();

                Snackbar.make(floatingButton, R.string.msg_account_added, Snackbar.LENGTH_LONG).show();
            } catch (Exception e) {
                Snackbar.make(floatingButton, getResources().getString(R.string.msg_invalid_qr_code) + " " + e, Snackbar.LENGTH_LONG).setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                    }
                }).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            getAboutDialog().show();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(AccountsActivity.this, SettingsActivity.class), ACTION_SETTINGS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.menu_edit, menu);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        adapter.notifyDataSetChanged();

        return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.action_delete) {
            return deleteAccount();

        } else if (id == R.id.action_edit) {
            editAccount();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (this.viewLongClicked != null) {
            this.viewLongClicked.setBackground(getResources().getDrawable(R.drawable.row_unselected));
        }
        this.viewLongClicked = null;
        this.actionMode = null;
        this.nextSelection = null;
    }

    private void setupListRecyclerView() {
        listView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountsListAdapter(this,this, accounts, R.layout.account_list_row, R.id.image, false);
        listView.setAdapter(adapter, true);
        listView.setCanDragHorizontally(false);
        listView.setCustomDragItem(new MyDragItem(this, R.layout.account_list_row));
    }

    @Override
    public void itemClicked(View view, int position) {
        if (actionMode != null) {
            actionMode.finish();
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean copyToClipboard =  pref.getBoolean("copy_clipboard", false);

        final AccountStruc account = accounts.get(position).second;
        String numOtp = account.getCurrentOTP();

        if (DataHelper.OTP_NONE.equals( account.getCurrentOTP())) {
            if (handlerTaskRow != null) {
                handlerTaskRow.interrupt();
            }
        } else {
            if (copyToClipboard) {
                copyToClipboard(numOtp);
            }
            return;
        }

        final int period = account.getPeriod();
        final ProgressBar progressBar = view.findViewById(R.id.progress);
        final int max = progressBar.getMax();
        final TextView txtCount = view.findViewById(R.id.txtcount);


        handlerRow = new Handler();
        handlerTaskRow = new MyRunnable() {
            int counter = 0;
            @Override
            public void run() {
                int progress = (int) (System.currentTimeMillis() / 1000) % period;
                if (counter == 0 || (progress % period) < 1) {
                    String numOtp = TOTPHelper.generate(account.getSecret(), account.getDigits(), account.getAlgorithm());
                    account.setCurrentOTP(numOtp);
                    adapter.notifyDataSetChanged();
                    if (counter == 0  && copyToClipboard) {
                        copyToClipboard(numOtp);
                    }
                }

                progressBar.setProgress(max - (progress * 100 *30/period));
                progressBar.setSecondaryProgress(max - counter); //***100
                txtCount.setText("" + (period - progress));
                counter = counter + 20;

                if (counter >= 3000 || interrupted) { //***30
                    progressBar.setProgress(0);
                    progressBar.setSecondaryProgress(0);
                    txtCount.setText("");
                    handlerRow.removeCallbacks(this);
                    account.setCurrentOTP(DataHelper.OTP_NONE);
                    adapter.notifyDataSetChanged();
                    return;
                }

                handlerRow.postDelayed(this, 200); //***1000
            }
        };
        handlerTaskRow.run();
    }

    @Override
    public boolean itemLongClicked(View view, int position) {
        if (actionMode != null) {
            actionMode.finish();
        }

        nextSelection = accounts.get(position);
        viewLongClicked = view;
        view.setBackground(getResources().getDrawable(R.drawable.row_selected));
        actionMode = startActionMode(AccountsActivity.this);
        return true;
    }

    private static class MyDragItem extends DragItem {
        public MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.textViewLabel)).getText();
            ((TextView) dragView.findViewById(R.id.textViewLabel)).setText(text);
            ((TextView) dragView.findViewById(R.id.textViewOTP)).setText(((TextView) clickedView.findViewById(R.id.textViewOTP)).getText());
            ((TextView) dragView.findViewById(R.id.textViewAccount)).setText(((TextView) clickedView.findViewById(R.id.textViewAccount)).getText());
            Drawable img = ((ImageView) clickedView.findViewById(R.id.imgLocked)).getDrawable();
            ((ImageView) dragView.findViewById(R.id.imgLocked)).setImageDrawable(img);
            dragView.findViewById(R.id.imgLocked).setVisibility(clickedView.findViewById(R.id.imgLocked).getVisibility());
            dragView.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void copyToClipboard(String numOtp) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("otp", numOtp);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(floatingButton, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show();
    }

    private void exit() {
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void editAccount() {
        AccountStruc account = nextSelection.second;
        Intent intent = new Intent(this, AccountEditActivity.class);
        intent.putExtra("label", account.getLabel());
        intent.putExtra("account", account.getAccount());
        intent.putExtra("issuer", account.getIssuer());
        String tmp = account.getAlgorithm();
        if ("SHA256".equalsIgnoreCase(tmp)) {
            intent.putExtra("algorithm", 1);
        } else if ("SHA512".equalsIgnoreCase(tmp)) {
            intent.putExtra("algorithm", 2);
        } else {
            intent.putExtra("algorithm", 0);
        }
        int intTmp = account.getPeriod();
        if (intTmp == 15) {
            intent.putExtra("period", 0);
        } else if (intTmp == 60) {
            intent.putExtra("period", 2);
        } else {
            intent.putExtra("period", 1);
        }
        intent.putExtra("digits", account.getDigits());
        intent.putExtra("locked", account.isFavourite());
        intent.putExtra("secret", new String(new Base32().encode(account.getSecret())));
        startActivityForResult(intent, ACTION_EDIT);
    }

    private void saveExistingAccount(Intent intent) {
        AccountStruc account = nextSelection.second;
        account.setLabel(intent.getStringExtra("label"));
        account.setAccount(intent.getStringExtra("account"));
        account.setIssuer(intent.getStringExtra("issuer"));
        account.setAlgorithm(intent.getStringExtra("algorithm"));
        account.setPeriod(intent.getIntExtra("period", 30));
        account.setDigits(intent.getIntExtra("digits", 6));
        account.setFavourite(intent.getBooleanExtra("locked", false));
        try {
            DataHelper.store(this, accounts);
        } catch (Exception e) {
            Utils.saveException("Editing account", e);
            Snackbar.make(floatingButton, R.string.err_editing_account, Snackbar.LENGTH_LONG).show();
        }
        adapter.notifyDataSetChanged();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private boolean deleteAccount() {
        if (nextSelection.second.isFavourite()) {
            Snackbar.make(floatingButton, R.string.mag_not_removed_locked, Snackbar.LENGTH_LONG).show();
            return true;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.button_remove) + nextSelection.second.getLabel() + "?");
        alert.setMessage(R.string.msg_confirm_delete);

        alert.setPositiveButton(R.string.button_remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                accounts.remove(nextSelection);
                try {
                    DataHelper.store(AccountsActivity.this, accounts);

                    Snackbar.make(floatingButton, R.string.msg_account_removed, Snackbar.LENGTH_LONG).setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                        }
                    }).show();
                } catch (Exception e) {
                    Utils.saveException("Deleting account.", e);
                    Snackbar.make(floatingButton, R.string.err_deleting_account, Snackbar.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
                actionMode.finish();
            }
        });

        alert.setNegativeButton(R.string.button_cancel, (dialog, whichButton) -> {
            dialog.cancel();
            actionMode.finish();
        });

        alert.show();
        return true;
    }

    private void saveFromParameters(Intent intent) throws Exception {
        String label = intent.getStringExtra("label");
        String algorithm = intent.getStringExtra("algorithm");
        int period = intent.getIntExtra("period", 30);
        int digits = intent.getIntExtra("digits", 6);
        String secret = intent.getStringExtra("secret");

        String data = "otpauth://totp/" + URLEncoder.encode(label, StandardCharsets.UTF_8.name())
                + (label.length() > 0 ? ":" : "")
                + URLEncoder.encode(intent.getStringExtra("account"), StandardCharsets.UTF_8.name()) + "?"
                + "issuer=" + intent.getStringExtra("issuer")
                + "&algorithm=" + URLEncoder.encode(algorithm, StandardCharsets.UTF_8.name())
                + "&period=" + period
                + "&digits=" + digits
                + "&secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8.name());

        AccountStruc account = new AccountStruc(data);
        accounts.add(new Pair<>(accounts.size(), account));

        DataHelper.store(this, accounts);
    }

    private Dialog getAboutDialog() {
        Dialog dialog = new Dialog(this){
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.about_dialog);

                ((TextView)findViewById(R.id.txtappname)).setText(getText(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
                findViewById(R.id.bierbaumer).setOnClickListener(view -> {
                    Uri uri = Uri.parse("https://github.com/0xbb");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });
                findViewById(R.id.linkcommons).setOnClickListener(view -> {
                    Uri uri = Uri.parse("https://commons.apache.org/proper/commons-codec/");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });
                findViewById(R.id.ZXing).setOnClickListener(view -> {
                    Uri uri = Uri.parse("https://github.com/zxing/zxing");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });
                findViewById(R.id.ZXingEmbedded).setOnClickListener(view -> {
                    Uri uri = Uri.parse("https://github.com/journeyapps/zxing-android-embedded");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });
                findViewById(R.id.draglistview).setOnClickListener(view -> {
                    Uri uri = Uri.parse("https://github.com/woxblom/DragListView");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });
            }
        };
        dialog.setTitle(R.string.menu_about);
        return dialog;
    }

    private static class MyRunnable implements Runnable {
        boolean interrupted = false;

        public void interrupt() {
            interrupted = true;
        }
        @Override
        public void run() {

        }
    }
}
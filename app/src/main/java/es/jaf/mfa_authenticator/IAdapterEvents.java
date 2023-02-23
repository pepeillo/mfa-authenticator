package es.jaf.mfa_authenticator;

import android.view.View;

public interface IAdapterEvents {
    void itemClicked(View view, int position);
    boolean itemLongClicked(View view, int position);
}

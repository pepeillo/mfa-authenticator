package es.jaf.mfa_authenticator;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

public class AccountsListAdapter extends DragItemAdapter<Pair<Integer, AccountStruc>, AccountsListAdapter.ViewHolder> {

    private final IAdapterEvents listener;
    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private final SpannableStringBuilder imgOtpNone;

    public AccountsListAdapter(Context context, IAdapterEvents listener, ArrayList<Pair<Integer, AccountStruc>> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        imgOtpNone = new SpannableStringBuilder().append(" ", new ImageSpan(context, R.drawable.dots), 0);
        this.listener = listener;
        mLayoutId = layoutId;

        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setHasStableIds(true);
        setItemList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =  LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        AccountStruc account = mItemList.get(position).second;
        if (DataHelper.OTP_NONE.equals(account.getCurrentOTP())) {
            holder.txtOtp.setText(imgOtpNone);
        } else {
            holder.txtOtp.setText(account.getCurrentOTP());
        }

        String text = account.getLabel();
        if (text == null || text.length() == 0) {
            text = account.getIssuer();
        }
        holder.txtLabel.setText(text);
        holder.itemView.setTag(text);
        holder.txtAccount.setText(account.getAccount());
        holder.imgLocked.setVisibility(account.isFavourite() ? View.VISIBLE : View.INVISIBLE);
    }

    public class ViewHolder extends DragItemAdapter.ViewHolder {
        public final TextView txtOtp;
        public final TextView txtLabel;
        public final TextView txtAccount;
        public final ImageView imgLocked;

        public ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            txtOtp = itemView.findViewById(R.id.textViewOTP);
            txtLabel = itemView.findViewById(R.id.textViewLabel);
            txtAccount = itemView.findViewById(R.id.textViewAccount);
            imgLocked = itemView.findViewById(R.id.imgLocked);
        }

        @Override
        public void onItemClicked(View view) {
            if (listener != null) {
                listener.itemClicked(view, getLayoutPosition());
            }
        }

        @Override
        public boolean onItemLongClicked(View view) {
            if (listener != null) {
                return listener.itemLongClicked(view, getLayoutPosition());
            }
            return true;
        }
    }
}
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

public class EntryAdapter extends DragItemAdapter<Pair<Integer, EntryObject>, EntryAdapter.ViewHolder> {

    private final IAdapterEvents listener;
    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private final SpannableStringBuilder imgOtpNone;

    public EntryAdapter(Context context, IAdapterEvents listener, ArrayList<Pair<Integer, EntryObject>> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
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

        EntryObject entry = mItemList.get(position).second;
        if (DataHelper.OTP_NONE.equals(entry.getCurrentOTP())) {
            holder.txtOtp.setText(imgOtpNone);
        } else {
            holder.txtOtp.setText(entry.getCurrentOTP());
        }

        String text = entry.getLabel();
        if (text == null || text.length() == 0) {
            text = entry.getIssuer();
        }
        holder.txtLabel.setText(text);
        holder.itemView.setTag(text);
        holder.txtAccount.setText(entry.getAccount());
        holder.imgLocked.setVisibility(entry.isLocked() ? View.VISIBLE : View.INVISIBLE);
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
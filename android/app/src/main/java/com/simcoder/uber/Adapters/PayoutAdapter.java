package com.simcoder.uber.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simcoder.uber.Objects.PayoutObject;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * Adapter responsible for displaying type of cars in the CustomerActivity.class
 */

public class PayoutAdapter  extends RecyclerView.Adapter<PayoutAdapter.viewHolders> {

    private List<PayoutObject> itemArrayList;

    public PayoutAdapter(List<PayoutObject> itemArrayList, Context context) {
        this.itemArrayList = itemArrayList;
    }

    @NotNull
    @Override
    public PayoutAdapter.viewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payout, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        return new viewHolders(layoutView);
    }

    /**
     * Bind view to holder, setting the text to
     * the design elements
     * @param position - current position of the recyclerView
     */
    @Override
    public void onBindViewHolder(final @NonNull viewHolders holder, int position) {
        holder.mAmount.setText(itemArrayList.get(position).getAmount());
        holder.mDate.setText(String.valueOf(itemArrayList.get(position).getDate()));
    }


    @Override
    public int getItemCount() {
        return this.itemArrayList.size();
    }

    /**
     * Responsible for handling the data of each view
     */
    static class viewHolders extends RecyclerView.ViewHolder {

        TextView    mAmount,
                mDate;
        viewHolders(View itemView) {
            super(itemView);
            mAmount = itemView.findViewById(R.id.amount_text);
            mDate = itemView.findViewById(R.id.date_text);
        }
    }
}
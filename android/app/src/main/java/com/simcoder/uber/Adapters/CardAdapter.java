package com.simcoder.uber.Adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simcoder.uber.Objects.CardObject;
import com.simcoder.uber.Payment.PaymentActivity;
import com.simcoder.uber.R;

import java.util.List;


/**
 * Adapter responsible for displaying type of cars in the CustomerActivity.class
 */

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.viewHolders> {

    private Activity activity;
    private List<CardObject> itemArrayList;

    public CardAdapter(List<CardObject> itemArrayList, Activity activity) {
        this.itemArrayList = itemArrayList;
        this.activity = activity;
    }

    @Override
    public CardAdapter.viewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        return new viewHolders(layoutView);
    }

    /**
     * Bind view to holder, setting the text to
     * the design elements
     *
     * @param position - current position of the recyclerView
     */
    @Override
    public void onBindViewHolder(final @NonNull viewHolders holder, int position) {
        holder.mName.setText(itemArrayList.get(position).getName());
        holder.mNumber.setText("**** **** **** " + itemArrayList.get(position).getLastDigits());
        holder.mDate.setText(itemArrayList.get(position).getExpMonth() + "/" + itemArrayList.get(position).getExpYear());

        switch (itemArrayList.get(position).getBrand()) {
            case "visa":
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.ic_visa));
                break;
            case "American Express":
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.ic_amex));
                break;
            case "mastercard":
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.ic_mastercard));
                break;
            case "discover":
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.ic_discover));
                break;
            case "jcb":
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.cio_ic_jcb));
                break;
            default:
                holder.mCard.setImageDrawable(activity.getDrawable(R.drawable.ic_credit_card_black_24dp));
                break;
        }

        if(itemArrayList.get(position).getDefaultCard()){
            holder.mDefault.setVisibility(View.VISIBLE);
        }else{
            holder.mDefault.setVisibility(View.INVISIBLE);
        }

        holder.mLayout.setOnClickListener(v -> {
            ((PaymentActivity)activity).initializeBottomSheetDialog(itemArrayList.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return this.itemArrayList.size();
    }


    /**
     * Responsible for handling the data of each view
     */
    static class viewHolders extends RecyclerView.ViewHolder {

        TextView mName,
                mNumber, mDate;
        ImageView mCard, mDefault;
        LinearLayout mLayout;

        viewHolders(View itemView) {
            super(itemView);
            mCard = itemView.findViewById(R.id.card_image);
            mNumber = itemView.findViewById(R.id.number_text);
            mName = itemView.findViewById(R.id.name_text);
            mDate = itemView.findViewById(R.id.date_text);
            mLayout = itemView.findViewById(R.id.card_layout);
            mDefault = itemView.findViewById(R.id.default_image);
        }
    }
}
package com.simcoder.uber.Adapters;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.simcoder.uber.Objects.RideObject;
import com.simcoder.uber.R;

import java.util.List;


/**
 * Adapter responsible for handling the display of the Cards to the user
 */
public class CardRequestAdapter extends ArrayAdapter<RideObject>{

    Context context;
    List<RideObject> items;

    public CardRequestAdapter(Context context, int resourceId, List<RideObject> items){
        super(context, resourceId, items);
        this.context = context;
        this.items = items;


    }

    /**
     * Populate the item_message with user in the current position
     *
     * Changes the message aspect if it is from the current user or the match
     *
     * @param position - position of the list
     */
    public View getView(int position, View convertView, ViewGroup parent){
        RideObject card_item = getItem(position);

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item__card_request, parent, false);
        }

        TextView mDistance = convertView.findViewById(R.id.distance);
        TextView mTime = convertView.findViewById(R.id.time);
        TextView mRatingText = convertView.findViewById(R.id.ratingText);

        CircularProgressBar mProgressBar = convertView.findViewById(R.id.circularProgressBar);


        if (card_item != null) {
            mDistance.setText(card_item.getCalculatedRideDistance());
            mTime.setText(card_item.getCalculatedTime() + " min");
            mRatingText.setText(card_item.getCustomer().getRatingString() );
        }


        final Handler ha=new Handler();
        ha.postDelayed(new Runnable() {

            @Override
            public void run() {
                //call function
                if (card_item != null) {
                    card_item.setTimePassed(card_item.getTimePassed() + (float)0.5);
                }
                if (card_item != null) {
                    mProgressBar.setProgress(card_item.getTimePassed());
                }

                if (card_item != null && card_item.getTimePassed() > 100) {
                    items.remove(card_item);

                    notifyDataSetChanged();
                }

                ha.postDelayed(this, 25);

            }
        }, 25);

        return convertView;

    }
}

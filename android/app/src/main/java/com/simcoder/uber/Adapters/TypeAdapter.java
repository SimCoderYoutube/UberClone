package com.simcoder.uber.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.simcoder.uber.Objects.TypeObject;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * Adapter responsible for displaying type of cars in the CustomerActivity.class
 */

public class TypeAdapter  extends RecyclerView.Adapter<TypeAdapter.viewHolders> {

    private Context context;
    private TypeObject selectedItem;
    private List<TypeObject> itemArrayList;
    private ArrayList<Double> data;

    public TypeAdapter(List<TypeObject> itemArrayList, Context context, ArrayList<Double> data) {
        this.itemArrayList = itemArrayList;
        selectedItem = itemArrayList.get(0);
        this.context = context;
        this.data = data;
    }

    @NotNull
    @Override
    public TypeAdapter.viewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_type, null, false);
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
        holder.mName.setText(itemArrayList.get(position).getName());
        holder.mPeople.setText(String.valueOf(itemArrayList.get(position).getPeople()));
        holder.mImage.setImageDrawable(itemArrayList.get(position).getImage());

        if(selectedItem.equals(itemArrayList.get(position))){
            holder.mLayout.setBackgroundColor(context.getResources().getColor(R.color.lightGrey));
        }else{
            holder.mLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        holder.mLayout.setOnClickListener(v -> {
            selectedItem = itemArrayList.get(holder.getAdapterPosition());
            notifyDataSetChanged();
        });

    }

    public TypeObject getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(TypeObject selectedItem) {
        this.selectedItem = selectedItem;
    }

    @Override
    public int getItemCount() {
        return this.itemArrayList.size();
    }


    public void setData(ArrayList<Double> data) {
        this.data = data;
    }


    /**
     * Responsible for handling the data of each view
     */
    static class viewHolders extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mPeople;
        ImageView   mImage;
        LinearLayout mLayout;
        viewHolders(View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.image);
            mPeople = itemView.findViewById(R.id.people);
            mName = itemView.findViewById(R.id.name);
            mLayout = itemView.findViewById(R.id.layout);
        }
    }
}
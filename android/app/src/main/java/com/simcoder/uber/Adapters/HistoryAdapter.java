package com.simcoder.uber.Adapters;

import android.content.Context;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.simcoder.uber.History.HistorySingleActivity;
import com.simcoder.uber.R;
import com.simcoder.uber.Objects.RideObject;

import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * Adapter responsible for displaying history items in the HistoryActivity.class
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolders> implements OnMapReadyCallback {

    private List<RideObject> itemList;
    private Context context;

    public HistoryAdapter(List<RideObject> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    @NotNull
    @Override
    public HistoryViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        return new HistoryViewHolders(layoutView);
    }

    @Override
    public void onBindViewHolder(@NotNull HistoryViewHolders holder, final int position) {

        holder.bindView(position);


        holder.mMask.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
            Bundle b = new Bundle();
            b.putString("rideId", itemList.get(position).getId());
            intent.putExtras(b);
            v.getContext().startActivity(intent);
        });
    }
    @Override
    public int getItemCount() {
        return this.itemList.size();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setAllGesturesEnabled(false);

    }


    /**
     * Responsible for handling the data of each view
     */
    class HistoryViewHolders extends RecyclerView.ViewHolder implements OnMapReadyCallback {

        TextView rideId;
        TextView time;
        TextView mCar;
        TextView mPrice;
        MapView mapView;
        GoogleMap map;
        CardView mCard;
        View mMask;
        HistoryViewHolders(View itemView) {
            super(itemView);

            rideId = itemView.findViewById(R.id.rideId);
            time = itemView.findViewById(R.id.time);
            mCar = itemView.findViewById(R.id.car);
            mPrice = itemView.findViewById(R.id.price);
            mCard = itemView.findViewById(R.id.card_view);
            mMask = itemView.findViewById(R.id.mask_layout);
            mapView = itemView.findViewById(R.id.map);
            if (mapView != null) {
                // Initialise the MapView
                mapView.onCreate(null);
                // Set the map ready callback to receive the GoogleMap object
                mapView.getMapAsync(this);
            }
        }
        @Override
        public void onMapReady(GoogleMap googleMap) {
            googleMap.getUiSettings().setAllGesturesEnabled(false);
            MapsInitializer.initialize(context);
            map = googleMap;
            googleMap.setMapStyle(new MapStyleOptions(context.getResources()
                    .getString(R.string.style_json)));
            setMapLocation();
        }

        /**
         * Sets markers of pickup and destination to the holder's map
         */
        private void setMapLocation() {
            if (map == null) return;

            RideObject data = (RideObject) mapView.getTag();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(data.getDestination().getCoordinates());
            builder.include(data.getPickup().getCoordinates());
            LatLngBounds bounds = builder.build();


            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_finish)).position(data.getDestination().getCoordinates()).title("destination"));
            map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start)).position(data.getPickup().getCoordinates()).title("pickup"));
            map.getUiSettings().setAllGesturesEnabled(false);
        }

        /**
         * Bind view to holder, setting the text to
         * the design elements
         * @param pos
         */
        private void bindView(int pos) {
            RideObject item = itemList.get(pos);

            time.setText(item.getDate());
            if(item.getDriver() != null){
                mCar.setText(item.getDriver().getCar());
            }
            mPrice.setText(item.getPriceString() + context.getString(R.string.money_type));

            rideId.setText(item.getId());

            mapView.setTag(item);
            setMapLocation();
        }
    }
}
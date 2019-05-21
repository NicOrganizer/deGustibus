package com.madness.deliveryman.incoming;

import android.content.Context;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.support.design.animation.Positioning;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.madness.deliveryman.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class IncomingHolder extends RecyclerView.ViewHolder {

    public TextView status, customer, restaurant, date, hour, price;
    public Button button, refuse, clientPos, restaurantPos;

    public String cli, res;

    public IncomingHolder(final View itemView) {
        super(itemView);
        status = itemView.findViewById(R.id.status);
        customer = itemView.findViewById(R.id.customer);
        //customerAddress = itemView.findViewById(R.id.custAddress);
        restaurant = itemView.findViewById(R.id.restaurant);
        //restaurantAddress = itemView.findViewById(R.id.restAddress);
        date = itemView.findViewById(R.id.date);
        hour = itemView.findViewById(R.id.hour);
        price = itemView.findViewById(R.id.price);
        button = itemView.findViewById(R.id.orderButton);
        refuse = itemView.findViewById(R.id.refuseButton);
        clientPos = itemView.findViewById(R.id.customerMaps);
        restaurantPos = itemView.findViewById(R.id.restaurantMaps);

    }
}

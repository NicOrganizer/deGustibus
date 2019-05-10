package com.madness.degustibus.order;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.madness.degustibus.R;

import java.util.ArrayList;

public class DishHolder extends RecyclerView.ViewHolder {

    private static final String TAG = DishHolder.class.getSimpleName();
    public TextView title, description, price, quantity;
    public ImageView image;
    public Button buttonPlus;
    public Button buttonMinus;

    private ArrayList<Dish> dishList;

    public DishHolder(final View view) {
        super(view);
        title = view.findViewById(R.id.rest_title);
        description = view.findViewById(R.id.rest_description);
        price = view.findViewById(R.id.price);
        quantity = view.findViewById(R.id.quantity);
        image = view.findViewById(R.id.rest_imageView);
        buttonMinus = view.findViewById(R.id.buttonPlus);
        buttonPlus = view.findViewById(R.id.buttonMinus);
    }

    public Dish getDish(int position) {
        return dishList.get(position);
    }
}
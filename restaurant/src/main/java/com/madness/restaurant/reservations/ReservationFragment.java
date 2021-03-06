package com.madness.restaurant.reservations;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.madness.restaurant.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReservationFragment class
 */
public class ReservationFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private FirebaseDatabase db;
    private DatabaseReference databaseReference;
    private ValueEventListener emptyListener;
    private FirebaseRecyclerAdapter adapter;
    private FirebaseUser user;
    private Fragment fragment;

    public ReservationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    /* During the creation of the view the title is set and layout is generated */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_reservations, container, false);
        getActivity().setTitle(getResources().getString(R.string.title_Reservations));

        db = FirebaseDatabase.getInstance();
        databaseReference = db.getReference();
        recyclerView = rootView.findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        rootView.findViewById(R.id.progress_horizontal).setVisibility(View.VISIBLE);
        final Query query = databaseReference.child("orders").orderByChild("restaurantID").equalTo(user.getUid());

        /*
        FirebaseRecyclerOptions<ReservationClass> options =
                new FirebaseRecyclerOptions.Builder<ReservationClass>()
                        .setQuery(query, ReservationClass.class)
                        .build();*/

        FirebaseRecyclerOptions<ReservationClass> options =
                new FirebaseRecyclerOptions.Builder<ReservationClass>()
                        .setQuery(query, new SnapshotParser<ReservationClass>() {
                            @NonNull
                            @Override
                            public ReservationClass parseSnapshot(@NonNull DataSnapshot snapshot) {
                                List<ItemClass> cart = new ArrayList<>();
                                for (DataSnapshot obj : snapshot.child("cart").getChildren()) {
                                    ItemClass item = new ItemClass(Integer.parseInt(obj.child("quantity").getValue().toString()),
                                            obj.getKey(),
                                            obj.child("name").getValue().toString(),
                                            Integer.parseInt(obj.child("rating").getValue().toString())
                                    );
                                    cart.add(item);
                                }

                                ReservationClass order = new ReservationClass(snapshot.getKey(),
                                        snapshot.child("customerID").getValue().toString(),
                                        snapshot.child("restaurantID").getValue().toString(),
                                        snapshot.child("deliverymanID").getValue().toString(),
                                        snapshot.child("deliveryDate").getValue().toString(),
                                        snapshot.child("deliveryHour").getValue().toString(),
                                        snapshot.child("totalPrice").getValue().toString(),
                                        snapshot.child("customerAddress").getValue().toString(),
                                        snapshot.child("restaurantAddress").getValue().toString(),
                                        snapshot.child("status").getValue().toString(),
                                        snapshot.child("riderComment").getValue().toString(),
                                        snapshot.child("riderRating").getValue().toString(),
                                        snapshot.child("restaurantComment").getValue().toString(),
                                        snapshot.child("restaurantRating").getValue().toString(),
                                        cart
                                );

                                return order;
                            }
                        })
                        .build();

        adapter = new FirebaseRecyclerAdapter<ReservationClass, ReservationHolder>(options) {

            @NonNull
            @Override
            public ReservationHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.reservation_listitem, viewGroup, false);

                return new ReservationHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ReservationHolder holder, final int position, @NonNull final ReservationClass model) {
                /* Set the name of the customer */
                databaseReference.child("customers").child(model.getCustomerID()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            String customerName = objectMap.get("name").toString();
                            holder.customer.setText(customerName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                StringBuilder listOfDishes = new StringBuilder();
                for (int i = 0; i < model.getCart().size(); i++) {
                    listOfDishes.append(model.getCart().get(i).getQuantity());
                    listOfDishes.append(" x ");
                    listOfDishes.append(model.getCart().get(i).getName());
                }

                holder.description.setText(listOfDishes.toString());
                holder.price.setText(model.getTotalPrice());
                holder.date.setText(model.getDeliveryDate());
                holder.hour.setText(model.getDeliveryHour());
                holder.status.setText(model.getStatus());
                if (model.getStatus().equals("new")) {
                    holder.status.setText(R.string.status_new);
                    holder.status.setTextColor(getResources().getColor(R.color.theme_colorTertiary));
                } else if (model.getStatus().equals("refused")) {
                    holder.status.setText(R.string.status_refused);
                    holder.status.setTextColor(Color.RED);
                } else if (model.getStatus().equals("incoming")) {
                    holder.status.setText(R.string.status_incoming);
                    holder.status.setTextColor(getResources().getColor(R.color.theme_colorTertiaryDark));
                } else if (model.getStatus().equals("done")) {
                    holder.status.setText(R.string.status_done);
                    holder.status.setTextColor(Color.BLACK);
                } else if (model.getStatus().equals("delivering")) {
                    holder.status.setText(getString(R.string.status_deliverying));
                    holder.status.setTextColor(getResources().getColor(R.color.theme_colorAccent));
                } else if (model.getStatus().equals("elaboration")) {
                    holder.status.setText(R.string.status_elaboration);
                    holder.status.setTextColor(getResources().getColor(R.color.theme_colorTertiaryDark));
                }

                holder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String orderID = getRef(position).getKey();
                        try {
                            Bundle bundle = new Bundle();
                            bundle.putString("orderID", orderID);
                            fragment = null;
                            fragment = RiderChoiceFragment.class.newInstance();
                            fragment.setArguments(bundle);

                        } catch (Exception e) {
                            Log.e("MAD", "editProfileClick: ", e);
                        }

                        ((FragmentActivity) getContext()).getSupportFragmentManager().beginTransaction()
                                .replace(R.id.flContent, fragment, " Order")
                                .addToBackStack("Reservation")
                                .commit();
                    }
                });
                rootView.findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
            }

        };
        recyclerView.setAdapter(adapter);

        /* Listener to check if the recycler view is empty */
        emptyListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    rootView.findViewById(R.id.emptyLayout).setVisibility(View.GONE);
                } else {
                    rootView.findViewById(R.id.emptyLayout).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return rootView;
    }

    /* Here is set the click listener on the floating button */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        databaseReference.removeEventListener(emptyListener);
    }

}
package com.madness.restaurant.reservations;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.madness.restaurant.R;
import com.madness.restaurant.haversine.ComputeDistance;
import com.madness.restaurant.haversine.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiderChoiceFragment extends Fragment {

    /* Database references */
    private DatabaseReference databaseReference;
    private DatabaseReference summaryReference;
    private DatabaseReference customerReference;
    private DatabaseReference riderReference;

    /* Value Event Listeners */
    private ValueEventListener summaryListener;
    private ValueEventListener locationListener;
    private ValueEventListener customerListener;
    private ValueEventListener riderListener;
    private GeoQueryEventListener eventListener;

    /* Firebase and GeoFire */
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerView;
    private Geocoder geocoder;
    private GeoQuery geoQuery;

    private LinearLayoutManager linearLayoutManager;
    private RiderAdapter adapter;
    private Point restaurant;
    private RiderComparable rider;
    private boolean isNew;
    private View view;
    private OrderData order;

    public RiderChoiceFragment() {
        // Required empty public constructor
    }

    /* In the onCreate method all variables containing useful information are set in a way that other
     * methods can use them once called.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        order = new OrderData();
    }

    /* The onCreateView allows to inflate the view of the fragment, in particular here are load information
     * from Firebase related to the order and the riders in case is a new order.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_rider_choice, container, false);
        getActivity().setTitle(getResources().getString(R.string.title_Order));

        recyclerView = rootView.findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        view = rootView;

        String orderID = this.getArguments().getString("orderID");
        /* Here data about the order is loaded into the first part of the fragment, then if loadData
         * returns true (this happens in case is a new order) the part related to the rider choice is
         * displayed and computed, else no other operation is perfomed.
         */
        loadData(orderID, rootView, new isNewCallback() {
            @Override
            public void onCallback(boolean value) {
                if (value) {
                    convertLocation(new RestPositionCallback() {
                        @Override
                        public void onCallback(String value) {
                            geocoder = new Geocoder(getContext(), Locale.getDefault());
                            List<Address> fromLocationName = null;
                            Double latitude = null;
                            Double longitude = null;
                            try {
                                fromLocationName = geocoder.getFromLocationName(value, 1);
                                if (fromLocationName != null && fromLocationName.size() > 0) {
                                    Address a = fromLocationName.get(0);
                                    latitude = a.getLatitude();
                                    longitude = a.getLongitude();

                                    restaurant = new Point();
                                    restaurant.setLatitude(latitude);
                                    restaurant.setLongitude(longitude);
                                }
                            } catch (Exception e) {
                                Log.e("MAD", "onCallback: ", e);
                            }
                            loadAdapter();
                        }
                    });
                }
            }
        });
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            summaryReference.removeEventListener(summaryListener);
            if (eventListener != null) {
                geoQuery.removeAllListeners();
            }
            databaseReference.child("restaurants").child(user.getUid()).removeEventListener(locationListener);
            customerReference.removeEventListener(customerListener);
            riderReference.removeEventListener(riderListener);
        } catch (Exception e) {
            Log.e("MAD", "onDetach: ", e);
        }
    }

    /* Retrieve data from Firebase and load it into the summary at the beginning of the fragment */
    private void loadData(final String id, final View view, final isNewCallback callback) {
        /* Items of the view are here retrieved to be populated during the firebase request */
        final TextView status = view.findViewById(R.id.status);
        final TextView customer = view.findViewById(R.id.customer);
        final TextView description = view.findViewById(R.id.description);
        final TextView date = view.findViewById(R.id.date);
        final TextView hour = view.findViewById(R.id.hour);
        final TextView price = view.findViewById(R.id.price);
        final Button button = view.findViewById(R.id.orderButton);
        final Button refuse = view.findViewById(R.id.refuseButton);

        summaryReference = databaseReference.child("orders").child(id);
        summaryListener = summaryReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Map<String, Object> orderData = (HashMap<String, Object>) dataSnapshot.getValue();
                order.setCustomerID(orderData.get("customerID").toString());
                order.setCustomerAddress(orderData.get("customerAddress").toString());
                order.setDeliveryDate(orderData.get("deliveryDate").toString());
                order.setDeliveryHour(orderData.get("deliveryHour").toString());
                order.setRestaurantID(orderData.get("restaurantID").toString());
                order.setTotalPrice(orderData.get("totalPrice").toString());
                order.setDescription(orderData.get("description").toString());
                order.setStatus(orderData.get("status").toString());
                order.setOrderKey(dataSnapshot.getKey());

                /* Set the name of the customer */
                customerReference = databaseReference.child("customers").child(orderData.get("customerID").toString());
                customerListener = customerReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            String customerName = objectMap.get("name").toString();
                            customer.setText(customerName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

                /* Set other fields and update correctly the current status */
                description.setText(orderData.get("description").toString());
                price.setText(orderData.get("totalPrice").toString());
                date.setText(orderData.get("deliveryDate").toString());
                hour.setText(orderData.get("deliveryHour").toString());
                if (orderData.get("status").toString().equals("new")) {
                    refuse.setVisibility(View.VISIBLE);
                    status.setText(R.string.status_new);
                    refuse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            NewNotificationClass notification = new NewNotificationClass(getContext());
                            notification.refuseAndNotify(order);
                            view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                            view.findViewById(R.id.recyclerView).setVisibility(View.GONE);
                        }
                    });
                    isNew = true;
                } else if (orderData.get("status").toString().equals("refused")) {
                    refuse.setVisibility(View.GONE);
                    view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                    status.setText(R.string.status_refused);
                    isNew = false;
                } else if (orderData.get("status").toString().equals("incoming")) {
                    refuse.setVisibility(View.GONE);
                    view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                    status.setText(R.string.status_elaboration);
                    isNew = false;
                } else if (orderData.get("status").toString().equals("done")) {
                    refuse.setVisibility(View.GONE);
                    view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                    status.setText(R.string.status_done);
                    isNew = false;
                } else if (orderData.get("status").toString().equals("delivering")) {
                    refuse.setVisibility(View.GONE);
                    view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                    status.setText(getString(R.string.status_deliverying));
                    isNew = false;
                } else if (orderData.get("status").toString().equals("elaboration")) {
                    refuse.setVisibility(View.GONE);
                    view.findViewById(R.id.select_rider).setVisibility(View.GONE);
                    status.setText(R.string.status_elaboration);
                    isNew = false;
                }

                /* Give two different behaviours to the same fragment: if isNew is true it will display
                 * the section related to the riders choice, while if the order is not a new order it will
                 * be simply a review of the past order.
                 */
                if (isNew) {
                    callback.onCallback(isNew);
                } else {
                    view.findViewById(R.id.layout).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
                    callback.onCallback(isNew);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /* This method allows to download data from Firebase through different calls to Event Listeners
     * at the end the custom Adapter is populated and are present also some methods for data change/delete.
     */
    private void loadAdapter() {
        /* Get all the riders in a certain range (here defined in 5 kilometers) */
        getRiders(new GetRidersCallback() {
            /* Save the riders in a List of RiderComparable type */
            List<RiderComparable> list = new ArrayList<>();

            @Override
            public void onCallback(RiderComparable rider) {
                boolean exists = false;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getName().equals(rider.getName())) {
                        exists = true;
                        list.set(i, rider);
                    }
                }
                if(exists) {
                    adapter.updateData(list);
                } else {
                    list.add(rider);
                    /* Sort the riders in the list according to an ascending order (distance) */
                    Collections.sort(list, new Comparator<RiderComparable>() {
                        public int compare(RiderComparable obj1, RiderComparable obj2) {
                            // ## Ascending order
                            return Double.valueOf(obj1.getDistance()).compareTo(Double.valueOf(obj2.getDistance()));
                        }
                    });
                    /* Set the adapter and show the recycler view while make invisible the progress bar */
                    adapter = new RiderAdapter(getContext(), view, list, order);
                    recyclerView.setAdapter(adapter);
                    view.findViewById(R.id.layout).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
                }

            }

            /* On update method finds the rider which was update (or has been inserted in the GeoFire radius */
            @Override
            public void onUpdate(RiderComparable rider) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getName().equals(rider.getName())) {
                        list.set(i, rider);
                    }
                }
                Collections.sort(list, new Comparator<RiderComparable>() {
                    public int compare(RiderComparable obj1, RiderComparable obj2) {
                        return Double.valueOf(obj1.getDistance()).compareTo(Double.valueOf(obj2.getDistance()));
                    }
                });
                /* This method of the adapter reload all the riders in order to update current shown items */
                adapter.updateData(list);
            }

            /* On exit method is called once a rider goes out of the GeoFire radius */
            @Override
            public void onExit(RiderComparable rider) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getKey().equals(rider.getKey())) {
                        list.remove(i);
                    }
                }
                adapter.updateData(list);
            }
        });
    }

    /* The convertLocation method is used to convert the restaurant address (written) into a set of GPS
     * coordinates which are obtained through a callback after a query on the database
     */
    private void convertLocation(final RestPositionCallback callback) {
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> rest = (HashMap<String, Object>) dataSnapshot.getValue();
                callback.onCallback(rest.get("address").toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        databaseReference.child("restaurants").child(user.getUid()).addValueEventListener(locationListener);
    }

    /* This method retrieves data about the riders */
    private void retrieveData(String key, final DataRetrieveCallback callback) {
        riderReference = databaseReference.child("riders").child(key);
        riderListener = riderReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> user = (HashMap<String, Object>) dataSnapshot.getValue();
                callback.onCallback(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /* This method retrieves all the riders which are in the GeoFire radius */
    private void getRiders(final GetRidersCallback callback) {
        GeoFire geoFire = new GeoFire(databaseReference.child("positions"));
        /* This GeoQuery retrieves all the riders whose distance is in 5 kilometers from the restaurant location */
        geoQuery = geoFire.queryAtLocation(new GeoLocation(restaurant.getLatitude(), restaurant.getLongitude()), 5);

        eventListener = new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                retrieveData(key, new DataRetrieveCallback() {
                    @Override
                    public void onCallback(Map user) {
                            /* This method retrieves the information of the rider and will add them to the item to be passed to the adapter */
                            rider = null;
                            rider = new RiderComparable();
                            rider.setAvailable((boolean) user.get("available"));
                            rider.setName(user.get("name").toString());
                            try {
                                rider.setPhoto(user.get("photo").toString());
                            } catch (Exception e) {
                                rider.setPhoto("default");
                            }
                            rider.setKey(key);
                            Point customer = new Point();
                            customer.setLatitude(location.latitude);
                            customer.setLongitude(location.longitude);

                            // Get haversine class and call method to calculate distance, then display it on the recycler view
                            ComputeDistance computeDistance = new ComputeDistance();
                            rider.setDistance(computeDistance.getDistance(customer, restaurant));

                            try {
                                rider.setCount(Integer.valueOf(user.get("count").toString()));
                                rider.setRating(Float.valueOf(user.get("rating").toString()));
                            }catch (Exception e){
                                rider.setCount(0);
                                rider.setRating(0);
                            }


                            callback.onCallback(rider);
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                /* Case where a rider exits the geofire radius */
                rider = null;
                rider = new RiderComparable();
                rider.setKey(key);
                callback.onExit(rider);
            }

            @Override
            public void onKeyMoved(final String key, final GeoLocation location) {
                /* Same thing as key entered is performed since is similar to a new incoming but the callback
                 * method is another because it just update that modified rider.
                 */
                retrieveData(key, new DataRetrieveCallback() {
                    @Override
                    public void onCallback(Map user) {
                        rider = null;
                        rider = new RiderComparable();
                        rider.setAvailable((boolean) user.get("available"));
                        rider.setName(user.get("name").toString());
                        rider.setPhoto(user.get("photo").toString());
                        rider.setKey(key);
                        Point customer = new Point();
                        customer.setLatitude(location.latitude);
                        customer.setLongitude(location.longitude);

                        // Get haversine class and call method to calculate distance, then display it on the recycler view
                        ComputeDistance computeDistance = new ComputeDistance();
                        rider.setDistance(computeDistance.getDistance(customer, restaurant));

                        callback.onUpdate(rider);
                    }
                });
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        };

        geoQuery.addGeoQueryEventListener(eventListener);
    }

    /* Interfaces for callbacks */
    public interface RestPositionCallback {
        void onCallback(String value);
    }

    public interface DataRetrieveCallback {
        void onCallback(Map user);
    }

    public interface GetRidersCallback {
        void onCallback(RiderComparable rider);

        void onUpdate(RiderComparable rider);

        void onExit(RiderComparable rider);
    }

    public interface isNewCallback {
        void onCallback(boolean value);
    }
}

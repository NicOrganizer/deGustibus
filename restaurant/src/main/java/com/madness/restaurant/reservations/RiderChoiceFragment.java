package com.madness.restaurant.reservations;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.madness.restaurant.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RiderChoiceFragment extends Fragment {

    private DatabaseReference databaseReference;
    private ValueEventListener summaryListener;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;

    public RiderChoiceFragment() {
        // Required empty public constructor
    }

    /* During the creation of the view the title is set and layout is generated */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_rider_choice, container, false);
        getActivity().setTitle("Scegli fattorino"); // TODO: string

        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        String orderID = this.getArguments().getString("orderID");
        loadData(orderID, rootView);

        //loadAdapter();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        databaseReference.removeEventListener(summaryListener);
    }



    /* Retrieve data from Firebase and load it into the summary at the beginning of the fragment */
    private void loadData(final String id, final View view) {
        final TextView status = view.findViewById(R.id.status);
        final TextView customer = view.findViewById(R.id.customer);
        final TextView description = view.findViewById(R.id.description);
        final TextView date = view.findViewById(R.id.date);
        final TextView hour = view.findViewById(R.id.hour);
        final TextView price = view.findViewById(R.id.price);
        final Button button = view.findViewById(R.id.orderButton);
        final Button refuse = view.findViewById(R.id.refuseButton);

        summaryListener = databaseReference.child("orders").child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> orderData = (HashMap<String, Object>) dataSnapshot.getValue();

                /* Set the name of the customer */
                databaseReference.child("customers").child(orderData.get("customerID").toString()).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    button.setVisibility(View.VISIBLE);
                    status.setText(R.string.status_new);
                    refuse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refuse(id);
                        }
                    });
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            accept(id);
                        }
                    });
                } else if (orderData.get("status").toString().equals("refused")) {
                    refuse.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    status.setText(R.string.status_refused);
                } else if (orderData.get("status").toString().equals("incoming")) {
                    refuse.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    status.setText(R.string.status_elaboration);
                } else if (orderData.get("status").toString().equals("done")) {
                    refuse.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    status.setText(R.string.status_done);
                } else if (orderData.get("status").toString().equals("delivering")) {
                    refuse.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    status.setText(getString(R.string.status_deliverying));
                } else if (orderData.get("status").toString().equals("elaboration")) {
                    refuse.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    status.setText(R.string.status_elaboration);
                }

                view.findViewById(R.id.layout).setVisibility(View.VISIBLE);
                view.findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void refuse(final String orderID) {
        Query refuseQuery = databaseReference.child("orders").child(orderID);

        refuseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    /* Set order as refused */
                    databaseReference.child("restaurants").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                objectMap.put("status", "refused");
                                databaseReference.child("orders").child(dataSnapshot.getKey()).updateChildren(objectMap);

                                /* Send notification to user */
                                final Map<String, Object> newNotification = new HashMap<String, Object>();
                                newNotification.put("type", getString(R.string.typeNot_refused));

                                Map<String, Object> restaurantMap = (HashMap<String, Object>) snapshot.getValue();
                                String restaurantName = restaurantMap.get("name").toString();
                                newNotification.put("description", getString(R.string.desc1) + restaurantName);

                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                Date date = new Date();
                                newNotification.put("date", dateFormat.format(date));

                                databaseReference.child("notifications").child(objectMap.get("customerID").toString()).push().setValue(newNotification);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void accept(final String orderID) {
        Query selectDeliveryman = databaseReference.child("riders").limitToFirst(1);

        selectDeliveryman.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    /* Rider selection */
                    String temp = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        temp = snapshot.getKey();

                    }
                    final String riderID = temp;

                    Query updateQuery = databaseReference.child("orders").child(orderID);
                    updateQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            objectMap.put("status", "incoming");
                            objectMap.put("deliverymanID", riderID);
                            databaseReference.child("orders").child(dataSnapshot.getKey()).updateChildren(objectMap);
                            incomingNotifications(objectMap.get("restaurantID").toString(), riderID, objectMap.get("customerID").toString(), orderID);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    /* No riders available error */
                    Query updateQuery = databaseReference.child("orders").child(orderID);

                    updateQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                            databaseReference.child("restaurants").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                        objectMap.put("status", "done");
                                        databaseReference.child("orders").child(dataSnapshot.getKey()).updateChildren(objectMap);

                                        /* Send notification to user */
                                        final Map<String, Object> newNotification = new HashMap<String, Object>();
                                        newNotification.put("type", getString(R.string.typeNot_noRider));

                                        Map<String, Object> restaurantMap = (HashMap<String, Object>) snapshot.getValue();
                                        String restaurantName = restaurantMap.get("name").toString();
                                        newNotification.put("description", getString(R.string.desc1) + restaurantName + getString(R.string.desc2));

                                        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                        Date date = new Date();
                                        newNotification.put("date", dateFormat.format(date));

                                        databaseReference.child("notifications").child(objectMap.get("customerID").toString()).push().setValue(newNotification);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void incomingNotifications(String restaurantID, final String riderID, final String customerID, final String orderID) {
        databaseReference.child("restaurants").child(restaurantID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    /* Send notification to user */
                    final Map<String, Object> newNotification = new HashMap<String, Object>();
                    newNotification.put("type", getString(R.string.typeNot_accepted));

                    Map<String, Object> restaurantMap = (HashMap<String, Object>) snapshot.getValue();
                    String restaurantName = restaurantMap.get("name").toString();
                    newNotification.put("description", getString(R.string.desc3) + orderID.substring(1, 6) + getString(R.string.desc4) + restaurantName);

                    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    Date date = new Date();
                    newNotification.put("date", dateFormat.format(date));

                    databaseReference.child("notifications").child(customerID).push().setValue(newNotification).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            /* Send notification to rider */
                            final Map<String, Object> notificationRider = new HashMap<String, Object>();
                            notificationRider.put("type", getString(R.string.typeNot_incoming));
                            notificationRider.put("description", getString(R.string.desc5) + orderID.substring(1, 6) + getString(R.string.desc6));
                            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            notificationRider.put("date", dateFormat.format(date));

                            databaseReference.child("notifications").child(riderID).push().setValue(notificationRider);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}

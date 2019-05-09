package com.madness.degustibus.order;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.madness.degustibus.R;
import com.madness.degustibus.picker.TimePickerFragment;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class CompletedOrderFragment extends Fragment {

    private TextView productsPrice;
    private TextView shippingPrice;
    private TextView totalPrice;
    private Button complete_btn;
    private Fragment fragment;
    private ArrayList<Dish> dishList = new ArrayList<>();
    private HashMap<String,String> order=new HashMap<>();
    private Dish dish;
    private String descr = "";
    private double prodPrice= 0.0;
    private double shipPrice= 2.5;
    private double totPrice = 0.0;
    private RecyclerView recyclerView;
    private CartDataAdapter mAdapter;
    private SharedPreferences pref;
    private LinearLayoutManager linearLayoutManager;
    private FirebaseRecyclerAdapter adapter;
    private DatabaseReference databaseRef;
    private FirebaseUser user;
    private float totalAmount;
    private TextView totalAmountTextView;
    private EditText customerAddress;



    public CompletedOrderFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_completed_order, container, false);
        getActivity().setTitle("Complete order");   //TODO string

        recyclerView = rootView.findViewById(R.id.recyclerViewOrderCompleted);
        complete_btn = rootView.findViewById(R.id.confirm_btn);
        totalAmountTextView = rootView.findViewById(R.id.total_price);
        customerAddress = rootView.findViewById(R.id.costumer_address);

        final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        //recyclerView.setHasFixedSize(true);

        user = FirebaseAuth.getInstance().getCurrentUser();
        //click on complete order create order and delete cart objects

        complete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(totalAmount == 0){
                    Toast.makeText(getContext(), "No dishes selected", Toast.LENGTH_SHORT).show(); //TODO strings
                }else{
                    boolean doOItOnce = true;
                    final ReservationClass reservation = new ReservationClass();
                    reservation.setCustomerAddress(customerAddress.getText().toString());
                    reservation.setCustomerID(user.getUid());
                    reservation.setDeliverymanID("null");

                    // store the selected dishes in the cart of the user
                    for(Dish dish: dishList){             // for each dish in the dailyoffer
                        if(dish.quantity > 0) {              // keep only the selected ones


                            if (doOItOnce){// retrieve restaurant address
                                FirebaseDatabase.getInstance().getReference()
                                        .child("restaurants")
                                        .child(dish.restaurant)
                                        .child("address").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        reservation.setRestaurantAddress(dataSnapshot.getValue().toString());
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                                reservation.setRestaurantID(dish.restaurant);
                                doOItOnce = false;
                            }

                            /*DialogFragment timePicker = new TimePickerFragment();
                            timePicker.show();*/

                            /*reservation.set

                            // we use `updateChildren()` since the user can easily
                            // update the selected quantity overwriting the previous
                            // cart item in the database. In addition it avoids duplicated elements
                            // in the db

                            FirebaseDatabase.getInstance().getReference()
                                    .child("customers")
                                    .child(user.getUid())
                                    .child("cart").updateChildren(cartItem);*/
                        }else{

                            // it could appen that the data was previously stored,
                            // if so remove the item
                            FirebaseDatabase.getInstance().getReference()
                                    .child("customers")
                                    .child(user.getUid())
                                    .child("cart").child(dish.identifier).removeValue();
                        }
                    }

                    Toast.makeText(getContext(), "done!", Toast.LENGTH_SHORT).show(); //TODO strings
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStackImmediate("HOME", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }


            }
        });

        // TODO change it
        /*complete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    fragment = null;
                    Class fragmentClass;
                    fragmentClass = HomeFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                } catch (Exception e) {
                    Log.e("MAD", "editProfileClick: ", e);
                }

                ((FragmentActivity) v.getContext()).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.flContent, fragment, getString(R.string.title_Home))
                        .addToBackStack("Home")
                        .commit();
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                final DatabaseReference dbRef = db.getReference("orders");
                for(CartClass d: dishList){
                    descr = descr+ " - " +d.getQuantity() + " x " + d.getTitle() ;

                }
                order.put("title","order");
                order.put("idRest","toDo");
                order.put("description",descr);
                order.put("address",customerAddress.getText().toString());
                order.put("Idcustomer",FirebaseAuth.getInstance().getCurrentUser().getUid());
                order.put("date","toDo");
                order.put("hour","toDo");
                order.put("priceProd",String.valueOf(prodPrice));
                order.put("priceShip",String.valueOf(shipPrice));
                order.put("price",String.valueOf((prodPrice)+shipPrice));
                order.put("state","Incoming");
                dbRef.push().setValue(order);
                databaseRef.child("customers/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/cart").removeValue();
                Toast.makeText(getContext(), "ToDo: Completed!", Toast.LENGTH_SHORT).show();
            }
        });*/
        //populate the list of cart objects
        loadFromFirebase();
        return rootView;
    }
    @Override
    public void onResume() {
        customerAddress = getView().findViewById(R.id.costumer_address);
        totalPrice = getView().findViewById(R.id.total_price);

        customerAddress.setText(pref.getString("addressCustomer", getResources().getString(R.string.es_street)));
        totalPrice.setText(pref.getString("totPrice","20.5"));

        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        pref = this.getActivity().getSharedPreferences("DEGUSTIBUS", Context.MODE_PRIVATE);
    }

    public void loadFromFirebase(){

        // obtain the url /offers/{restaurantIdentifier}
        databaseRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("customers")
                .child(user.getUid())
                .child("cart");

        Query query = databaseRef; // query data at /customers/{customerIdentifier}/cart

        FirebaseRecyclerOptions<Dish> options =
                new FirebaseRecyclerOptions.Builder<Dish>()
                        .setQuery(query, new SnapshotParser<Dish>() {
                            @NonNull
                            @Override
                            public Dish parseSnapshot(@NonNull DataSnapshot snapshot) {
                                dish = snapshot.getValue(Dish.class);  // get the snapshot and cast it
                                // into a `Dish` item
                                dishList.add(dish);                         // add the `dish` into the list
                                return dish;                               // return the item to the builder
                            }
                        }).build(); // build the option


        adapter = new FirebaseRecyclerAdapter<Dish, CartHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final CartHolder holder, final int position, @NonNull final Dish model) {

                final Integer maxAvail = Integer.parseInt(model.getAvail());

                // the user can only decrease the selected quantity
                final int maxQuantity = model.getQuantity();

                holder.title.setText(model.getDish());
                holder.price.setText(model.getPrice() + " €");
                holder.quantity.setText(String.valueOf(model.getQuantity()));
                totalAmount += Float.parseFloat(model.getPrice())*model.quantity;
                totalAmountTextView.setText(String.valueOf(totalAmount));

                // set button plus
                holder.buttonPlus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int n =Integer.parseInt(holder.quantity.getText().toString());
                        // check for the availability of the product
                        if(n < maxQuantity){
                            n ++;
                            holder.quantity.setText(String.valueOf(n));
                            dishList.get(position).setQuantity(n);
                            totalAmount += Float.parseFloat(model.price);
                            totalAmountTextView.setText(String.valueOf(totalAmount));
                        }

                    }
                });

                // set button minus
                holder.buttonMinus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int n =Integer.parseInt(holder.quantity.getText().toString());
                        // check for non negative numbers
                        if(n > 0){
                            n --;
                            holder.quantity.setText(String.valueOf(n));
                            dishList.get(position).setQuantity(n);
                            totalAmount -= Float.parseFloat(model.price);
                            totalAmountTextView.setText(String.valueOf(totalAmount));
                        }
                    }
                });
            }

            @Override
            public CartHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cart_listitem, parent, false);
                return new CartHolder(view);

            }

        };
        recyclerView.setAdapter(adapter);

        /*dbR.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //this method is called once with the initial value and again whenever data at this location is updated
                for(DataSnapshot dS : dataSnapshot.getChildren()){
                    dish = new CartClass(dS.getValue(CartClass.class).getTitle(),dS.getValue(CartClass.class).getPrice(),dS.getValue(CartClass.class).getQuantity());
                    dish.setId(dS.getKey());
                    prodPrice = prodPrice + (Double.parseDouble(dish.getPrice())*Integer.parseInt(dish.getQuantity()));
                    dishList.add(dish);
                    productsPrice.setText(String.valueOf(prodPrice));
                    totalPrice.setText(String.valueOf(prodPrice+shipPrice));
                }



                mAdapter = new CartDataAdapter(dishList);
                recyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/

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


}
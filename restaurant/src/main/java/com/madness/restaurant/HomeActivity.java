package com.madness.restaurant;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.madness.restaurant.auth.LoginActivity;
import com.madness.restaurant.daily.DailyFragment;
import com.madness.restaurant.daily.NewDailyOffer;
import com.madness.restaurant.home.HomeFragment;
import com.madness.restaurant.insights.InsightsFragment;
import com.madness.restaurant.profile.EditProfile;
import com.madness.restaurant.profile.ProfileFragment;
import com.madness.restaurant.reservations.ReservationFragment;
import com.madness.restaurant.reviews.ReviewsFragment;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener,
        ProfileFragment.ProfileListener, DailyFragment.DailyListener {

    private final String CHANNEL_ID = "channelRestaurant";
    private final int NOTIFICATION_ID = 001;
    /* Widgets */
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private TextView userName;
    private TextView userEmail;
    private CircleImageView userPhoto;

    /* Firebase */
    private Fragment fragment;
    private FragmentManager fragmentManager;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private DatabaseReference databaseReference;
    private DatabaseReference listenerReference;
    private ValueEventListener listener;
    private FirebaseUser user;

    /* Check if connection is enabled! */
    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /* Lifecycle */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        toolbar = findViewById(R.id.toolbarhome);
        setSupportActionBar(toolbar);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();
        firebaseAuth = FirebaseAuth.getInstance();
        fragmentManager = getSupportFragmentManager();

        /* Check if there is an user authenticated, in case no user launch the login screen */
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (user == null) {
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                } else {
                    userName = navigationView.getHeaderView(0).findViewById(R.id.userName);
                    userEmail = navigationView.getHeaderView(0).findViewById(R.id.userEmail);
                    userPhoto = navigationView.getHeaderView(0).findViewById(R.id.userImage);
                    manageNewUser();
                }
            }
        };

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateMenu();
            }
        });

        createNotificationChannel();

        // listen for notifications
        if (user != null) FirebaseDatabase.getInstance().getReference()
                .child("notifications")
                .child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            makeNotification(getString(R.string.new_notification), getString(R.string.notification_message));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void manageNewUser() {
        fragment = null;

        listenerReference = databaseReference.child("restaurants").child(user.getUid());
        listener = listenerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("name")) {
                    // User created and populated, so retrieve data
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    userName.setText(dataSnapshot.child("name").getValue(String.class));
                    userEmail.setText(dataSnapshot.child("email").getValue(String.class));
                    String photo = null;
                    if (dataSnapshot.hasChild("photo")) {
                        photo = dataSnapshot.child("photo").getValue(String.class);
                    }

                    /* Glide */
                    GlideApp.with(getApplicationContext())
                            .load(photo)
                            .placeholder(R.drawable.user_profile)
                            .into(userPhoto);

                    if (fragmentManager.findFragmentById(R.id.flContent) == null) {
                        System.out.println("Loaded data");
                        try {
                            fragment = null;
                            Class fragmentClass;
                            fragmentClass = HomeFragment.class;
                            fragment = (Fragment) fragmentClass.newInstance();
                            fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "HOME").commit();
                        } catch (Exception e) {
                            Log.e("MAD", "onCreate: ", e);
                        }
                    }
                } else {
                    // User created but not populated => go to edit profile
                    if (fragmentManager.findFragmentById(R.id.flContent) == null) {
                        System.out.println("Load edit profile");
                        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        try {
                            fragment = null;
                            Class fragmentClass;
                            fragmentClass = EditProfile.class;
                            fragment = (Fragment) fragmentClass.newInstance();

                            Bundle args = new Bundle();
                            args.putBoolean("isNew", true);
                            fragment.setArguments(args);

                            fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "EditProfile").commit();
                            navigationView.getMenu().getItem(1).setChecked(true);
                        } catch (Exception e) {
                            Log.e("MAD", "onCreate: ", e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
    // end Lifecycle

    @Override
    public void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            listenerReference.removeEventListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // end Notifications

    /* Notifications */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = CHANNEL_ID;
            String description = "desc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void makeNotification(String type, String description) {

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);  // it avoid to recreate the activiy
        // it simply call the `onNewIntent()` method
        intent.putExtra("notification", "open");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // show a new notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_toolbar_notifications);
        builder.setContentTitle(type);
        builder.setContentText(description);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());

    }

    /* Helpers */
    @Override
    public void onItemClicked() {
        try {
            fragment = null;
            Class fragmentClass;
            fragmentClass = EditProfile.class;
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e("MAD", "onItemClicked: ", e);
        }

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.flContent, fragment, "EditProfile");
        ft.addToBackStack("PROFILE");
        ft.commit();
    }

    @Override
    public void reviewsClick() {
        try {
            fragment = null;
            Class fragmentClass;
            fragmentClass = ReviewsFragment.class;
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e("MAD", "reviewsClick: ", e);
        }

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.flContent, fragment, "RestaurantReviews");
        ft.addToBackStack("Profile");
        ft.commit();
    }

    private void updateMenu() {
        fragment = fragmentManager.findFragmentById(R.id.flContent);
        if (fragment != null) {
            if (fragment instanceof ProfileFragment) {
                navigationView.getMenu().findItem(R.id.nav_profile).setChecked(true);
            } else if (fragment instanceof EditProfile) {
                navigationView.getMenu().findItem(R.id.nav_profile).setChecked(true);
            } else if (fragment instanceof DailyFragment) {
                navigationView.getMenu().findItem(R.id.nav_daily).setChecked(true);
            } else if (fragment instanceof NewDailyOffer) {
                navigationView.getMenu().findItem(R.id.nav_daily).setChecked(true);
            } else if (fragment instanceof ReservationFragment) {
                navigationView.getMenu().findItem(R.id.nav_reservations).setChecked(true);
            } else if (fragment instanceof SettingsFragment) {
                navigationView.getMenu().findItem(R.id.nav_settings).setChecked(true);
            } else {
                navigationView.getMenu().findItem(R.id.nav_home).setChecked(true);
            }
        }

        if (!isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), getString(R.string.err_connection), Toast.LENGTH_LONG).show();
        }

        if (user != null) {
            FirebaseDatabase.getInstance().getReference().child("restaurants").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.errProfile), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    public void addDailyOffer(String identifier) {
        try {
            fragment = null;
            Class fragmentClass;
            fragmentClass = NewDailyOffer.class;
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e("MAD", "onItemClicked: ", e);
        }

        Bundle args = new Bundle();
        args.putString("id", identifier);
        fragment.setArguments(args);

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.flContent, fragment, "AddOffer");
        ft.addToBackStack("Daily");
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        fragment = null;
        Class fragmentClass;

        switch (item.getItemId()) {
            case R.id.nav_profile:
                try {
                    fragmentClass = ProfileFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "PROFILE").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_reservations:
                try {
                    fragmentClass = ReservationFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "RESERVATION").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_daily:
                try {
                    fragmentClass = DailyFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "DAILY").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_settings:
                try {
                    fragmentClass = SettingsFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "SETTINGS").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_insights:
                try {
                    fragmentClass = InsightsFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "INSIGHTS").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                try {
                    fragmentClass = HomeFragment.class;
                    fragment = (Fragment) fragmentClass.newInstance();
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, fragment, "HOME").addToBackStack("HOME").commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        item.setChecked(true);

        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        EditProfile editFrag = (EditProfile)
                getSupportFragmentManager().findFragmentByTag("EditP");
        if (editFrag != null) {
            editFrag.setHourAndMinute(hourOfDay, minute);
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // empty method
    }
    // end Helpers
}

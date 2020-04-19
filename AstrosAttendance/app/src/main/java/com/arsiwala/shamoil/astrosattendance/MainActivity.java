package com.arsiwala.shamoil.astrosattendance;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    NavController navController;
    String TAG = "Astros_MainActivity";
    private static final int RC_SIGN_IN = 123;
    SharedPreferences sharedpreferences;
    boolean loggedIn = false;
    FirebaseUser user;
    Menu menu;
    DrawerLayout drawer;
    ArrayList admins = null;
    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        menu = navigationView.getMenu();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        } else {
            user = currentUser;
            loggedIn = true;
            menu.findItem(R.id.nav_logout).setTitle("Log Out");
            download_firebase_data();
        }

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_scanqr, R.id.nav_makeqr)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return setNavigationItem(menuItem);
            }
        });

        sharedpreferences = getSharedPreferences("", Context.MODE_PRIVATE);

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean setNavigationItem(MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_home:
                navController.popBackStack();
                navController.navigate(R.id.nav_home);
                closeNavigationDrawer();
                Log.d(TAG, "setNavigationItem: home");
                break;
            case R.id.nav_scanqr:
                item.setCheckable(false);
                if(isCameraPermissionGranted()) {
//                    navController.popBackStack();
                    navController.navigate(R.id.nav_scanqr);
                    Log.d(TAG, "setNavigationItem: scanner");
                }
                closeNavigationDrawer();

                break;

            case R.id.nav_makeqr:
                closeNavigationDrawer();
                item.setCheckable(false);

                if(user != null && admins != null) {
                    if ((admins).contains(user.getEmail())) {
//                    navController.popBackStack();
                        navController.navigate(R.id.nav_makeqr);
                        Log.d(TAG, "setNavigationItem: maker");
                    } else {
                        Toast.makeText(getApplicationContext(), "You're not a senior", Toast.LENGTH_LONG).show();
                    }
                }
                else if(user == null) {
                    Toast.makeText(getApplicationContext(), "Not logged in", Toast.LENGTH_LONG).show();
                }
                else { // if(admins == null) {
                    Toast.makeText(getApplicationContext(), "Fetching data... please wait", Toast.LENGTH_LONG).show();
                }

                break;
                
            case R.id.nav_logout: /* WHAT IF SOMEONE GIVES THE PASSWORD TO SOMEONE ELSE TO SCAN THE
                                     CODE. MOSTLY WONT HAPPEN COZ ONE DEVICE ONLY ONCE CAN BE
                                     SCANNED (CHECKED BY SENIORS) */
            if(user != null) {
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(getApplicationContext(), "Logged Out Successfully", Toast.LENGTH_LONG).show();
                                Log.d(TAG, " : Logged Out Successfully");
                                user = null;
                                menu.findItem(R.id.nav_logout).setTitle("Log In");
                            }
                        });
            }
            else {
                List<AuthUI.IdpConfig> providers = Collections.singletonList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
                break;
        }
    return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                user = FirebaseAuth.getInstance().getCurrentUser();
                assert user != null;
                Log.d(TAG, "onActivityResult: " + user.getEmail());
                sharedpreferences.edit().putString("email", user.getEmail()).apply();
                menu.findItem(R.id.nav_logout).setTitle("Log Out");
                download_firebase_data();
            } else {
                Log.d(TAG, "onActivityResult: failed" + response.toString());
            }
        }
    }


    public boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "CAMERA permission is granted");
                return true;
            } else {
                Log.e(TAG, "CAMERA permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        } else {
            Log.i(TAG, "CAMERA permission is granted by default");
            return true;
        }
    }


    public void closeNavigationDrawer() {
        drawer.closeDrawer(GravityCompat.START);
    }

    public void setActionBarTitle(String title) {
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getFragmentManager().getBackStackEntryCount() > 0)
            getFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    public void download_firebase_data(){
        progress=new ProgressDialog(this);
        progress.setMessage("Downloading encryption details");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        FirebaseFirestore.getInstance().collection("2019-2020").document("admin")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.d(TAG, "Successfully received details from firestore");
                        admins = (ArrayList) documentSnapshot.get("seniors");

                        assert admins != null;
                        if(!admins.contains(user.getEmail())){
                            menu.findItem(R.id.nav_makeqr).setEnabled(false);
                            Log.d(TAG, "Removed option from menu");
//                            menu.add(R.id.group_attendance, R.id.nav_makeqr, 1, "Make QR");
                        }

                        progress.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {progress.dismiss();}
                });
    }
}

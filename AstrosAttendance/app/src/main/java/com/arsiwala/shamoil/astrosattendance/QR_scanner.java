package com.arsiwala.shamoil.astrosattendance;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;
import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class QR_scanner extends Fragment {
    private CodeScanner mCodeScanner;
    private SharedPreferences sharedpreferences;
    private String messageAfterEncrypt, TAG = "Astros_Scanner", encryption_password = null;
    private FirebaseFirestore db;
    private ProgressDialog progress;
    boolean gotFireStoreData = false;


    public QR_scanner() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity activity = getActivity();
        View root = inflater.inflate(R.layout.fragment_qr_scanner, container, false);

        Objects.requireNonNull(((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar()).show();
        ((MainActivity) getActivity()).setActionBarTitle("QR Code Scanner");

        CodeScannerView scannerView = root.findViewById(R.id.scanner_view);
        assert activity != null;
        mCodeScanner = new CodeScanner(activity, scannerView);
        sharedpreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("", Context.MODE_PRIVATE);

        progress=new ProgressDialog(getContext());
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
                        encryption_password = (String) documentSnapshot.get("AES Encryption");
                        gotFireStoreData = true;
                        mCodeScanner.startPreview();
                        Log.d(TAG, "Successfully received details from firestore ");
                        progress.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed to get encryption details", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Error while receiving details from firestore \n" + e.toString());
                        progress.dismiss();
                    }
                });

        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        String date = sdf.format(new Date());

                        if(!date.equals(sharedpreferences.getString("last_marked_attendance", null))){
                            try {
                                messageAfterEncrypt = AESCrypt.encrypt(encryption_password, date);
                            } catch (GeneralSecurityException e) {}

                            Log.d(TAG, "Message - " + messageAfterEncrypt);

                            if (result.getText().equals(messageAfterEncrypt)) {
                                db = FirebaseFirestore.getInstance();
                                String email = sharedpreferences.getString("email", null);
                                if (email != null) {
                                    ConnectivityManager cm = (ConnectivityManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;

                                    if(activeNetwork != null) {
                                        db.collection("2019-2020").document("Attendance")
                                                .update(email.substring(0, email.indexOf('@')), FieldValue.arrayUnion(Timestamp.now()));

                                        sharedpreferences.edit().putString("last_marked_attendance", date).apply();
                                        Toast.makeText(getContext(), "Attendance marked", Toast.LENGTH_LONG).show();
                                    }
                                    else {
                                        Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_LONG).show();
                                    }
                                }
                                else {
                                    Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_LONG).show();
                                }
                            }
                            else {
                                Toast.makeText(getContext(), "Attendance not marked", Toast.LENGTH_LONG).show();
                            }
                        }
                        else {
                            Toast.makeText(getContext(), "Attendance can only be marked once per day", Toast.LENGTH_LONG).show();
                        }


                        NavHostFragment.findNavController(QR_scanner.this).popBackStack();
                        NavHostFragment.findNavController(QR_scanner.this).navigate(R.id.nav_home);
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    public void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
}

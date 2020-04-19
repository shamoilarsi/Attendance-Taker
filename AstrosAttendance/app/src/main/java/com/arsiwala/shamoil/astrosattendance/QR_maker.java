package com.arsiwala.shamoil.astrosattendance;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.WriterException;
import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import static android.content.Context.WINDOW_SERVICE;

public class QR_maker extends Fragment {
    private String TAG = "Astros_Maker", encryptedMsg, encryption_password = null, QR_password = null;
    private Bitmap bitmap;
    private ProgressDialog progress;



    public QR_maker() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_qr_maker, container, false);

        Objects.requireNonNull(((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar()).show();
        ((MainActivity) getActivity()).setActionBarTitle("QR Code Builder");

        final EditText editText = rootView.findViewById(R.id.edittext);
        final ImageView qrImage = rootView.findViewById(R.id.imageView2);
        editText.setFocusable(false);

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
                        QR_password = (String) documentSnapshot.get("QR Password");
                        editText.setFocusableInTouchMode(true);
                        progress.dismiss();
                        Log.d(TAG, "Successfully received details from firestore");
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

        qrImage.setVisibility(View.INVISIBLE);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String entered_value = String.valueOf(s);
                if(entered_value.equals(QR_password)){
                    editText.setText("");

                    WindowManager manager = (WindowManager) Objects.requireNonNull(getActivity()).getSystemService(WINDOW_SERVICE);
                    Display display = manager.getDefaultDisplay();
                    Point point = new Point();
                    display.getSize(point);
                    int width = point.x;
                    int height = point.y;
                    int smallerDimension = width < height ? width : height;
                    smallerDimension = smallerDimension * 3 / 4;

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    String date = sdf.format(new Date());

                    try {
                        if(encryption_password != null)
                            encryptedMsg = AESCrypt.encrypt(encryption_password, date);
                    }catch (GeneralSecurityException e) {}

                    Log.d(TAG, "Message - " + encryptedMsg);

                    if(encryptedMsg != null) {
                        QRGEncoder qrgEncoder = new QRGEncoder(encryptedMsg, null, QRGContents.Type.TEXT, smallerDimension);
                        try {
                            bitmap = qrgEncoder.encodeAsBitmap();
                            qrImage.setVisibility(View.VISIBLE);
                            qrImage.setImageBitmap(bitmap);
                        } catch (WriterException e) {
                            Log.v(TAG, e.toString());
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return rootView;
    }
}

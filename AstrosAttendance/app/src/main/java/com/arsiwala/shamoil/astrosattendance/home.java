package com.arsiwala.shamoil.astrosattendance;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class home extends Fragment {
    private SharedPreferences sharedpreferences;
    String TAG = "Astros_home";

    public home() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        Objects.requireNonNull(((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar()).show();
        ((MainActivity) getActivity()).setActionBarTitle("Home");

        sharedpreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("", Context.MODE_PRIVATE);

        TextView textview_status = rootView.findViewById(R.id.home_status);
        ImageView imageView = rootView.findViewById(R.id.home_imageview);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String date = sdf.format(new Date());

        if(Objects.equals(sharedpreferences.getString("last_marked_attendance", ""), date)){
            textview_status.setText("Attendance is marked for today");
            textview_status.setTextColor(Color.GREEN);
            imageView.setImageResource(R.drawable.done);
        }
        else {
            textview_status.setText("Attendance not marked for today");
            textview_status.setTextColor(Color.RED );
            imageView.setImageResource(R.drawable.cancel);
        }

        return rootView;
    }
}

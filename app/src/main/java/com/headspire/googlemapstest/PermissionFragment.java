package com.headspire.googlemapstest;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class PermissionFragment extends Fragment {


    private static final int REQUEST_CODE =2 ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_permission, container, false);
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
                ,Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
        return view;
    }
}

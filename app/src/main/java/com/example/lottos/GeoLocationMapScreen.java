package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentGeoLocationMapScreenBinding;
import com.example.lottos.organizer.OrganizerEventManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;

/**
 * Displays a map showing markers for all entrants who submitted geolocation.
 */
public class GeoLocationMapScreen extends Fragment implements OnMapReadyCallback {

    private FragmentGeoLocationMapScreenBinding binding;
    private GoogleMap mMap;

    private String eventId;
    private String userName;  // Needed for navigation context
    private OrganizerEventManager manager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGeoLocationMapScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get eventId + username
        if (getArguments() != null) {
            GeoLocationMapScreenArgs args = GeoLocationMapScreenArgs.fromBundle(getArguments());
            eventId = args.getEventId();
            userName = args.getUserName();   // Make sure NavGraph passes this
        }

        manager = new OrganizerEventManager();

        // Initialize Google Map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_container);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            toast("Error loading map.");
        }

        setupBackButton();
        setupNavButtons();
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void setupNavButtons() {

        binding.btnHome.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(GeoLocationMapScreenDirections
                                .actionGeoLocationMapScreenToHomeScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(GeoLocationMapScreenDirections
                                .actionGeoLocationMapScreenToOrganizerEventsScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(GeoLocationMapScreenDirections
                                .actionGeoLocationMapScreenToNotificationScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(GeoLocationMapScreenDirections
                                .actionGeoLocationMapScreenToEventHistoryScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(GeoLocationMapScreenDirections
                                .actionGeoLocationMapScreenToProfileScreen(userName)));
    }

    // ------------------------------ MAP LOGIC -------------------------------- //

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (eventId == null) {
            toast("Missing event ID.");
            return;
        }

        loadEntrantLocations();
    }

    private void loadEntrantLocations() {
        manager.getEntrantLocations(eventId, new OrganizerEventManager.LocationsCallback() {
            @Override
            public void onSuccess(Map<String, Map<String, Double>> locations) {
                if (locations.isEmpty()) {
                    toast("No location data recorded for entrants.");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
                    return;
                }
                plotMarkers(locations);
            }

            @Override
            public void onError(Exception e) {
                toast("Failed to load geolocation data: " + e.getMessage());
            }
        });
    }

    private void plotMarkers(Map<String, Map<String, Double>> locations) {
        LatLng firstLocation = null;

        for (Map.Entry<String, Map<String, Double>> entry : locations.entrySet()) {
            String user = entry.getKey();
            Map<String, Double> coords = entry.getValue();

            Double lat = coords.get("latitude");
            Double lon = coords.get("longitude");

            if (lat == null || lon == null) continue;
            if (lat == 0.0 && lon == 0.0) continue;

            LatLng userLatLng = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions().position(userLatLng).title(user));

            if (firstLocation == null) {
                firstLocation = userLatLng;
            }
        }

        if (firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10));
        } else {
            toast("No valid geolocation found.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
        }
    }

    private void toast(String msg) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

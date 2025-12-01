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
 * A Fragment that displays a Google Map with markers representing the geographic
 * locations of all users who have checked into a specific event.
 *
 * Role: This screen serves as a visual tool for organizers to see where their
 * event attendees have checked in from. Its primary responsibilities are:
 * <ul>
 *     <li>Initializing a Google Map view.</li>
 *     <li>Retrieving an event ID from navigation arguments.</li>
 *     <li>Using the {@link OrganizerEventManager} to fetch the geolocation data
 *         for all entrants of that event.</li>
 *     <li>Plotting a marker on the map for each entrant at their recorded coordinates.</li>
 *     <li>Centering the map on the first available location marker.</li>
 *     <li>Providing simple UI feedback if no location data is available.</li>
 * </ul>
 * It implements the {@link OnMapReadyCallback} interface to handle the asynchronous
 * loading of the Google Map.
 */
public class GeoLocationMapScreen extends Fragment implements OnMapReadyCallback {

    private FragmentGeoLocationMapScreenBinding binding;
    private GoogleMap mMap;

    private String eventId;
    private String userName;  // Needed for navigation context
    private OrganizerEventManager manager;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the fragment's layout and initializes the view binding.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGeoLocationMapScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This is where the fragment's logic is initialized, including retrieving arguments and setting up the map.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Callback method that is triggered when the Google Map is ready to be used.
     * This is the entry point for all interactions with the map object.
     *
     * @param googleMap The fully initialized GoogleMap object.
     */
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

    /**
     * Iterates through a map of user locations and adds a marker to the map for each one.
     * After plotting, it moves the camera to focus on the first valid location found.
     *
     * @param locations A map where the key is a username and the value is a map of their coordinates ("latitude", "longitude").
     */
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
            // Filter out default/dummy coordinates (e.g., 0.0, 0.0) if they are not meaningful.
            if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                LatLng userLatLng = new LatLng(lat, lon);
                mMap.addMarker(new MarkerOptions().position(userLatLng).title(userName));

                // Save the first valid location to focus the camera on it.
                if (firstLocation == null) {
                    firstLocation = userLatLng;
                }
            }
        }

        // Move camera to the first recorded location if available.
        if (firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10));
        } else {
            // If no valid locations were found, center the map on a generic location and inform the user.
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.8283, -98.5795), 2)); // Center of the US
            toast("No valid geolocation was captured for entrants.");
        }
    }

    /**
     * A utility method to display a long toast message, checking for a valid context.
     *
     * @param message The message to display.
     */
    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * The view binding object is cleared here to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

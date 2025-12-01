package com.example.lottos;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.lottos.databinding.ActivityMainBinding;

/**
 * The main and only Activity for the application, serving as the host for all Fragments
 * and the primary controller for the app's navigation structure.
 *
 * Role: This class acts as the application's entry point after launch. It is responsible for
 * setting up the main window, including the toolbar (Action Bar), and initializing the
 * Android Jetpack Navigation Component. It uses a {@link NavController} to manage all
 * fragment transitions within the {@code nav_host_fragment_content_main}. This single-activity
 * architecture simplifies the app's lifecycle and provides a consistent navigation framework.
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    /**
     * Called when the activity is first created. This is where the majority of initialization
     * should go: calling setContentView(int) to inflate the activity's UI, using
     * view binding to get references to widgets, and setting up the NavController
     * with the ActionBar for a unified navigation experience.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using ViewBinding and set it as the content view.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the custom toolbar as the support action bar for this activity.
        setSupportActionBar(binding.toolbar);

        // Find the NavController associated with the NavHostFragment.
        NavController navController = Navigation.findNavController(this,R.id.nav_host_fragment_content_main);

        // Build the AppBarConfiguration, defining top-level destinations from the navigation graph.
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

        // Connect the NavController to the ActionBar, which enables the Up button and updates the title.
        NavigationUI.setupActionBarWithNavController(this,navController, appBarConfiguration);

    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     * This is only called once, the first time the options menu is displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    /**
     * Handles the "Up" button navigation. When the user presses the Up arrow in the
     * action bar, this method delegates the navigation action to the NavController.
     * If the NavController can't navigate up (i.e., the user is at the start
     * destination), it falls back to the default superclass behavior.
     *
     * @return true if navigation was handled by the NavController, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        // Let the NavigationUI handle the Up button press. It will pop the back stack.
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}

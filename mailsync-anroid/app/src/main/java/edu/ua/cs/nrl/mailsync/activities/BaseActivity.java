package edu.ua.cs.nrl.mailsync.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.LogFileConfiguration;
import com.couchbase.lite.LogLevel;

import java.io.File;

import edu.ua.cs.nrl.mailsync.EmailViewModel;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.fragments.LoginFragment;
import edu.ua.cs.nrl.mailsync.fragments.TutorialFragment;


public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    EmailViewModel emailViewModel;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final File path = getApplicationContext().getCacheDir();
        Database.log.getFile().setConfig(new LogFileConfiguration(path.toString()));
        Database.log.getFile().setLevel(LogLevel.INFO);
        setContentView(R.layout.activity_fragment_base);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        emailViewModel= ViewModelProviders.of(this).get(EmailViewModel.class);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

// Replace the contents of the container with the new fragment
        ft.replace(R.id.activity_fragment_base_fragmentContainer, new LoginFragment());
        ft.commit();
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
        mActivityTitle = getTitle().toString();
        setupDrawer();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        System.out.println("Item id is" + id);
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        // Activate the navigation drawer toggle
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupDrawer() {

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Navigating");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        if(id==R.id.home){
           fragment = new LoginFragment();
        } else if(id == R.id.logout){

        } else if(id == R.id.tutorial){
            fragment = new TutorialFragment();
        }
        if( fragment==null){
            fragment = new LoginFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
// Replace the contents of the container with the new fragment
        ft.replace(R.id.activity_fragment_base_fragmentContainer, fragment);
        ft.commit();
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}

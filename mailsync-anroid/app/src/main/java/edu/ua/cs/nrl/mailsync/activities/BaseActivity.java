package edu.ua.cs.nrl.mailsync.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.LogFileConfiguration;
import com.couchbase.lite.LogLevel;

import java.io.File;

import edu.ua.cs.nrl.mailsync.EmailViewModel;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.fragments.LoginFragment;


public class BaseActivity extends AppCompatActivity {
    EmailViewModel emailViewModel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final File path = getApplicationContext().getCacheDir();
        Database.log.getFile().setConfig(new LogFileConfiguration(path.toString()));
        Database.log.getFile().setLevel(LogLevel.INFO);
        setContentView(R.layout.activity_fragment_base);
        emailViewModel= ViewModelProviders.of(this).get(EmailViewModel.class);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

// Replace the contents of the container with the new fragment
        ft.replace(R.id.activity_fragment_base_fragmentContainer, new LoginFragment());
        ft.commit();
    }
}

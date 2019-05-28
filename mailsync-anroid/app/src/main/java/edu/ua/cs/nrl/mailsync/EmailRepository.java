package edu.ua.cs.nrl.mailsync;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.StrictMode;

import com.icegreen.greenmail.ExternalProxy;

import java.util.concurrent.ScheduledExecutorService;

import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;

public class EmailRepository {
    private int mailboxSize;
    boolean hasInternetBefore = false;
    private Unbinder unbinder;
    private String userEmail;
    private String userPassword;
    private ScheduledExecutorService scheduleTaskExecutor;
    private boolean ndnService = false;
    private NdnDBConnection ndnDBConnection;
    private Handler handler = new Handler();
    private boolean lastInternetState = true;
    private Relayer relayer;
    private int lastMailboxSize;
    private Context context;
    public EmailRepository(Context context){
        this.context=context;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void initializeExternalProxy() {
        ExternalProxy.context = context;

        if (isNetworkAvailable()) {
            lastInternetState = true;
            emailViewModel.setNetworkStatus(true);
            ExternalProxy.setSelectedProxy(2);
        } else {
            lastInternetState = false;
            emailViewModel.setNetworkStatus(false);
            ExternalProxy.setSelectedProxy(2);
        }
    }

    private void initializeThreadPolicy() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private void initializeDB() {
        ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
                "couchbaseLite",
                getContext().getApplicationContext()
        );
    }
}


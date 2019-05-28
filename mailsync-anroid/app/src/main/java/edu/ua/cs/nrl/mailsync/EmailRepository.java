package edu.ua.cs.nrl.mailsync;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.StrictMode;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;
import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Name;
import net.named_data.jndn_xx.util.FaceUri;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;
import edu.ua.cs.nrl.mailsync.utils.NfdcHelper;

public class EmailRepository {
    private int mailboxSize;
    boolean hasInternetBefore = false;
    private Unbinder unbinder;
    private String userEmail;
    private String userPassword;
    private boolean isFirstTime = true;
    private ScheduledExecutorService scheduleTaskExecutor;
    private boolean ndnService = false;
    private NdnDBConnection ndnDBConnection;
    private Handler handler = new Handler();
    private boolean lastInternetState = true;
    private Relayer relayer;
    private int lastMailboxSize;
    private Context context;
    private MutableLiveData<Boolean>networkStatus;
    public EmailRepository(Context context, String userEmail, String userPassword){
        this.context=context;
        this.userEmail=userEmail;
        this.userPassword=userPassword;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public void init(){
        initializeDB();
        initializeThreadPolicy();
        initializeExternalProxy();
    }
    private void initializeExternalProxy() {
        ExternalProxy.context = context;

        if (isNetworkAvailable()) {
            lastInternetState = true;
//            emailViewModel.setNetworkStatus(true);
            getNetworkStatus().setValue(true);
            ExternalProxy.setSelectedProxy(2);
        } else {
            lastInternetState = false;
//            emailViewModel.setNetworkStatus(false);
            getNetworkStatus().setValue(false);
            ExternalProxy.setSelectedProxy(2);
        }
    }

    public MutableLiveData<Boolean> getNetworkStatus(){

        if(networkStatus==null){
            networkStatus=new MutableLiveData<>();
        }
        return networkStatus;
    }

    private void initializeThreadPolicy() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private void initializeDB() {
        ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
                "couchbaseLite",
                context.getApplicationContext()
        );
    }
    public void registerPrefix(){
        if (!isNetworkAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NfdcHelper nfdcHelper = new NfdcHelper();
                    try {
                        List<String> ipList = getArpLiveIps(true);
                        String connectedDeviceIp = ipList.get(0);

                        System.out.println("IP address is: " + connectedDeviceIp);
                        String faceUri = "udp4://" + connectedDeviceIp + ":56363";
                        int faceId = nfdcHelper.faceCreate(faceUri);
                        nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
                        nfdcHelper.shutdown();
                    } catch (ManagementException e) {
                        e.printStackTrace();
                    } catch (FaceUri.CanonizeError canonizeError) {
                        canonizeError.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }
    private ArrayList<String> getArpLiveIps(boolean onlyReachables) {
        BufferedReader bufRead = null;
        ArrayList<String> result = null;

        try {
            result = new ArrayList<>();
            bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
            String fileLine;
            while ((fileLine = bufRead.readLine()) != null) {
                String[] splitted = fileLine.split(" +");
                if ((splitted != null) && (splitted.length >= 4)) {
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        boolean isReachable = pingCmd(splitted[0]);/**
                         * Method to Ping  IP Address
                         * @return true if the IP address is reachable
                         */
                        if (!onlyReachables || isReachable) {
                            result.add(splitted[0]);
                        }
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                bufRead.close();
            } catch (IOException e) {
            }
        }
        return result;
    }
    private boolean pingCmd(String addr) {
        try {
            String ping = "ping  -c 1 -W 1 " + addr;
            Runtime run = Runtime.getRuntime();
            Process pro = run.exec(ping);
            try {
                pro.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int exit = pro.exitValue();
            if (exit == 0) {
                return true;
            } else {
                //ip address is not reachable
                return false;
            }
        } catch (IOException e) {
        }
        return false;
    }
    public void startGmail(){
        hasInternetBefore = true;
//      ExternalProxy.gmail.stop();

        new Thread(new Runnable() {
            public void run() {
                ExternalProxy.gmail.start();
            }
        }).start();
        ExternalProxy.setSelectedProxy(2);



        // Start the relayer service
//        startRelayer();

        if (!isFirstTime) {
            ExternalProxy.ndnMailSyncOneThread.face_.shutdown();
        }

        ExternalProxy.ndnMailSyncOneThread =
                new NDNMailSyncOneThread(context.getApplicationContext());
    }

    public void shutdownRelayer(){
        if (hasInternetBefore) {
            try {
                if (relayer.getServerSocket() != null) {
                    relayer.getServerSocket().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startRelayer(){
        relayer = new Relayer(3143);
        relayer.execute(new String[]{""});
    }

    public void ndnMailExecution(){
        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ExternalProxy.ndnMailSyncOneThread.start();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        isFirstTime = false;
    }

    public void startServer(String userEmail,String userPassword) {
        System.out.println("In EmailRepo"+"username:"+userEmail+userPassword);
        registerPrefix();
//        progressStatus = 0;
        ExternalProxy.setUser(userEmail, userPassword);
        ExternalProxy.setSelectedProxy(2);
        if (isNetworkAvailable()) {
            System.out.println("Network available");
            startRelayer();
            startGmail();

        } else {
            shutdownRelayer();
            System.out.println("Network NOT available");
            startGmail();
        }
        ndnMailExecution();
    }

    public void clearDatabase() {
        try {
            new Database("MailFolder", ndnDBConnection.getConfig()).delete();
            new Database("Attribute", ndnDBConnection.getConfig()).delete();
            new Database("MimeMessage", ndnDBConnection.getConfig()).delete();
            new Database("MessageID", ndnDBConnection.getConfig()).delete();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}


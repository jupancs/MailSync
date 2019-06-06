package edu.ua.cs.nrl.mailsync;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;
import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Name;
import net.named_data.jndn_xx.util.FaceUri;

import org.w3c.dom.Text;

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

public  class EmailRepository {
    private int mailboxSize;
    boolean hasInternetBefore = false;
    private Unbinder unbinder;
    private String userEmail;
    private String userPassword;
    private boolean isFirstTime = true;
    private ScheduledExecutorService scheduleTaskExecutor;
    private boolean ndnService = false;
    private NdnDBConnection ndnDBConnection;
    private boolean lastInternetState = true;
    private Relayer relayer;
    private int lastMailboxSize;
    private static Context context;
    private MutableLiveData<Boolean> networkStatus;
    private static View view;
    private static int storedMessages;
    private static final String TAG = "EmailRepo";
    TextView textView;
    private static ArrayList<Long>incompleteUids= new ArrayList<>();
    public static boolean isIncomplete=false;





    public EmailRepository(Context context, String userEmail, String userPassword) {
        this.context = context;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    public EmailRepository(){

    }

    //Adds uids of emails that are not completed
    synchronized public void addIncompleteUids(long uid){
        incompleteUids.add(uid);
        isIncomplete=true;
    }
    //Notifies user if an email is not stored completely
    public void notifyIncompleteEmail(long uid){
        toast(context,"The email of " + uid + " was not stored correctly. Please try again later");
    }

    //Returns a boolean that returns true if there are any incomplete emails
    public static boolean isIsIncomplete() {
        return isIncomplete;
    }

    //Returns an array of incomplete email uids
    public static ArrayList<Long> getIncompleteUids() {
        return incompleteUids;
    }

    //Removes uid from the list of uids
    synchronized public void removeIncompleteUids(long uid){
        incompleteUids.remove(uid);
        if(incompleteUids.isEmpty()){
            isIncomplete=false;
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Initializes the DB, thread policy, External policy and view
    public void init(View view) {
        initializeDB();
        initializeThreadPolicy();
        initializeExternalProxy();
        this.view=view;

        if(view==null){
            Log.d(TAG,"View null");
        }
    }

    //Increments stored messages number and checks if the view is null if not then
    // the text view showing the stored messages is incremented
    synchronized public void incrementStoredMessages(){
        if(view==null){
            Log.d(TAG,"View is null inside increment");
        }
        storedMessages++;
        System.out.println("Email repo stored message " + storedMessages);
        textView=view.findViewById(R.id.stored_emails);
        updateText(Integer.toString(storedMessages));
    }

    //Updates the Textview to the new storedMessages value
    synchronized public void updateText(String text){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {

                textView.setText(text);

            }
        });
    }

    public   int getStoredMessages() {
        return storedMessages;
    }

    public void setStoredMessages(int value){
        storedMessages=value;
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

    public MutableLiveData<Boolean> getNetworkStatus() {

        if (networkStatus == null) {
            networkStatus = new MutableLiveData<>();
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

    public void registerPrefix() {
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

    public void startGmail(View view) {
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
                new NDNMailSyncOneThread(context.getApplicationContext(),view);
    }

    public void shutdownRelayer() {
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

    public void startRelayer() {
        relayer = new Relayer(3143);
        relayer.execute(new String[]{""});
    }

    public void ndnMailExecution() {
        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ExternalProxy.ndnMailSyncOneThread.start();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        isFirstTime = false;
    }

    public void startServer(String userEmail, String userPassword, View view) {
        System.out.println("In EmailRepo" + "username:" + userEmail + userPassword);
        registerPrefix();
//        progressStatus = 0;
        ExternalProxy.setUser(userEmail, userPassword);
        ExternalProxy.setSelectedProxy(2);
        if (isNetworkAvailable()) {
            System.out.println("Network available");
            startRelayer();
            startGmail(view);

        } else {
            shutdownRelayer();
            System.out.println("Network NOT available");
            startGmail(view);
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
    //Gives Toast about syncing set amount of messages...to be removed soon
    public void toast(final Context context, final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();

            }
        });
    }
}


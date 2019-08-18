package edu.ua.cs.nrl.mailsync;

import android.annotation.TargetApi;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndntranslator.ImapToNdnTranslator;
import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Name;
import net.named_data.jndn_xx.util.FaceUri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.mail.Flags;

import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;
import edu.ua.cs.nrl.mailsync.utils.NfdcHelper;

/**
 * EmailRepository connects the EmailViewmodel to the database
 * This is part of the architecture recommended by Android
 * It centralizes all the access to the database and deals with the initialization of threads and NDN Proxies
 * It also contains variables that keep track of the maxEmails that can be stored and emails that are stored
 * so that it can be displayed by the fragment
 * Majority of the functioning of the app is started and controlled in this class
 *
 * @TODO Transfer the updation of textview and progress bar to the ViewModel
 * @see <a href="https://developer.android.com/jetpack/docs/guide">https://developer.android.com/jetpack/docs/guide</a>
 */
public class EmailRepository {
    private static final String TAG = "EmailRepo";
    /**
     * Keeps track of the uid of next mail to follow the current one
     */
    public static long nextUid;
    public static boolean isIncomplete = false;
    /**
     * Keeps track of max amount of emails that can be stored when refreshed
     */
    public static int maxEmailsStored = 0;
    private static Context context;
    private static View view;
    private static int storedMessages = 0;
    private static ArrayList<Long> incompleteUids = new ArrayList<>();
    boolean hasInternetBefore = false;
    TextView textView;
    private String userEmail;
    private String userPassword;
    private boolean isFirstTime = true;
    private ScheduledExecutorService scheduleTaskExecutor;
    private NdnDBConnection ndnDBConnection;
    private boolean lastInternetState = true;
    private Relayer relayer;
    private MutableLiveData<Boolean> networkStatus;
    private static SharedPreferences sharedPreferences;
    private static List<Long> messageUIDList = new ArrayList<>();
    public static HashMap<Long, Flags> flagsMap = new HashMap<>();
    public static boolean isRegistered = false;
    private static HashMap<Long, Boolean> isGettingFetched = new HashMap<>();
    private static HashMap<String,String> hmap = new HashMap<>();
    private static HashSet<Long> fetchedMap = new HashSet<>();

    public EmailRepository(Context context, String userEmail, String userPassword) {
        this.context = context;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    public EmailRepository() {

    }

    /**
     * When an email is getting fetched it gets added to this HashMap
     * so that we avoid fetching the same email more than once
     * @param uid uid of the email getting fetched
     */
    public static void addToisGettingFetched(long uid){
        System.out.println(uid +" is getting fetched");
        isGettingFetched.put(uid,true);
    }

    /**
     * Once fetching of the email is done it should be removed from the
     * Hashmap using this command
     * @param uid uid of the email done getting fetched
     */
    public static void doneGettingFetched(long uid){
        System.out.println(uid +" is done getting fetched");
        isGettingFetched.remove(uid);
        fetchedMap.add(uid);
    }

    /**
     * This can be used to check if the email is getting fetched or not
     * depending on if it is in the Hashmap or not
     * @param uid uid of the email to check
     * @return
     */
    public static boolean checkGettingFetched(long uid){
        System.out.println(uid +" is getting fetched" + isGettingFetched.get(uid));
        if(isGettingFetched.get(uid)==null){
            return false;
        }
        else {
            return isGettingFetched.get(uid);
        }
    }


    /**
     * Returns a boolean that returns true if there are any incomplete emails
     *
     * @return boolean isIncomplete
     */
    public static boolean getIsIncomplete() {
        return isIncomplete;
    }

    /**
     * Returns a reference to the Array list containing UIDs of emails not fetched properly
     *
     * @return Arraylist of IncompleteUids
     */
    public static ArrayList<Long> getIncompleteUids() {
        return incompleteUids;
    }

    /**
     * Adds uids of emails that are not completed to the arraylist and sets the boolean isIncomplete to true
     *
     * @param uid the UID of the email that needs to be fetched
     */
    synchronized public void addIncompleteUids(long uid) {
        if (incompleteUids.indexOf(uid) == -1) {
            incompleteUids.add(uid);
            System.out.println("Uid : " + uid + "Was added");
        }

        isIncomplete = true;
    }

    /**
     * Prints all the uids in the Array containing the incomplete uids
     * needed to be fetched
     */
    public void getAllUids() {
        if (incompleteUids == null) {
            System.out.println("Array List is empty");
        }
        if (incompleteUids != null) {
            System.out.println("Uids in the list are");
            for (int i = 0; i < incompleteUids.size(); i++) {
                System.out.println("uid = " + incompleteUids.get(i));
            }
        }

    }

    /**
     * Displays a toast message notifying the user that the email was not stored properly
     *
     * @param uid a long number identifying the uid of the email that is not completed
     */
    public void notifyIncompleteEmail(long uid) {
        toast(context, "The email of " + uid + " was not stored correctly. Please try again later");
    }

    /**
     * Removes uid from the Array list of incomplete uids and if
     * the Array list is empty then sets isIncomplete to false
     *
     * @param uid UID of the email that was fetched
     */
    synchronized public void removeIncompleteUids(long uid) {
        incompleteUids.remove(uid);
        System.out.println("Removed uid " + uid);
        if (incompleteUids.isEmpty()) {
            isIncomplete = false;
        }
    }

    /**
     * Checks if network is available
     *
     * @return true if network is available or else false
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    /**
     * Initializes the DB, thread policy, External policy and view
     *
     * @param view the fragment
     */
    //
    public void init(View view) {
        initializeDB();
        initializeThreadPolicy();
        initializeExternalProxy();
//        startNetworkCheckThread();
        this.view = view;

        if (view == null) {
            Log.d(TAG, "View null");
        }
    }

    public void deleteStoredMessage(){
        storedMessages = 0;
    }
    public void clearAllUids(){ incompleteUids.clear();}

    public void initSharedPreferences(){
        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences("MessageUIDList",0);
    }

    /**
     * Increments the stored messages number
     */
    synchronized public void incrementStoredMessages() {
        if(storedMessages < maxEmailsStored){
            storedMessages++;
        }

    }

    /**
     * Updates the textview with the new stored messages
     * Is in the format storedMessages /  maxEmailsStored
     */
    synchronized public void notifyStorageCompletion() {
        if (view == null) {
            Log.d(TAG, "View is null inside increment");
        }
        System.out.println("Email repo stored message " + storedMessages + "/" + maxEmailsStored);
        textView = view.findViewById(R.id.stored_emails);
        updateText(Integer.toString(storedMessages) + "/" + maxEmailsStored);
    }

    /**
     * Decrements stored messages
     */
    synchronized public void decrementStoredMessages() {
        if (view == null) {
            Log.d(TAG, "View is null inside increment");
        }
        if(storedMessages > 0){
            storedMessages--;
        }

        textView = view.findViewById(R.id.stored_emails);
        updateText(Integer.toString(storedMessages) + "/" + maxEmailsStored);
    }


    /**
     * Updates the Textview to the new storedMessages value
     *
     * @param text the String to display
     */
    synchronized public void updateText(String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                textView = view.findViewById(R.id.stored_emails);
                textView.setText(text);

            }
        });
    }

    /**
     * Sets storedMessages to 0 and updates textview and progressbar
     */
    synchronized public void deleteAllStoredMessage() {
        if (view == null) {
            Log.d(TAG, "View is null inside increment");
        }
        storedMessages = 0;
        textView = view.findViewById(R.id.stored_emails);
        updateText(Integer.toString(storedMessages));
        updateProgress(0);
    }


    /**
     * returns stored messages
     *
     * @return storedMessages
     */
    public int getStoredMessages() {
        return storedMessages;
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

    /**
     * Saves messageuidlist as a jsontext into sharedpreferences
     */
    public static void saveMessageUIDList(){
//        System.out.println("trying to save messageUIDList" + NdnFolder.messageUidList.isEmpty());
//            System.out.println("Saving MessageUIDList..The array is");
            messageUIDList.clear();
            messageUIDList.addAll(NdnFolder.messageUidList);
//            for(int i = 0;i < messageUIDList.size();i++){
//                System.out.print(messageUIDList.get(i) + " ");
//            }
            Gson gson = new Gson();
            String jsonText = gson.toJson(messageUIDList);
//            System.out.println("Json Text" + jsonText);
            sharedPreferences = context.getSharedPreferences("uidlist",0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("msguid", jsonText);
            editor.apply();

    }

    /**
     * Updates the messageuidlist of ndnfolder by getting the stored messageuidlist from
     * sharedpreferences. The messageuidlist is stored as  a jsontext and is parsed and
     * retrieved
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void updateMessageUIDList(){
        Gson gson = new Gson();
        sharedPreferences = context.getSharedPreferences("uidlist",0);
        String jsonText = sharedPreferences.getString("msguid", null);
//        System.out.println("Json Text" + jsonText);
        if(jsonText!=null){
//            System.out.println("Updating messageUidList...");
            List<String> textList = Arrays.asList(gson.fromJson(jsonText, String[].class));
            messageUIDList = textList.stream().map(Long::parseLong).collect(Collectors.toList());
//            for(int i = 0;i < messageUIDList.size();i++){
//                System.out.print(messageUIDList.get(i) + " ");
//            }
            NdnFolder.messageUidList.addAll(messageUIDList);
        }
//        String[] text = gson.fromJson(jsonText, String[].class);


    }

    public static boolean isFetched(long uid){
        return fetchedMap.contains(uid);
    }
    public static int isGettingFetchedSize(){return isGettingFetched.size();}


    /**
     * Saves flagHashmap to as a Jsonarray and then into shared preferences
     */
    public static void saveFlagMap(){
            flagsMap.clear();
            flagsMap.putAll(NdnFolder.flagsMap);
//            System.out.println("Saved Flag Hash Map");
//            for(HashMap.Entry<Long,Flags> entry : flagsMap.entrySet()){
//                System.out.println("HashMap key" + entry.getKey() + "HashMap value" + entry.getValue());
//            }

            JSONArray jsonArray = new JSONArray();
            for(Long key: flagsMap.keySet()){
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("key",key);
                    jsonObject.put("val",flagsMap.get(key));
                    jsonArray.put(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            sharedPreferences = context.getSharedPreferences("flagmap",0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("map",jsonArray.toString());
//            System.out.println("JSON text saved is" + jsonArray.toString());
            editor.apply();

    }

    /**
     * Updates flaghashmap by retrieving it from SharedPreferences and parsing
     * the string to convert it to a hashmap and then adding it to the ndnfolder
     * hashmap
     */
    public static void updateFlagMap(){
//        System.out.println("Updating Flag Map");
        sharedPreferences = context.getSharedPreferences("flagmap",0);
        String jsonText = sharedPreferences.getString("map",null);
//        System.out.println("Update Flag map with " + jsonText);
        if(jsonText!=null){
            try {
                JSONArray jsonArray = new JSONArray(jsonText);
                for(int i = 0; i < jsonArray.length();i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    flagsMap.put(jsonObject.getLong("key"),new Flags(jsonObject.get("val").toString()));
//                    System.out.print("Value is " + flagsMap.get(jsonObject.getLong("key")));
                }
                NdnFolder.flagsMap.putAll(flagsMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Checks if NDNFolder messagelist and flaghashmap is empty
     * if it is then updates both of them with the stored hashmap and
     * arraylist and updates the lastsize of ndnfolder so that it can
     * start updating from that point
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public static void updateMailboxUids(){
//        System.out.println("ndnMessageList empty " + NdnFolder.messageUidList.isEmpty() + "ndnFlagMap Empty? " + NdnFolder.flagsMap.isEmpty());
        if(NdnFolder.messageUidList.isEmpty() && NdnFolder.flagsMap.isEmpty()&&context!=null){
//            System.out.println("Updating FlagMap and uidlist");
            updateMessageUIDList();
            updateFlagMap();
            NdnFolder.lastSize  = NdnFolder.messageUidList.size();
        }

//        System.out.println("ndnMessageList length " + NdnFolder.messageUidList.size() + "ndnFlagMap size? " + NdnFolder.flagsMap.size());

    }

    public static boolean correctUID(long uid){
        if(uid > 0){
            return true;
        } else return false;
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
                            EmailRepository.isRegistered =true;
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
        System.out.println("Network available");

        // Start the relayer service
//        startRelayer();
        if (isNetworkAvailable()) {
            startRelayer();
        }

        if (!isFirstTime) {
            ExternalProxy.ndnMailSyncOneThread.face_.shutdown();
        }

        ExternalProxy.ndnMailSyncOneThread =
                new NDNMailSyncOneThread(context.getApplicationContext(), view);
    }
//
//    public void stopGmail(){
//        ExternalProxy.stopGmail();
//        shutdownRelayer();
//    }

    public void shutdownRelayer() {
        if (hasInternetBefore) {
            try {
                if (relayer!=null && relayer.getServerSocket() != null) {
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
//
//    public void startNetworkCheckThread() {
//        new Thread(new Runnable() {
//            public void run() {
//                while (true) {
//                    try {
//                        boolean currnetInternetState = isNetworkAvailable();
//                        if (lastInternetState != currnetInternetState) {
//                            System.out.println("Network Config Changed");
////              NfdcHelper nfdcHelper = new NfdcHelper();
////              boolean routeExists = false;
////              try {
////                for (RibEntry ribEntry : nfdcHelper.ribList()) {
////                  if (ribEntry.getName().toString().equals("udp4://224.0.23.170:56363")) {
////                    routeExists = true;
////                    break;
////                  }
////                }
////                if (!routeExists) {
////                  FaceStatus faceStatus =
////                      nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
////                  int faceId = faceStatus.getFaceId();
////                  nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
////                }
////                nfdcHelper.shutdown();
////              } catch (ManagementException e) {
////                e.printStackTrace();
////              } catch (FaceUri.CanonizeError canonizeError) {
////                canonizeError.printStackTrace();
////              } catch (Exception e) {
////                e.printStackTrace();
////              }
//
////              new Thread(new Runnable() {
////                @Override
////                public void run() {
////                  NfdcHelper nfdcHelper = new NfdcHelper();
////                  boolean routeExists = false;
////                  try {
////                    for (RibEntry ribEntry : nfdcHelper.ribList()) {
////                      if (ribEntry.getName().toString().equals("udp4://224.0.23.170:56363")) {
////                        routeExists = true;
////                        break;
////                      }
////                    }
////                    if (!routeExists) {
////                      FaceStatus faceStatus =
////                          nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
////                      int faceId = faceStatus.getFaceId();
////                      nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
////                    }
////                    nfdcHelper.shutdown();
////                  } catch (ManagementException e) {
////                    e.printStackTrace();
////                  } catch (FaceUri.CanonizeError canonizeError) {
////                    canonizeError.printStackTrace();
////                  } catch (Exception e) {
////                    e.printStackTrace();
////                  }
////                }
////              }).start();
//
//                            stop = !currnetInternetState;
//                            runServer();
//                        }
//                        lastInternetState = currnetInternetState;
//                        // Sleep for 1000 milliseconds.
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
//    }


    public void ndnMailExecution() {
        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ExternalProxy.ndnMailSyncOneThread.start();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
        isFirstTime = false;
    }

//    public boolean shutdownlAllDbconnections(){
//        scheduleTaskExecutor.shutdownNow();
//        try {
//            Thread.sleep(1000);
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return scheduleTaskExecutor.isTerminated();
//
//    }

    public void startServer(String userEmail, String userPassword, View view) {
        System.out.println("In EmailRepo" + "username:" + userEmail + userPassword);
        registerPrefix();
//        progressStatus = 0;
        ExternalProxy.setUser(userEmail, userPassword);
        ExternalProxy.setSelectedProxy(2);
        if (isNetworkAvailable()) {
            System.out.println("Network available");

            startGmail(view);


        } else {
            shutdownRelayer();
            System.out.println("Network NOT available");
            startGmail(view);
        }
        ndnMailExecution();
    }

    /**
     * Clears Database and sets sync number and sync checkpoint to 0
     */
    public void clearDatabase() {

//        System.out.println("Again trying to clear");
        try {
            NdnFolder.syncNumber = 0;
            NdnFolder.syncCheckpoint = 0;
//            ImapToNdnTranslator.stopDB();
            new Database("Attribute", ndnDBConnection.getConfig()).close();
            new Database("MimeMessage", ndnDBConnection.getConfig()).close();
            new Database("MessageID", ndnDBConnection.getConfig()).close();
            new Database("MailFolder", ndnDBConnection.getConfig()).close();
            saveFlagMap();
            saveMessageUIDList();
//            System.out.println("Worked");


        } catch (CouchbaseLiteException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * A general function to display toast message for a short length given the context and the message
     *
     * @param context Fragment Context
     * @param text    Message to dsiplay
     */
    public void toast(final Context context, final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

            }
        });
    }

    /**
     * Updates progress bar with the given progress
     *
     * @param progress integer signifying the progress
     */
    synchronized public void updateProgress(final int progress) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                ProgressBar progressBar = view.findViewById(R.id.download_bar);
                progressBar.setProgress(progress);

            }
        });
    }

    /**
     * Saves the password and username as a shared preference
     * @param pass pass of the user
     * @param userName userName of the user
     */
    public void saveUser(String pass, String userName){
        sharedPreferences = context.getSharedPreferences("User",0);
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putString("pass",pass);
        editor.putString("userName",userName);
        editor.apply();
//        Log.d(TAG, "Username saved" + userName + "Pass saved" + pass);
//        Log.d(TAG, "User in SP" + sharedPreferences.getString("userName","") + "Pass in SP" + sharedPreferences.getString("pass",""));
    }

    /**
     * Removes user from the app by removing the account from the shared preference
     */
    public void removeUser(){
        sharedPreferences = context.getSharedPreferences("User",0);
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.remove("pass");
        editor.remove("userName");
        editor.clear();
        editor.commit();

        sharedPreferences = context.getSharedPreferences("User",0);
        String name = sharedPreferences.getString("userName","");
        String pass = sharedPreferences.getString("pass","");
//        Log.d("Remove","Remove User" + name + pass);

        hmap.clear();
    }

    /**
     * Returns user details so that it can be used to log in into the app
     * @return Hashmap containing the user details
     */
    public HashMap<String, String> getUser(){
        hmap.clear();
        sharedPreferences = context.getSharedPreferences("User",0);
        String name = sharedPreferences.getString("userName","");
        String pass = sharedPreferences.getString("pass","");
        if(name==null || pass==null) {
            return null;
        }
        hmap.put("name",name);
        hmap.put("pass",pass);
//        Log.d(TAG, "Username go" + hmap.get("name") + "Pass got" + hmap.get("pass"));
        return hmap;
    }

}


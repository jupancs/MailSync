package edu.ua.cs.nrl.mailsync.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;

import java.util.concurrent.ScheduledExecutorService;

import javax.mail.Message;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.EmailViewModel;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;

public class MainServerFragment extends BaseFragment {

    public static boolean stop = false;
    private static int progressStatus = 0;
    private final int LIMIT = 5;
    public Message[] messages;
    @BindView(R2.id.icon_letter)
    TextView iconLetter;

    //  @BindView(R2.id.get_ip)
//  Button getIpButton;
    @BindView(R2.id.email_account)
    TextView emailAccount;
    @BindView(R2.id.email_description)
    TextView emailDescription;
    @BindView(R2.id.run_server)
    Button runServerButton;
    @BindView(R2.id.btn_clear_database)
    Button clearDatabaseButton;
    @BindView(R2.id.server_status)
    TextView serverStatus;
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

    private boolean isFirstTime = true;
    private EmailViewModel emailViewModel;

    private String TAG = "MainServerFragment";

    public static MainServerFragment newInstance() {
        return new MainServerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        emailViewModel = ViewModelProviders.of(getActivity()).get(EmailViewModel.class);

        android.support.v7.app.ActionBar actionBar
                = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#ffffff"));
        actionBar.setBackgroundDrawable(colorDrawable);
        actionBar.setTitle(Html.fromHtml("<font color='#009a68'>MailSync</font>"));

//    Intent intent = getActivity().getIntent();
//    userEmail = intent.getExtras().getString("EMAIL_ACCOUNT");
//    userPassword = intent.getExtras().getString("EMAIL_PASSWORD");


        emailViewModel.init(userEmail, userPassword);


//        new Thread(new Runnable() {
//            public void run() {
//                while (true) {
//                    try {
//                        boolean currnetInternetState = isNetworkAvailable();
//                        if (lastInternetState != currnetInternetState) {
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
//                            getActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
////                  synchronized (TranslateWorker.class) {
//                                    runServerButton.performClick();
////                  }
//                                }
//                            });
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
    }

//    private void init(){
//        initializeDB();
//        initializeThreadPolicy();
//        initializeExternalProxy();
//    }
//
//    private void initializeExternalProxy() {
//        ExternalProxy.context = getContext().getApplicationContext();
//
//        if (isNetworkAvailable()) {
//            lastInternetState = true;
//            emailViewModel.setNetworkStatus(true);
//            ExternalProxy.setSelectedProxy(2);
//        } else {
//            lastInternetState = false;
//            emailViewModel.setNetworkStatus(false);
//            ExternalProxy.setSelectedProxy(2);
//        }
//    }
//
//    private void initializeThreadPolicy() {
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//    }
//
//    private void initializeDB() {
//        ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
//                "couchbaseLite",
//                getContext().getApplicationContext()
//        );
//    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_server, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        emailViewModel.getEmail().observe(this, userEmail -> {
            // update UI
            char firstLetter = Character.toUpperCase(userEmail.charAt(0));
            iconLetter.setText(String.valueOf(firstLetter));
            emailAccount.setText(userEmail);
            emailDescription.setText("You are running email account: " + userEmail + " for test.");
            this.userEmail = userEmail;
            Log.v(TAG, userEmail);
        });

        emailViewModel.getPassword().observe(this, userPassword -> {
            this.userPassword = userPassword;
        });


        return rootView;
    }


    @OnClick(R2.id.run_server)
    public void setRunServerButton() {
        Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
        emailViewModel.startServer(userEmail, userPassword);
        Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();
        serverStatus.setText("Running ...");
    }

    @OnClick(R2.id.btn_clear_database)
    public void setClearDatabaseButton() {
        EmailViewModel.clearDatabase();
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

//  @OnClick(R2.id.get_ip)
//  public void setGetIpButton() {
//    List<String> list = getArpLiveIps(true);
//    String ipAddr = "";
//    for (String str : list) {
//      ipAddr = str;
//    }
//    ClipboardManager clipboard = (ClipboardManager) getActivity()
//        .getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
//    ClipData clip = ClipData.newPlainText("simple text", "udp4://" + ipAddr + ":6363");
//    clipboard.setPrimaryClip(clip);
//
//    Toast.makeText(getActivity(),
//        "Route: " + "udp4://" + ipAddr + ":6363 is copied to the clipboard!",
//        Toast.LENGTH_LONG).show();
//  }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    // **********************
    // Helper methods below

    /**
     * Check out if the Internet if available.
     *
     * @return
     */
//    private boolean isNetworkAvailable() {
//        ConnectivityManager connectivityManager
//                = (ConnectivityManager) getActivity()
//                .getApplicationContext()
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
//    }
//
//    private void saveToNdnStorage(String user, String password) {
//        try {
//            Properties props = new Properties();
//            props.setProperty("mail.store.protocol", "imaps");
//            props.setProperty("mail.imap.host", "imap.gmail.com");
//
//            // create properties field
//            Session session = Session.getInstance(props, null);
//
//            // create the IMAP store object and connect with the pop server
//            Store store = session.getStore("imaps");
//
//            try {
//                store.connect("imap.gmail.com", user, password);
////        store.connect("127.0.0.1", 3143, user, password);
//            } catch (AuthenticationFailedException e) {
//                System.out.println("Login Failed: " + e.getMessage());
//            }
//
//            // create the folder object and open it
//            Folder emailFolder = store.getFolder("INBOX");
//            emailFolder.open(Folder.READ_WRITE);
//
//            NdnFolder.folder = (IMAPFolder) emailFolder;
//
//            messages = emailFolder.getMessages();
//            lastMailboxSize = messages.length;
//
//            System.out.println("messages.length---" + messages.length);
////      System.out.println("IMAP count: --" + NdnFolder.folder.getMessageCount());
//            int msgSize = messages.length;
//
//            int i = msgSize - 1;
//            while (true) {
//                Properties props2 = new Properties();
//                props2.setProperty("mail.store.protocol", "imaps");
//
//                // create properties field
//                Session session2 = Session.getInstance(props2, null);
//
//                // create the IMAP store object and connect with the pop server
//                Store store2 = session2.getStore("imaps");
//
//                try {
//                    store2.connect("imap.gmail.com", user, password);
////          store2.connect("127.0.0.1", 3143, user, password);
//                } catch (AuthenticationFailedException e) {
//                    System.out.println("Login Failed: " + e.getMessage());
//                }
//
//                // create the folder object and open it
//                Folder folder = store2.getFolder("INBOX");
//                folder.open(Folder.READ_WRITE);
//
//                NdnFolder.folder = (IMAPFolder) folder;
//
//                messages = new Message[folder.getMessageCount()];
//                messages = folder.getMessages();
//
//                int mailboxSize = folder.getMessageCount();
//
//                if (msgSize < mailboxSize) {
//                    for (int j = mailboxSize - 1; j >= msgSize; j--) {
//                        MimeMessage mimeMessage = (MimeMessage) messages[j];
//                        NdnFolder.messgeID.add(0, mimeMessage.getMessageID());
//                        System.out.println("size: " + j);
//                        TranslateWorker.start(mimeMessage, getContext());
//                    }
//                    msgSize = mailboxSize;
//                    i++;
//                } else if (msgSize - i <= LIMIT) {
//                    MimeMessage mimeMessage = (MimeMessage) messages[i];
//                    NdnFolder.messgeID.add(mimeMessage.getMessageID());
//                    System.out.println("Normallllllllllll size: " + i);
//                    TranslateWorker.start(mimeMessage, getContext());
//                }
//                if (msgSize - i <= LIMIT) {
//                    i--;
//                }
//                store2.close();
//                if (i > LIMIT) {
//                    Thread.sleep(200);
//                }
//            }
//        } catch (NoSuchProviderException e) {
//            e.printStackTrace();
//        } catch (MessagingException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//
//    /**
//     * Display database content in console
//     *
//     * @param database
//     * @throws CouchbaseLiteException
//     */
//    private void printDatabaseHelper(Database database) throws CouchbaseLiteException {
//        Query queryShowAll = QueryBuilder
//                .select(SelectResult.all())
//                .from(DataSource.database(database));
//        ResultSet resultShowAll = queryShowAll.execute();
//        System.out.println(">>> " + database.getName() + " <<<");
//        for (Result result : resultShowAll) {
//            System.out.println(result.toList().toString());
//        }
//        System.out.println(">>>>>>>>>>>><<<<<<<<<<<<");
//    }
//
//    /**
//     * Get IP addresses that connected to the Android hotspot
//     *
//     * @param onlyReachables
//     * @return a list of IP addresses
//     */
//    private ArrayList<String> getArpLiveIps(boolean onlyReachables) {
//        BufferedReader bufRead = null;
//        ArrayList<String> result = null;
//
//        try {
//            result = new ArrayList<>();
//            bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
//            String fileLine;
//            while ((fileLine = bufRead.readLine()) != null) {
//                String[] splitted = fileLine.split(" +");
//                if ((splitted != null) && (splitted.length >= 4)) {
//                    String mac = splitted[3];
//                    if (mac.matches("..:..:..:..:..:..")) {
//                        boolean isReachable = pingCmd(splitted[0]);/**
//                         * Method to Ping  IP Address
//                         * @return true if the IP address is reachable
//                         */
//                        if (!onlyReachables || isReachable) {
//                            result.add(splitted[0]);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//        } finally {
//            try {
//                bufRead.close();
//            } catch (IOException e) {
//            }
//        }
//        return result;
//    }
//
//    private boolean pingCmd(String addr) {
//        try {
//            String ping = "ping  -c 1 -W 1 " + addr;
//            Runtime run = Runtime.getRuntime();
//            Process pro = run.exec(ping);
//            try {
//                pro.waitFor();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            int exit = pro.exitValue();
//            if (exit == 0) {
//                return true;
//            } else {
//                //ip address is not reachable
//                return false;
//            }
//        } catch (IOException e) {
//        }
//        return false;
//    }
}

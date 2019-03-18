package edu.ua.cs.nrl.mailsync.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndntranslator.TranslateWorker;
import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;
import com.sun.mail.imap.IMAPFolder;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn_xx.util.FaceUri;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;
import edu.ua.cs.nrl.mailsync.utils.NfdcHelper;

public class MainServerFragment extends BaseFragment {

  @BindView(R2.id.icon_letter)
  TextView iconLetter;

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

  private Unbinder unbinder;

  private String userEmail;
  private String userPassword;

  private ScheduledExecutorService scheduleTaskExecutor;

  public Message[] messages;

  private boolean ndnService = false;

  private NdnDBConnection ndnDBConnection;

  private static int progressStatus = 0;
  private Handler handler = new Handler();

  private boolean lastInternetState = true;

  private Relayer relayer;
  boolean hasInternetBefore = false;

  public static boolean stop = false;

  private final int LIMIT = 5;
  private int lastMailboxSize;

  private boolean isFirstTime = true;

  public static MainServerFragment newInstance() {
    return new MainServerFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    android.support.v7.app.ActionBar actionBar
        = ((AppCompatActivity) getActivity()).getSupportActionBar();
    ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#ffffff"));
    actionBar.setBackgroundDrawable(colorDrawable);

    actionBar.setTitle(Html.fromHtml("<font color='#009a68'>MailSync</font>"));

    // Create a route for "mailsync"
    new Thread(new Runnable() {
      @Override
      public void run() {
        NfdcHelper nfdcHelper = new NfdcHelper();
        try {
          FaceStatus faceStatus =
              nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
          int faceId = faceStatus.getFaceId();
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

    Intent intent = getActivity().getIntent();
    userEmail = intent.getExtras().getString("EMAIL_ACCOUNT");
    userPassword = intent.getExtras().getString("EMAIL_PASSWORD");

    ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
        "couchbaseLite",
        getContext().getApplicationContext()
    );

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    ExternalProxy.context = getContext().getApplicationContext();

    if (isNetworkAvailable()) {
      lastInternetState = true;
      ExternalProxy.setSelectedProxy(2);
    } else {
      lastInternetState = false;
      ExternalProxy.setSelectedProxy(2);
    }

    new Thread(new Runnable() {
      public void run() {
        while (true) {
          try {
            boolean currnetInternetState = isNetworkAvailable();
            if (lastInternetState != currnetInternetState) {
              new Thread(new Runnable() {
                @Override
                public void run() {
                  NfdcHelper nfdcHelper = new NfdcHelper();
                  boolean routeExists = false;
                  try {
                    for (RibEntry ribEntry : nfdcHelper.ribList()) {
                      if (ribEntry.getName().toString().equals("udp4://224.0.23.170:56363")) {
                        routeExists = true;
                        break;
                      }
                    }
                    if (!routeExists) {
                      FaceStatus faceStatus =
                          nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
                      int faceId = faceStatus.getFaceId();
                      nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
                    }
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
              stop = !currnetInternetState;
              getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                  synchronized (TranslateWorker.class) {
                    runServerButton.performClick();
//                  }
                }
              });
            }
            lastInternetState = currnetInternetState;
            // Sleep for 1000 milliseconds.
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_main_server, container, false);
    unbinder = ButterKnife.bind(this, rootView);

    char firstLetter = Character.toUpperCase(userEmail.charAt(0));
    iconLetter.setText(String.valueOf(firstLetter));
    emailAccount.setText(userEmail);
    emailDescription.setText("You are running email account: " + userEmail + " for test.");

    return rootView;
  }
//
//  @OnCheckedChanged(R2.id.main_server_switch)
//  public void setServiceSwitch() {
//    if (ndnService) {
//      ExternalProxy.setSelectedProxy(2);
//      ndnService = false;
//    } else {
//      ExternalProxy.setSelectedProxy(1);
//      ndnService = true;
//    }
//  }
  
  @OnClick(R2.id.run_server)
  public void setRunServerButton() {
    progressStatus = 0;
    ExternalProxy.setUser(userEmail, userPassword);
    ExternalProxy.setSelectedProxy(2);

    if (isNetworkAvailable()) {
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
      relayer = new Relayer(3143);
      relayer.execute(new String[]{""});

      if (!isFirstTime) {
        ExternalProxy.ndnMailSyncOneThread.face_.shutdown();
      }

      ExternalProxy.ndnMailSyncOneThread =
          new NDNMailSyncOneThread(getContext().getApplicationContext());
    } else {
      if (hasInternetBefore) {
        try {
          if (relayer.getServerSocket() != null) {
            relayer.getServerSocket().close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      new Thread(new Runnable() {
        public void run() {
          ExternalProxy.gmail.start();
//          fetchFromInternet(userEmail, userPassword);
        }
      }).start();

      System.out.println("Network NOT available");
      ExternalProxy.setSelectedProxy(2);

      if (!isFirstTime) {
        ExternalProxy.ndnMailSyncOneThread.face_.shutdown();
      }

      ExternalProxy.ndnMailSyncOneThread =
          new NDNMailSyncOneThread(getContext().getApplicationContext());
    }

    Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();

    scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
    scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
      public void run() {
        ExternalProxy.ndnMailSyncOneThread.start();
      }
    }, 0, 10, TimeUnit.MILLISECONDS);

    isFirstTime = false;

    serverStatus.setText("Running ...");
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

  @OnClick(R2.id.btn_clear_database)
  public void setClearDatabaseButton() {
    try {
      new Database("MailFolder", ndnDBConnection.getConfig()).delete();
      new Database("Attribute", ndnDBConnection.getConfig()).delete();
      new Database("MimeMessage", ndnDBConnection.getConfig()).delete();
      new Database("MessageID", ndnDBConnection.getConfig()).delete();
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }
  }

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
  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getActivity()
                                .getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  private void saveToNdnStorage(String user, String password) {
    try {
      Properties props = new Properties();
      props.setProperty("mail.store.protocol", "imaps");
      props.setProperty("mail.imap.host", "imap.gmail.com");

      // create properties field
      Session session = Session.getInstance(props, null);

      // create the IMAP store object and connect with the pop server
      Store store = session.getStore("imaps");

      try {
        store.connect("imap.gmail.com", user, password);
//        store.connect("127.0.0.1", 3143, user, password);
      } catch (AuthenticationFailedException e) {
        System.out.println("Login Failed: " + e.getMessage());
      }

      // create the folder object and open it
      Folder emailFolder = store.getFolder("INBOX");
      emailFolder.open(Folder.READ_WRITE);

      NdnFolder.folder = (IMAPFolder) emailFolder;

      messages = emailFolder.getMessages();
      lastMailboxSize = messages.length;

      System.out.println("messages.length---" + messages.length);
//      System.out.println("IMAP count: --" + NdnFolder.folder.getMessageCount());
      int msgSize = messages.length;

      int i = msgSize - 1;
      while (true) {
        Properties props2 = new Properties();
        props2.setProperty("mail.store.protocol", "imaps");

        // create properties field
        Session session2 = Session.getInstance(props2, null);

        // create the IMAP store object and connect with the pop server
        Store store2 = session2.getStore("imaps");

        try {
          store2.connect("imap.gmail.com", user, password);
//          store2.connect("127.0.0.1", 3143, user, password);
        } catch (AuthenticationFailedException e) {
          System.out.println("Login Failed: " + e.getMessage());
        }

        // create the folder object and open it
        Folder folder = store2.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        NdnFolder.folder = (IMAPFolder) folder;

        messages = new Message[folder.getMessageCount()];
        messages = folder.getMessages();

        int mailboxSize = folder.getMessageCount();

        if (msgSize < mailboxSize) {
          for (int j = mailboxSize - 1; j >= msgSize; j--) {
            MimeMessage mimeMessage = (MimeMessage) messages[j];
            NdnFolder.messgeID.add(0, mimeMessage.getMessageID());
            System.out.println("size: " + j);
            TranslateWorker.start(mimeMessage, getContext());
          }
          msgSize = mailboxSize;
          i++;
        } else if (msgSize - i <= LIMIT) {
          MimeMessage mimeMessage = (MimeMessage) messages[i];
          NdnFolder.messgeID.add(mimeMessage.getMessageID());
          System.out.println("Normallllllllllll size: " + i);
          TranslateWorker.start(mimeMessage, getContext());
        }
        if (msgSize - i <= LIMIT) {
          i--;
        }
        store2.close();
        if (i > LIMIT) {
          Thread.sleep(200);
        }
      }
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

//  /**
//   * Download emails from Gmail server and feed them into GreenMail storage.
//   *
//   * @param user
//   * @param password
//   */
//  public void fetchFromInternet(String user, String password) {
//
//    try {
//      Properties props = new Properties();
//      props.setProperty("mail.store.protocol", "imaps");
//      props.setProperty("mail.imap.host", "imap.gmail.com");
//
//      // create properties field
//      Session session = Session.getInstance(props, null);
//
//      // create the IMAP store object and connect with the pop server
//      Store store = session.getStore("imaps");
//
//      try {
//        store.connect("imap.gmail.com", user, password);
//      } catch (AuthenticationFailedException e) {
//        System.out.println("Login Failed: " + e.getMessage());
//
//      }
//
//      // create the folder object and open it
//      Folder emailFolder = store.getFolder("INBOX");
//      emailFolder.open(Folder.READ_WRITE);
//
//      IMAPFolder imapFolder = (IMAPFolder) emailFolder;
//      NdnFolder.folder = imapFolder;
//
//      if (NdnFolder.folder != null && NdnFolder.folder.exists()) {
//        if (NdnFolder.folder.isOpen() && NdnFolder.folder.getMode() != Folder.READ_WRITE) {
//          NdnFolder.folder.close(false);
//          NdnFolder.folder.open(Folder.READ_WRITE);
//        } else if (!NdnFolder.folder.isOpen()) {
//          NdnFolder.folder.open(Folder.READ_WRITE);
//        }
//      } else {
//        String errMsg = "Folder not found";
//        System.out.println(errMsg);
//      }
//
//      messages = NdnFolder.folder.getMessages();
//
//      System.out.println("messages.length---" + messages.length);
//      System.out.println("IMAP count: --" + NdnFolder.folder.getMessageCount());
//      int msgSize = messages.length;
//
//      ImapServer imapS = ExternalProxy.gmail.getImap();
//      Managers manager = ExternalProxy.gmail.getManagers();
//      ImapHostManager imapM = manager.getImapHostManager();
//      UserManager userManager = manager.getUserManager();
//      Flags testFlags = new Flags();
//
//      imapS.getServerSetup();
//      MailFolder mailFolder = imapM.getInbox(userManager.getUserByEmail(userEmail));
//
//      NdnFolder.messageUids = NdnFolder.getMessageUids();
//      System.out.println("UID download finished!");
//
////      for (int i = msgSize - 1; i >= 0; i--) {
//////
//////        Properties props2 = new Properties();
//////        props2.setProperty("mail.store.protocol", "imaps");
//////
//////        // create properties field
//////        Session session2 = Session.getInstance(props2, null);
//////
//////        // create the IMAP store object and connect with the pop server
//////        Store store2 = session2.getStore("imaps");
//////
//////        try {
//////          store2.connect("imap.gmail.com", user, password);
//////        } catch (AuthenticationFailedException e) {
//////          System.out.println("Login Failed: " + e.getMessage());
//////        }
//////
//////        // create the folder object and open it
//////        Folder folder = store2.getFolder("INBOX");
//////        folder.open(Folder.READ_WRITE);
//////
//////        NdnFolder.folder = (IMAPFolder) folder;
//////
//////        if (NdnFolder.folder != null && NdnFolder.folder.exists()) {
//////          if (NdnFolder.folder.isOpen() && NdnFolder.folder.getMode() != Folder.READ_WRITE) {
//////            NdnFolder.folder.close(false);
//////            NdnFolder.folder.open(Folder.READ_WRITE);
//////          } else if (!NdnFolder.folder.isOpen()) {
//////            NdnFolder.folder.open(Folder.READ_WRITE);
//////          }
//////        } else {
//////          String errMsg = "Folder not found";
//////          System.out.println(errMsg);
//////        }
//////
//////        messages = new Message[folder.getMessageCount()];
//////        messages = folder.getMessages();
//////        int mailboxSize = folder.getMessageCount();
//////
//////        if (msgSize < mailboxSize) {
//////          for (int j = mailboxSize - 1; j >= msgSize; j--) {
//////            MimeMessage mimeMessage = (MimeMessage) messages[j];
//////
//////            long uid = NdnFolder.folder.getUID(messages[j]);
////////
////////            System.out.println(">>>>>> UID: " + uid);
////////            System.out.println(">>>>>> MSN: " + NdnFolder.getMsn(uid));
////////
////////            ndnDBConnection.saveNDNData(mimeMessage.getMessageID(), "".getBytes(), "MessageID");
////////            System.out.println("Saved Email[" +
////////                (new Database("MessageID", ndnDBConnection.getConfig()).getCount()) + "]");
////////
////////            System.out.println("This is manager: " + imapM);
////////            mailFolder.appendMessageNDN(mimeMessage, testFlags, null,
////////                getActivity().getApplicationContext());
//////          }
//////          msgSize = mailboxSize;
//////          i++;
//////        } else {
//////          MimeMessage mimeMessage = (MimeMessage) messages[i];
//////
//////          long uid = NdnFolder.folder.getUID(messages[i]);
//////
//////          String messageID = mimeMessage.getMessageID();
//////
////////          // Check if database already has this message
////////          Query query = QueryBuilder
////////              .select(SelectResult.property("name"))
////////              .from(DataSource.database(new Database("MessageID", ndnDBConnection.getConfig())))
////////              .where(Expression.property("name").equalTo(Expression.string(messageID)));
////////
////////          ResultSet resultSet = query.execute();
////////          if (resultSet.allResults().size() == 0) {
////////            ndnDBConnection.saveNDNData(mimeMessage.getMessageID(), "".getBytes(), "MessageID");
////////            System.out.println("Saved Email[" +
////////                (new Database("MessageID", ndnDBConnection.getConfig()).getCount()) + "]");
////////
////////            System.out.println("This is manager: " + imapM);
////////            mailFolder.appendMessageNDN(mimeMessage, testFlags, null,
////////                getActivity().getApplicationContext());
////////          }
//////        }
//////
//////        store2.close();
//////
//////      }
//    } catch (NoSuchProviderException e) {
//      e.printStackTrace();
//    } catch (MessagingException e) {
//      e.printStackTrace();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

  /**
   * Display database content in console
   *
   * @param database
   * @throws CouchbaseLiteException
   */
  private void printDatabaseHelper(Database database) throws CouchbaseLiteException {
    Query queryShowAll = QueryBuilder
        .select(SelectResult.all())
        .from(DataSource.database(database));
    ResultSet resultShowAll = queryShowAll.execute();
    System.out.println(">>> " + database.getName() + " <<<");
    for (Result result : resultShowAll) {
      System.out.println(result.toList().toString());
    }
    System.out.println(">>>>>>>>>>>><<<<<<<<<<<<");
  }

  /**
   * Get IP addresses that connected to the Android hotspot
   *
   * @param onlyReachables
   * @return a list of IP addresses
   */
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

  private boolean pingCmd(String addr){
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
    }
    catch (IOException e) {
    }
    return false;
  }
}

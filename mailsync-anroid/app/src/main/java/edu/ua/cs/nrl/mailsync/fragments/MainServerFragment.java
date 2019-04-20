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

  private NdnDBConnection ndnDBConnection;

  private boolean lastInternetState = true;

  private Relayer relayer;
  boolean hasInternetBefore = false;

  public static boolean stop = false;

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

    // get the user information from login fragment
    Intent intent = getActivity().getIntent();
    userEmail = intent.getExtras().getString("EMAIL_ACCOUNT");
    userPassword = intent.getExtras().getString("EMAIL_PASSWORD");

    // set the ndn database connection
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

    //nfdcHelperServer ();

  }

  /**
   * Nfdc helper server, it starts the nfdc starter server
   */
  private void nfdcHelperServer () {
    new Thread(new Runnable() {
      public void run() {

        while (true) {
          try {
            boolean currnetInternetState = isNetworkAvailable();
            if (lastInternetState != currnetInternetState) {

              //startNfdcServer ();
              stop = !currnetInternetState;
              if (isAdded()){
                getActivity().runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    runServerButton.performClick();
                  }
                });}
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

  /**
   * the nfdc starter server, it registers a nfdc face
   */
  private void startNfdcServer () {
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
  
  @OnClick(R2.id.run_server)
  /**
   * set the actions when the server button is pressed
   * the actions include: set user, set external proxy, set the main activity
   * then start the greenmail server which will translate and store the mail to
   * the storage.
   */
  public void setRunServerButton() {

    ExternalProxy.setUser(userEmail, userPassword);
    ExternalProxy.setSelectedProxy(2);
    ExternalProxy.setMainActivity(getActivity());

    if (isNetworkAvailable()) {
      System.out.println("Network available");
      hasInternetBefore = true;

      startGreenMailServer ();

      // Start the relayer service
      relayer = new Relayer(3143, getContext());
      relayer.execute(new String[]{""});

    } else {
      System.out.println("Network NOT available");
      if (hasInternetBefore) {
        try {
          if (relayer.getServerSocket() != null) {
            relayer.getServerSocket().close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      startGreenMailServer ();
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

  /**
   * Start the greenmail server, and set the external proxy to be local host
   */
  private void startGreenMailServer () {
    new Thread(new Runnable() {
      public void run() {
        ExternalProxy.greenMail.start();
      }
    }).start();
    ExternalProxy.setSelectedProxy(2);

    if (!isFirstTime) {
      ExternalProxy.ndnMailSyncOneThread.face_.shutdown();
    }
    ExternalProxy.ndnMailSyncOneThread =
            new NDNMailSyncOneThread(getContext().getApplicationContext());
  }

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
   * @return true if the network is available, false otherwise
   */
  private boolean isNetworkAvailable() {
    if (isAdded()) {
      ConnectivityManager connectivityManager
              = (ConnectivityManager) getActivity()
              .getApplicationContext()
              .getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    return false;
  }
}

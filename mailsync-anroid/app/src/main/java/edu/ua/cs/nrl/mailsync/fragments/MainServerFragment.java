package edu.ua.cs.nrl.mailsync.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import javax.mail.Message;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.EmailRepository;
import edu.ua.cs.nrl.mailsync.EmailViewModel;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;

public class MainServerFragment extends BaseFragment {

    public static boolean stop = false;
    private static int progressStatus = 0;
    private final int LIMIT = 5;
    public Message[] messages;
    @BindView(R2.id.icon_letter)
    TextView iconLetter;
    private boolean lastInternetState = true;
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
    final String packageName = "net.named_data.nfd";
    private Unbinder unbinder;
    private String userEmail;
    private String userPassword;
    private EmailViewModel emailViewModel;
    ProgressBar progressBar;

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


    }


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
        //initializes the view of viewmodel and the progress bar to 0
        Button button = getActivity().findViewById(R.id.run_server);
        emailViewModel.view= rootView;
        emailViewModel.init(userEmail, userPassword,button );
        progressBar= rootView.findViewById(R.id.download_bar);
        progressBar.setProgress(0);
        runServer();


        return rootView;
    }

    /**
     * Starts a thread that switches from ndnmode to normal mode depending on if network is available
     */
    public void runServer(){
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        boolean currnetInternetState = EmailViewModel.isNetworkAvailable();
                        if (lastInternetState != currnetInternetState) {
//              NfdcHelper nfdcHelper = new NfdcHelper();
//              boolean routeExists = false;
//              try {
//                for (RibEntry ribEntry : nfdcHelper.ribList()) {
//                  if (ribEntry.getName().toString().equals("udp4://224.0.23.170:56363")) {
//                    routeExists = true;
//                    break;
//                  }
//                }
//                if (!routeExists) {
//                  FaceStatus faceStatus =
//                      nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
//                  int faceId = faceStatus.getFaceId();
//                  nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
//                }
//                nfdcHelper.shutdown();
//              } catch (ManagementException e) {
//                e.printStackTrace();
//              } catch (FaceUri.CanonizeError canonizeError) {
//                canonizeError.printStackTrace();
//              } catch (Exception e) {
//                e.printStackTrace();
//              }

//              new Thread(new Runnable() {
//                @Override
//                public void run() {
//                  NfdcHelper nfdcHelper = new NfdcHelper();
//                  boolean routeExists = false;
//                  try {
//                    for (RibEntry ribEntry : nfdcHelper.ribList()) {
//                      if (ribEntry.getName().toString().equals("udp4://224.0.23.170:56363")) {
//                        routeExists = true;
//                        break;
//                      }
//                    }
//                    if (!routeExists) {
//                      FaceStatus faceStatus =
//                          nfdcHelper.faceListAsFaceUriMap(getContext()).get("udp4://224.0.23.170:56363");
//                      int faceId = faceStatus.getFaceId();
//                      nfdcHelper.ribRegisterPrefix(new Name("mailSync"), faceId, 10, true, false);
//                    }
//                    nfdcHelper.shutdown();
//                  } catch (ManagementException e) {
//                    e.printStackTrace();
//                  } catch (FaceUri.CanonizeError canonizeError) {
//                    canonizeError.printStackTrace();
//                  } catch (Exception e) {
//                    e.printStackTrace();
//                  }
//                }
//              }).start();

                            stop = !currnetInternetState;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                  synchronized (TranslateWorker.class) {
//                                    emailViewModel.shutdownRelayer();
                                    runServerButton.performClick();
                                    System.out.println("Server Started");
                                    emailViewModel.getAllUids();
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


    /**
     * Start Server
     */
    @OnClick(R2.id.run_server)
    public void setRunServerButton() {
        Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Are you working"  + userEmail+userPassword);
        emailViewModel.startServer(userEmail, userPassword);
        Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();
        serverStatus.setText("Running ...");
    }

    /**
     * Deletes database and updates UI
     */
    @OnClick(R2.id.btn_clear_database)
    public void setClearDatabaseButton() {
        EmailViewModel.clearDatabase();
        EmailRepository emailRepository = new EmailRepository();
        emailRepository.deleteAllStoredMessage();
    }

    @OnClick(R.id.run_nfd)
    public void setRunNfd(){
        startNewActivity(getContext(),packageName);
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

    //Opens a new activity
    // If nfd is installed opens it
    // If nfd is not installed then goes to the play store and asks user to install it
    public void startNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            // We found the activity now start the activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

}

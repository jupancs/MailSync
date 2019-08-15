package edu.ua.cs.nrl.mailsync.fragments;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.HashMap;

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
    private GoogleSignInClient googleSignInClient;
    GoogleSignInAccount account;

    private String TAG = "MainServerFragment";
    private boolean isGoogleSignin = false;

    public static MainServerFragment newInstance() {
        return new MainServerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        emailViewModel = ViewModelProviders.of(getActivity()).get(EmailViewModel.class);
//        android.support.v7.app.ActionBar actionBar
//                = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#ffffff"));
//        actionBar.setBackgroundDrawable(colorDrawable);
//        actionBar.setTitle(Html.fromHtml("<font color='#009a68'>MailSync</font>"));
//        actionBar.show();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        account = GoogleSignIn.getLastSignedInAccount(getActivity());
        if (account != null) {
            isGoogleSignin = true;
        }

        emailViewModel.init(userEmail, userPassword);

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
        emailViewModel.init(userEmail, userPassword);
        progressBar= rootView.findViewById(R.id.download_bar);
        progressBar.setProgress(0);
        runServer();


        return rootView;
    }

    /**
     * Updates the NDNfolder messageuidlist and flags hashmap with the stored uidlist and hashmap
     */
    @Override
    public void onResume() {
        EmailRepository.updateMailboxUids();
        super.onResume();
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
                                    if(runServerButton!=null){
                                        runServerButton.performClick();
                                    }

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
     * Depends on if the user is logged in through gmail or through traditional app username and password
     * If user is logged through google sign in then a pop up dialog is created to ask password from the user
     * because google does not give access to password  and then starts a server
     * If user logged in through normal means then there is no dialog box that will open
     */
    @OnClick(R2.id.run_server)
    public void setRunServerButton() {
//        serverStatus.setText(R.string.running);
//        Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
//        Log.d(TAG,"Are you working"  + userEmail+userPassword);
//        emailViewModel.startServer(userEmail, userPassword);
//        Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();

        if (isGoogleSignin) {
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.customdialog);
            dialog.setTitle("Connect");
            Button dialogButton = (Button) dialog.findViewById(R.id.btnLogin);
            EditText editText = dialog.findViewById(R.id.etPassword);
            // if button is clicked, close the custom dialog
            HashMap<String, String> hmap = emailViewModel.getUser();
            if(hmap==null||hmap.get("pass")==null ||hmap.get("name")==null|| hmap.get("pass").equals("") || hmap.get("name").equals("")){
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText editText = dialog.findViewById(R.id.etPassword);
                        System.out.println("Edit text " + editText);

                        if (editText!= null) {
                            emailViewModel.getPassword().setValue(String.valueOf(editText.getText()));
                            emailViewModel.getEmail().setValue(account.getEmail());
                            System.out.println("Dialog saved" + userEmail + userPassword);
                            emailViewModel.init(userEmail, userPassword);
                            Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
                            emailViewModel.startServer(userEmail, userPassword);
                            emailViewModel.saveUser(userPassword,userEmail);
                            Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();
                            serverStatus.setText(getString(R.string.running));
                        }

                        dialog.dismiss();
                    }
                });

                dialog.show();
            } else {
                userPassword = hmap.get("pass");
                userEmail = hmap.get("name");
                emailViewModel.getPassword().setValue(String.valueOf(userPassword));
                emailViewModel.init(userEmail, userPassword);
                Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
                emailViewModel.startServer(userEmail, userPassword);
                Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();
                serverStatus.setText(getString(R.string.running));
            }


        }
        else{
            Toast.makeText(getContext(), userEmail + userPassword, Toast.LENGTH_SHORT).show();
            emailViewModel.startServer(userEmail, userPassword);
            Toast.makeText(getActivity(), "Server is running ...", Toast.LENGTH_SHORT).show();
            serverStatus.setText(getString(R.string.running));

        }



    }

    /**
     * Deletes database and updates UI
     */
    @OnClick(R2.id.btn_clear_database)
    public void setClearDatabaseButton() {
//        System.out.println("Trying to clear");
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

    /**
     * When signout is clicked the gmail account is signed out and then sent to
     * the login page
     */
    @OnClick(R.id.sign_out)
    public void signOut() {
        if (googleSignInClient != null) {
            googleSignInClient.signOut()
                    .addOnCompleteListener(getActivity(), (task) -> {
                        Toast.makeText(getContext(), "Signed Out", Toast.LENGTH_SHORT).show();
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.activity_fragment_base_fragmentContainer, new LoginFragment());
                        fragmentTransaction.commit();
                    });
        } else {
            emailViewModel.removeUser();
            Toast.makeText(getContext(), "Signed Out" + emailViewModel.getEmail() + emailViewModel.getPassword(), Toast.LENGTH_SHORT).show();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.activity_fragment_base_fragmentContainer, new LoginFragment());
            fragmentTransaction.commit();
        }
        emailViewModel.removeUser();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

}

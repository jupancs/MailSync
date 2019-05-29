package edu.ua.cs.nrl.mailsync.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

import javax.mail.Message;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
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
    private Unbinder unbinder;
    private String userEmail;
    private String userPassword;
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

}

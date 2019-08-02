package edu.ua.cs.nrl.mailsync.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.EmailViewModel;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;
import edu.ua.cs.nrl.mailsync.activities.BaseFragmentActivity;
import edu.ua.cs.nrl.mailsync.activities.MainServerActivity;

public class LoginFragment extends BaseFragment {


  @BindView(R2.id.fragment_login_userEmail)
  TextInputEditText userEmailEditText;

  @BindView(R2.id.fragment_login_userPassword)
  TextInputEditText userPasswordEditText;

  @BindView(R2.id.fragment_login_login_button)
  Button loginButton;

  int RC_SIGN_IN =0;
  GoogleSignInClient googleSignInClient;
  private Unbinder unbinder;

  private String email;
  private String password;
  EmailViewModel emailViewModel;
  private String TAG = "Login Fragment";

  public static LoginFragment newInstance() {
    return new LoginFragment();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == RC_SIGN_IN){
      System.out.println("In here gmail 2");
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
      handleSiginResult(task);

    }
  }

  public void handleSiginResult(Task<GoogleSignInAccount> task){
    try{
      System.out.println("In here gmail");
      GoogleSignInAccount account = task.getResult(ApiException.class);
      emailViewModel.getEmail().setValue(account.getEmail());
      emailViewModel.getPassword().setValue("Google Sign In");
      FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
      fragmentTransaction.replace(R.id.activity_fragment_base_fragmentContainer,new MainServerFragment());
      fragmentTransaction.commit();
    } catch (ApiException e){
      System.out.println(e);
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build();
    googleSignInClient = GoogleSignIn.getClient(getActivity(),gso);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_login, container, false);
    unbinder = ButterKnife.bind(this, rootView);
    emailViewModel= ViewModelProviders.of(getActivity()).get(EmailViewModel.class);
    return rootView;
  }

  @OnClick(R2.id.fragment_login_login_button)
  public void setLoginButton() {
    email = userEmailEditText.getText().toString();
    password = userPasswordEditText.getText().toString();
    emailViewModel.getEmail().setValue(email);
    emailViewModel.getPassword().setValue(password);

    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
    fragmentTransaction.replace(R.id.activity_fragment_base_fragmentContainer,new MainServerFragment());
    fragmentTransaction.commit();
  }
  @OnClick(R.id.sign_in_button)
  public void clickGmailLogin(){
      System.out.println("In here gmail");
      Intent signInIntent = googleSignInClient.getSignInIntent();
      startActivityForResult(signInIntent,RC_SIGN_IN);
  }

  @Override
  public void onStart() {
    super.onStart();
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
    if(account!=null){
      emailViewModel.getEmail().setValue(account.getEmail());
      emailViewModel.getPassword().setValue("Google Sign In");
      FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
      fragmentTransaction.replace(R.id.activity_fragment_base_fragmentContainer,new MainServerFragment());
      fragmentTransaction.commit();
    }
  }


  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }
}

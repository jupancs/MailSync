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

  private Unbinder unbinder;

  private String email;
  private String password;
  EmailViewModel emailViewModel;
  private String TAG = "Login Fragment";

  public static LoginFragment newInstance() {
    return new LoginFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
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




  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }
}

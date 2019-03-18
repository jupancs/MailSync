package edu.ua.cs.nrl.mailsync.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;
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
    return rootView;
  }

  @OnClick(R2.id.fragment_login_login_button)
  public void setLoginButton() {
    email = userEmailEditText.getText().toString();
    password = userPasswordEditText.getText().toString();

    Intent intent = new Intent(getActivity(), MainServerActivity.class);
    intent.putExtra("EMAIL_ACCOUNT", email);
    intent.putExtra("EMAIL_PASSWORD", password);
    startActivity(intent);
  }




  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }
}

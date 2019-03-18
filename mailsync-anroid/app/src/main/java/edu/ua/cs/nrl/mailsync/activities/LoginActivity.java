package edu.ua.cs.nrl.mailsync.activities;

import android.support.v4.app.Fragment;

import edu.ua.cs.nrl.mailsync.fragments.LoginFragment;

public class LoginActivity extends BaseFragmentActivity {
  @Override
  Fragment createFragment() {
    return LoginFragment.newInstance();
  }
}

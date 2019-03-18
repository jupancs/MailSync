package edu.ua.cs.nrl.mailsync.activities;

import android.support.v4.app.Fragment;

import edu.ua.cs.nrl.mailsync.fragments.MainServerFragment;

public class MainServerActivity extends BaseFragmentActivity {
  @Override
  Fragment createFragment() {
    return new MainServerFragment().newInstance();
  }
}

package edu.ua.cs.nrl.mailsync.fragments;

import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.R2;
import edu.ua.cs.nrl.mailsync.activities.BaseActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class TestIntent {
        @Rule
    public IntentsTestRule<BaseActivity> intentsTestRule =
            new IntentsTestRule<>(BaseActivity.class);

    //Test passes only if NFD is not already installed
    @Test
    public void check_PlayStoreOpened_If_NFD_Not_Installed() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_login_button)).perform(click());
        onView(withId(R.id.run_nfd)).perform(click());
//        intended(toPackage("net.named_data.nfd"));
        intended(hasData(Uri.parse("market://details?id=" + "net.named_data.nfd")));
        intended(hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    //Test passes only if NFD is already installed
    @Test
    public void checkNFDOpened_If_Installed() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_login_button)).perform(click());
        onView(withId(R.id.run_nfd)).perform(click());
        intended(toPackage("net.named_data.nfd"));
    }





}

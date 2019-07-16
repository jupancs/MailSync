package edu.ua.cs.nrl.mailsync.fragments;


import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MainServerFragmentTest {
    @Rule
    public ActivityTestRule<BaseActivity> mActivityRule =
            new ActivityTestRule<>(BaseActivity.class);
//    @Rule
//    public IntentsTestRule<BaseActivity> intentsTestRule =
//            new IntentsTestRule<>(BaseActivity.class);
    @Test
    public void runServer() {
    }

    @Test
    public void checkRunServerStartText() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());

        onView(withId(R2.id.fragment_login_login_button)).perform(click());
//        onView(withId(R2.id.run_server)).perform(click());
        // Check that the text was changed.
        onView(withId(R2.id.server_status)).check(matches(withText("Server IDEL")));
    }

    @Test
    public void checkEmailDescription() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());

        onView(withId(R2.id.fragment_login_login_button)).perform(click());
//        onView(withId(R2.id.run_server)).perform(click());
        // Check that the text was changed.
        onView(withId(R2.id.email_description)).check(matches(withText("You are running email account: " + "mailtestm72@gmail.com" + " for test.")));
    }

    @Test
    public void checkEmailAccountText() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());

        onView(withId(R2.id.fragment_login_login_button)).perform(click());
//        onView(withId(R2.id.run_server)).perform(click());
        // Check that the text was changed.
        onView(withId(R2.id.email_account)).check(matches(withText("mailtestm72@gmail.com")));
    }
    @Test
    public void checkIconLetter() {
        onView(withId(R2.id.fragment_login_userEmail))
                .perform(clearText())
                .perform(typeText("mailtestm72@gmail.com"), closeSoftKeyboard());
        onView(withId(R2.id.fragment_login_userPassword))
                .perform(clearText())
                .perform(typeText("Abcdef12"), closeSoftKeyboard());

        onView(withId(R2.id.fragment_login_login_button)).perform(click());
//        onView(withId(R2.id.run_server)).perform(click());
        // Check that the text was changed.
        onView(withId(R2.id.icon_letter)).check(matches(withText("M")));

    }
}
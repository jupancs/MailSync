package edu.ua.cs.nrl.mailsync;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;


/**
 * Handles the information needed to be maintained throughout the use of the app
 * and connects the view to the repository
 * Also deals with calls EmailRepository for initialization
 * Follows the architecture linked below
 *
 * @see <a href="https://developer.android.com/jetpack/docs/guide">https://developer.android.com/jetpack/docs/guide</a>
 */
public class EmailViewModel extends AndroidViewModel {
    private MutableLiveData<String> email;
    private MutableLiveData<String> password;
    private static EmailRepository emailRepository;
    public View view;


    public EmailViewModel(@NonNull Application application) {
        super(application);

    }

    /**
     * Clears Database by calling clearDatabase method of EmailRepository
     */
    public static void clearDatabase() {
        emailRepository.clearDatabase();

    }

    /**
     * @return true if network is available or else false
     */
    public static boolean isNetworkAvailable() {
        return emailRepository.isNetworkAvailable();
    }

    public MutableLiveData<String> getPassword() {
        if (password == null) {
            password = new MutableLiveData<>();
        }
        return password;
    }

    /**
     * @return email of the user
     */
    public MutableLiveData<String> getEmail() {
        if (email == null) {
            email = new MutableLiveData<>();
        }
        return email;
    }


    /**
     * Starts server in emailRepo and passes view so that it can be updated
     *
     * @param userEmail    email of the user
     * @param userPassword password of the user
     */
    public void startServer(String userEmail, String userPassword) {
        emailRepository.startServer(userEmail, userPassword, view);
    }

//    public void shutdownRelayer(){
//        emailRepository.stopGmail();
//    }

    //

    /**
     * init initializes the view in EmailRepo
     *
     * @param userEmail    email of the user
     * @param userPassword password of the user
     * @param button       startserver button
     */
    public void init(String userEmail, String userPassword, Button button) {
        emailRepository = new EmailRepository(getApplication().getApplicationContext(), userEmail, userPassword);
        emailRepository.init(view);


    }

    /**
     * Calls function getAllUids of the emailRepository class
     */
    public void getAllUids() {
        emailRepository.getAllUids();
    }


}

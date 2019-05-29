package edu.ua.cs.nrl.mailsync;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;


public class EmailViewModel extends AndroidViewModel {
    private MutableLiveData<String> email;
    private MutableLiveData<String> password;
    private static EmailRepository emailRepository;
    private boolean networkStatus;


    public EmailViewModel(@NonNull Application application) {
        super(application);

    }

    public static void clearDatabase() {
        emailRepository.clearDatabase();

    }

    public MutableLiveData<String> getPassword() {
        if (password == null) {
            password = new MutableLiveData<>();
        }
        return password;
    }

    public MutableLiveData<String> getEmail() {
        if (email == null) {
            email = new MutableLiveData<>();
        }
        return email;
    }

    public void startServer(String userEmail, String userPassword) {
        emailRepository.startServer(userEmail, userPassword);
    }

    public void init(String userEmail, String userPassword) {
        emailRepository = new EmailRepository(getApplication().getApplicationContext(), userEmail, userPassword);
        emailRepository.init();


    }



}

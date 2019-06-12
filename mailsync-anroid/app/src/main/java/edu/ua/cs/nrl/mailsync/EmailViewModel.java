package edu.ua.cs.nrl.mailsync;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;


public class EmailViewModel extends AndroidViewModel {
    private MutableLiveData<String> email;
    private MutableLiveData<String> password;
    private static EmailRepository emailRepository;
    private boolean networkStatus;
    public View view;


    public EmailViewModel(@NonNull Application application) {
        super(application);

    }

    public static void clearDatabase() {
        emailRepository.clearDatabase();

    }

    public static boolean isNetworkAvailable(){
        return emailRepository.isNetworkAvailable();
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

    //Starts server in emailRepo and passes view so that it can be updated
    public void startServer(String userEmail, String userPassword) {
        emailRepository.startServer(userEmail, userPassword, view);
    }

//    public void shutdownRelayer(){
//        emailRepository.stopGmail();
//    }

    //init initializes the view in EmailRepo
    public void init(String userEmail, String userPassword, Button button) {
        emailRepository = new EmailRepository(getApplication().getApplicationContext(), userEmail, userPassword);
        emailRepository.init(view);


    }
    public void getAllUids(){
        emailRepository.getAllUids();
    }



}

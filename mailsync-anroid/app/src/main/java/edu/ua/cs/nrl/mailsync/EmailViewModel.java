package edu.ua.cs.nrl.mailsync;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.concurrent.ScheduledExecutorService;

import butterknife.Unbinder;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.relayer.Relayer;

public class EmailViewModel extends AndroidViewModel {
        private MutableLiveData<String> email;
        private MutableLiveData<String> password;
        private EmailRepository emailRepository;
        private boolean networkStatus;


        private boolean isFirstTime = true;

        public EmailViewModel(@NonNull Application application) {
                super(application);
                this.emailRepository=new EmailRepository(getApplication().getApplicationContext());
        }

//        public EmailViewModel(@NonNull Application application){
//                super(application);
//                this.emailRepository= new EmailRepository();
//        }



        public MutableLiveData<String> getPassword() {
                if(password==null){
                        password=new MutableLiveData<>();
                }
                return password;
        }

        public MutableLiveData<String> getEmail() {
                if(email==null){
                        email=new MutableLiveData<>();
                }
                return email ;
        }

        public void setNetworkStatus(boolean networkStatus) {
                this.networkStatus = networkStatus;
        }

        public boolean getNetworkStatus() {
                return networkStatus;
        }

        public void startServer(){

        }

        public void init(){

        }
        public boolean isNetworkAvailable() {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getApplication()
                        .getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }


}

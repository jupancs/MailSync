package edu.ua.cs.nrl.mailsync;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class EmailViewModel extends ViewModel {
        private MutableLiveData<String> email;
        private MutableLiveData<String> password;



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
}

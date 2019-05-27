package edu.ua.cs.nrl.mailsync;

import android.arch.lifecycle.ViewModel;

public class EmailViewModel extends ViewModel {
        private String email;
        private String password;

        public void setPassword(String password) {
                this.password = password;
        }

        public String getPassword() {
                return password.trim();
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public String getEmail() {
                return email ;
        }
}

package edu.ua.cs.nrl.mailsync.entities;

public class EmailAccount {

 private String email;
 private String password;

  public EmailAccount(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

}

package com.icegreen.greenmail;

import android.content.Context;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.imap.commands.CommandParser;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;

public class ExternalProxy {
  public static String userEmail;
  public static String userPassword;

  public static Message[] messages;

  public static GreenMail gmail = new GreenMail(ServerSetupTest.IMAP);
  public static CommandParser parser = new CommandParser();
  public static NDNMailSyncOneThread ndnMailSyncOneThread;
  private static int proxySelection = 1;

  public static Object monitor = new Object();

  public static Context context;

  public static int getSelectedProxy() {
    if (proxySelection == 0) {
      System.out.println("proxySelection = 0; select the Internet Service");
      return 0;
    } else if (proxySelection == 1) {
      System.out.println("proxySelection = 1; select the NDN Service");
      return 1;
    } else if (proxySelection == 2) {
      System.out.println("proxySelection = 2; " +
          "no network service selected, use local service");
      return 2;
    }
    return -1;
  }

  public static void setSelectedProxy(int i) {
    proxySelection = i;
  }

  public static boolean getNDNResult() {
    return ndnMailSyncOneThread.result;
  }

  public static void setNDNResult(boolean x) {
    ndnMailSyncOneThread.result = x;
    if (x) {
      synchronized(monitor) {
        monitor.notifyAll();
      }
    }
  }

  /**
   * Get the Interest or Data content.
   *
   * @return Interest or Data content
   */
  public static String getContentString() {
    return ndnMailSyncOneThread.getContentString();
  }

  public static void expressInterest(String nameUri) {
    ndnMailSyncOneThread.expressInterest(nameUri);
    System.out.println("Express Interest using external proxy");
  }

  /**
   * Store the MimeMessage from NDN service into Greenmail storage
   *
   * @param bais
   * @throws Exception
   */
  public static void storeInGreenStorageNDN(ByteArrayInputStream bais)
      throws Exception {
    ImapServer imapServer = ExternalProxy.gmail.getImap();
    Managers manager = ExternalProxy.gmail.getManagers();
    ImapHostManager imapHostManager = manager.getImapHostManager();
    UserManager userManager = manager.getUserManager();
    Flags testFlags = new Flags();

    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imaps");

    // create properties field
    Session session = Session.getInstance(props, null);

    // create the POP3 store object and connect with the pop server
    Store store = session.getStore("imaps");
    store.connect("imap.gmail.com", userEmail, userPassword);

    // create the folder object and open it
    Folder emailFolder = store.getFolder("INBOX");
    emailFolder.open(Folder.READ_WRITE);

    // retrieve the messages from the folder in an array and print it
    messages = emailFolder.getMessages();

    System.out.println("messages.length---" + messages.length);

    MimeMessage message = new MimeMessage(session, bais);

    imapServer.getServerSetup();
    System.out.println("This is manager: " + imapHostManager);
    imapHostManager.getInbox(userManager
        .getUserByEmail(userEmail))
        .appendMessage(message, testFlags, null);
  }

  public static void setUser(String email, String password) {
    userEmail = email;
    userPassword = password;
    gmail.setUser(email, email, password);
  }

  public static void main(String argv[]) {
    setUser(userEmail, userPassword);
    setSelectedProxy(proxySelection);
    gmail.start();
    ndnMailSyncOneThread.start();
    System.out.println("Finish starting Server. :)");
  }
}
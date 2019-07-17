package com.icegreen.greenmail;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;

import javax.mail.*;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.imap.commands.CommandParser;
import com.icegreen.greenmail.imap.ImapSession;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.ndnproxy.EmailFactory;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncOneThread;
import com.icegreen.greenmail.ipproxy.IpMailService;
import net.named_data.jndn.encoding.EncodingException;

class Email {
  public String from;
  public String to;
  public String subject;
  public String msg;
  public Date receive;
  public Date send;

  public Email(String from, String to, String subject,
               String msg, Date receive, Date send) {
    this.from = from;
    this.to = to;
    this.subject = subject;
    this.msg = msg;
    this.receive = receive;
    this.send = send;
  }
}

public class ExternalProxy extends Observable {
  public static Message[] messages;
  public static List<MimeMessage> messagesList = new ArrayList<>();

  public static GreenMail gmail = new GreenMail(ServerSetupTest.IMAP);
  private static IpMailService ipmail = new IpMailService();
  public static CommandParser parser = new CommandParser();
  private static LinkedList<Email> emailBuffer;
  public static LinkedList<MimeMessage> mimeMessageBuffer;
  public static NDNMailSyncOneThread ndnMailSyncOneThread = new NDNMailSyncOneThread();
  public static int proxySelection = 1;
  public static List<MimeMessage> appendList = new ArrayList<>();

  public static Object monitor = new Object();

  public static String userEmail;
  public static String userPassword;

  public static boolean relayerStop = false;
  private static boolean lastNetState = false;
  public static boolean hasNetBefore = false;

  public static Relayer relayer;

  public static int mailboxSize;

  static ImapServer imapServer = ExternalProxy.gmail.getImap();
  static Managers manager = ExternalProxy.gmail.getManagers();
  static ImapHostManager imapHostManager = manager.getImapHostManager();
  static UserManager userManager = manager.getUserManager();
  static Flags testFlags = new Flags();
  static Properties props = new Properties();
  public static Session session = Session.getInstance(props, null);
  public static int retransmissionMax = 0;

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

  public static void setRetransmissionMax(int x){
    if(retransmissionMax >= 0){
      retransmissionMax = x;
    }
  }

  public static String getContentString() {
    return ndnMailSyncOneThread.getContentString();
  }

  public static void expressInterest(String nameUri) {
    ndnMailSyncOneThread.expressInterest(nameUri);
//    System.out.println("Express Interest using external proxy");
  }

  public static void IpMailServiceLogin(ImapSession session, String userid,
                                        String password) {
    proxySelection = 1;
    System.out.println("IP mail service login");
    try {
      ipmail.login("imap.gmail.com", userid, password);
      ipmail.getMessages();
    } catch (Exception mex) {
      mex.printStackTrace();
    }
  }

  public static LinkedList<Email> readGmail(String currentEmail) {
    LinkedList<Email> result = new LinkedList<Email>();
    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imaps");

    try {
      Session session = Session.getInstance(props, null);
      Store store = session.getStore();
      store.connect("imap.gmail.com", 993, userEmail, userPassword);
      Folder inbox = store.getFolder("INBOX");
      inbox.open(Folder.READ_ONLY);
      Message[] msg = inbox.getMessages();
      Email temp;
      for (int i = 0; i < msg.length; i++) {
        temp = new Email("", "", "", "", null, null);
        Address[] in = msg[i].getFrom();
        for (Address address : in) {
          temp = new Email(address.toString(), currentEmail, "", "", null, null);
        }

        temp.subject = msg[i].getSubject();
        if (msg[i].isMimeType("text/plain")) {
          temp.msg = msg[i].getContent().toString();
        } else if (msg[i].isMimeType("multipart/*")) {
          System.out.println("message body is multipart");
          temp.msg = "Errors";
        }
        //temp.msg = msg[i].getContent().toString();
        temp.receive = msg[i].getReceivedDate();
        temp.send = msg[i].getSentDate();
        result.add(temp);
      }

    } catch (Exception mex) {
      mex.printStackTrace();
    }
    return result;
  }

  public static void updateEmails() throws Exception {
    ImapServer imapS = gmail.getImap();
    Managers manager = gmail.getManagers();
    ImapHostManager imapM = manager.getImapHostManager();
    UserManager userM = manager.getUserManager();

    Flags testFlags = new Flags();

    int updateCount = 0;
    LinkedList<Email> fetch = readGmail(userEmail);

    for (int j = fetch.size() - 1; j >= 0; j--) {
      if (emailBuffer.size() == 0) {
        System.out.println("before update, there's no email in the cache");
        for (Email i : fetch) {
          MimeMessage testMessage =
              GreenMailUtil.createTextEmail(i.to, i.from, i.subject, i.msg, imapS.getServerSetup());
          imapM.getInbox(userM.getUserByEmail(userEmail))
              .appendMessage(testMessage, testFlags, null);
        }
        return;
      } else {
        if (fetch.get(j).subject.equals(emailBuffer.getLast().subject)
            && fetch.get(j).from.equals(emailBuffer.getLast().from)
            && fetch.get(j).to.equals(emailBuffer.getLast().to)
            && fetch.get(j).send.equals(emailBuffer.getLast().send)
            && fetch.get(j).receive.equals(emailBuffer.getLast().receive)) {
          break;
        } else {
          updateCount++;
        }
      }
    }

    for (int i = updateCount; i > 0; i--) {
      emailBuffer.add(fetch.get(fetch.size() - i));
      MimeMessage testMessage = GreenMailUtil.createTextEmail(
          fetch.get(fetch.size() - i).to,
          fetch.get(fetch.size() - i).from,
          fetch.get(fetch.size() - i).subject,
          fetch.get(fetch.size() - i).msg,
          imapS.getServerSetup());
      imapM.getInbox(
          userM.
          getUserByEmail(userEmail)).
          appendMessage(testMessage, testFlags, null);
    }
  }

  // TODO: 8/27/18 Download and storage emails into GreenMail storage
  // TODO: 8/27/18 (be able to automatically fetch new emails)


  // TODO: 8/27/18 If the Internet is available, Thunderbird directly use mailSync-laptop storeage

  // TODO: 8/27/18 If the Internet is NOT available, use NDN service to sync only new emails
  /**
   * Download emails from Gmail server and feed them into GreenMail storage.
   *
   * @param user
   * @param password
   */
//  public void fetchFromInternet(String user, String password) {
//
//    try {
//      Properties props = new Properties();
//      props.setProperty("mail.store.protocol", "imaps");
//      props.setProperty("mail.imap.host", "imap.gmail.com");
//
//      // create properties field
//      Session session = Session.getInstance(props, null);
//
//      // create the IMAP store object and connect with the pop server
//      Store store = session.getStore("imaps");
//
//      try {
//        store.connect("imap.gmail.com", user, password);
//      } catch (AuthenticationFailedException e) {
//        System.out.println("Login Failed: " + e.getMessage());
//
//      }
//
//      // create the folder object and open it
//      Folder emailFolder = store.getFolder("INBOX");
//      emailFolder.open(Folder.READ_WRITE);
//
//      long downloadStart = System.nanoTime();
//      messages = emailFolder.getMessages();
//      long downloadEnd = System.nanoTime();
//      System.out.println(">>> Download time cost: " + (double) (downloadEnd - downloadStart) / 1000000000.0);
//      System.out.println("======================================");
//
//      System.out.println("messages.length---" + messages.length);
//      int msgSize = messages.length;
//
//      ImapServer imapS = ExternalProxy.gmail.getImap();
//      Managers manager = ExternalProxy.gmail.getManagers();
//      ImapHostManager imapM = manager.getImapHostManager();
//      UserManager userManager = manager.getUserManager();
//      Flags testFlags = new Flags();
//
//      imapS.getServerSetup();
//      MailFolder mailFolder = imapM.getInbox(userManager.getUserByEmail(userEmail));
//      for (int i = msgSize - 1; i >= 0; i--) {
//
//        Properties props2 = new Properties();
//        props2.setProperty("mail.store.protocol", "imaps");
//
//        // create properties field
//        Session session2 = Session.getInstance(props2, null);
//
//        // create the IMAP store object and connect with the pop server
//        Store store2 = session2.getStore("imaps");
//
//        try {
//          store2.connect("imap.gmail.com", user, password);
//        } catch (AuthenticationFailedException e) {
//          System.out.println("Login Failed: " + e.getMessage());
//        }
//
//        // create the folder object and open it
//        Folder folder = store2.getFolder("INBOX");
//        folder.open(Folder.READ_WRITE);
//
//        int mailboxSize = folder.getMessageCount();
//
//        if (msgSize < mailboxSize) {
//          for (int j = mailboxSize - 1; j >= msgSize; j--) {
//            MimeMessage mimeMessage = (MimeMessage) messages[j];
//
////            ndnDBConnection.saveNDNData(mimeMessage.getMessageID(), "".getBytes(), "MessageID");
////            System.out.println("Saved Email[" +
////                (new Database("MessageID", ndnDBConnection.getConfig()).getCount()) + "]");
////
////            System.out.println("This is manager: " + imapM);
////            mailFolder.appendMessageNDN(mimeMessage, testFlags, null,
////                getActivity().getApplicationContext());
//          }
//          msgSize = mailboxSize;
//          i++;
//        } else {
//          MimeMessage mimeMessage = (MimeMessage) messages[i];
//
////          String messageID = mimeMessage.getMessageID();
//
//          // Check if database already has this message
////          Query query = QueryBuilder
////              .select(SelectResult.property("name"))
////              .from(DataSource.database(new Database("MessageID", ndnDBConnection.getConfig())))
////              .where(Expression.property("name").equalTo(Expression.string(messageID)));
////
////          ResultSet resultSet = query.execute();
////          if (resultSet.allResults().size() == 0) {
////            ndnDBConnection.saveNDNData(mimeMessage.getMessageID(), "".getBytes(), "MessageID");
////            System.out.println("Saved Email[" +
////                (new Database("MessageID", ndnDBConnection.getConfig()).getCount()) + "]");
////
////            System.out.println("This is manager: " + imapM);
////            mailFolder.appendMessageNDN(mimeMessage, testFlags, null,
////                getActivity().getApplicationContext());
//          }
//        }
//
//        store2.close();
//
//      }
//    } catch (NoSuchProviderException e) {
//      e.printStackTrace();
//    } catch (MessagingException e) {
//      e.printStackTrace();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

//  public static void fetchFromInternet(String user, String password) {
//    Properties props = new Properties();
//    props.setProperty("mail.store.protocol", "imaps");
//
//    try {
//      // create properties field
//      Session session = Session.getInstance(props, null);
//
//      // create the POP3 store object and connect with the pop server
//      Store store = session.getStore("imaps");
//      store.connect("imap.gmail.com", user, password);
//
//      // create the folder object and open it
//      Folder emailFolder = store.getFolder("INBOX");
//      emailFolder.open(Folder.READ_WRITE);
//
//      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//
//      // retrieve the messages from the folder in an array and print it
//      messages = emailFolder.getMessages();
//
//      System.out.println("messages.length---" + messages.length);
//
//
//
//      for (int i = 0; i < messages.length; i++) {
//        MimeMessage curMessage = (MimeMessage) messages[i];
//        messagesList.add(curMessage);
//      }
//
//      // Store the email into Greenmail storage
//      storeInGreenStorage(userEmail);
//
//      // close the store and folder objects
//      emailFolder.close(false);
//      store.close();
//
//    } catch (NoSuchProviderException e) {
//      e.printStackTrace();
//    } catch (MessagingException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

  /**
   * Store the MimeMessage from Gmail server into Greenmail storage
   *
   * @param userEmail
   * @throws Exception
   */
  private static void storeInGreenStorage(String userEmail)
      throws Exception {
    ImapServer imapServer = ExternalProxy.gmail.getImap();
    Managers manager = ExternalProxy.gmail.getManagers();
    ImapHostManager imapHostManager = manager.getImapHostManager();
    UserManager userManager = manager.getUserManager();
    Flags testFlags = new Flags();

    // Using Gmail sever
    for (MimeMessage message : messagesList) {
      imapServer.getServerSetup();
      System.out.println("This is manager: " + imapHostManager);
      imapHostManager.getInbox(userManager
          .getUserByEmail(userEmail))
          .appendMessage(message, testFlags, null);
    }
  }

  /**
   * Store the MimeMessage from NDN service into Greenmail storage
   *
   * @param bais
   * @throws Exception
   */

  public static void storeInGreenStorageNDN(ByteArrayInputStream bais)
      throws Exception {
    MimeMessage message = new MimeMessage(session, bais);

    imapServer.getServerSetup();
    System.out.println("This is manager: " + imapHostManager);
    imapHostManager.getInbox(userManager
        .getUserByEmail(userEmail))
        .appendMessage(message, testFlags, null);
  }

  public static void setUser() {
    props.setProperty("mail.store.protocol", "imaps");
    gmail.setUser(userEmail, userEmail, userPassword);
  }

  public static void main(String argv[]) throws Exception {
    Scanner scanner = new Scanner(System.in);

    // User input
    System.out.println("Enter your Google account: ");
    // userEmail = scanner.next();
    userEmail = "mailtestm72@gmail.com";
    System.out.println("You email account is ===> " + userEmail);

    System.out.println("Enter your password: ");
    // userPassword = scanner.next();
    userPassword = "Abcdef12";
    System.out.println("You password is ===> " + "*******");

    final ObservableValue ov = new ObservableValue(lastNetState);
    BooleanObserver bo = new BooleanObserver(ov);
    ov.addObserver(bo);

    lastNetState = !netIsAvailable();

    // Check Internet availability
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          boolean currentNetState = netIsAvailable();
          if (lastNetState != currentNetState && lastNetState) {
            relayerStop = true;
            new Thread(new Runnable() {
              @Override
              public void run() {
                ov.setValue(false);
              }
            }).start();
            System.out.println("HIHIHIHIH");
          } else if (lastNetState != currentNetState && !lastNetState) {
            relayerStop = false;
            ov.setValue(true);
          }
          lastNetState = currentNetState;

          try {
            Thread.sleep(300);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();

    System.out.println("Finish starting Server. :)");
  }

  private static boolean netIsAvailable() {
    try {
      final URL url = new URL("http://www.google.com");
      final URLConnection conn = url.openConnection();
      conn.connect();
      conn.getInputStream().close();
      return true;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      return false;
    }
  }
}

class ObservableValue extends Observable {
  private boolean val;
  public ObservableValue(boolean val) {
    this.val = val;
  }
  public void setValue(boolean val) {
    this.val = val;
    setChanged();
    notifyObservers();
  }
  public boolean getValue() {
    return val;
  }
}

class BooleanObserver implements Observer {
  private ObservableValue ov = null;
  RegisterRoute registerRoute = new RegisterRoute();

  public BooleanObserver(ObservableValue ov)
  {
    this.ov = ov;
  }

  public void update(Observable obs, Object obj) {
    if (obs == ov) {
      if (!ov.getValue()) {
        try {
          registerRoute.run();
          // Wait for route to be created
          Thread.sleep(5000);
        } catch (EncodingException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        System.out.println("I am using greenmaillllllll");
        ExternalProxy.setUser();
        ExternalProxy.setSelectedProxy(ExternalProxy.proxySelection);
        ExternalProxy.gmail.start();
        new Thread(new Runnable() {
          public void run() {
            try {
              EmailFactory.start();
            } catch (IOException e) {
              e.printStackTrace();
            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            }
          }
        }).start();
        ExternalProxy.ndnMailSyncOneThread.start();
      } else {
        try {
          registerRoute.run();
        } catch (EncodingException e) {
          e.printStackTrace();
        }

        System.out.println("I am using RelayerRelayerRelayerRelayer!");
        ExternalProxy.hasNetBefore = true;
        ExternalProxy.gmail.stop();

        new Thread(new Runnable() {
          @Override
          public void run() {
            while (true) {
              Properties props = new Properties();
              props.setProperty("mail.store.protocol", "imaps");
              props.setProperty("mail.imap.host", "imap.gmail.com");

              // create properties field
              Session session = Session.getInstance(props, null);

              // create the IMAP store object and connect with the pop server
              Store store = null;

              try {
                store = session.getStore("imaps");
                store.connect("imap.gmail.com", ExternalProxy.userEmail, ExternalProxy.userPassword);
                // create the folder object and open it
                Folder emailFolder = store.getFolder("INBOX");
                emailFolder.open(Folder.READ_WRITE);

//                ExternalProxy.mailboxSize = emailFolder.getMessageCount();

//                System.out.println("Size::::::::: " + ExternalProxy.mailboxSize);

              } catch (AuthenticationFailedException e) {
                System.out.println("Login Failed: " + e.getMessage());
              } catch (MessagingException e) {
                e.printStackTrace();
              }

              try {
                store.close();
                Thread.sleep(300);
              } catch (InterruptedException e) {
                e.printStackTrace();
              } catch (MessagingException e) {
                e.printStackTrace();
              }
            }
          }
        }).start();

        new Thread(new Runnable() {
          @Override
          public void run() {
            Relayer relayer = new Relayer(3143);
            relayer.run();
          }
        }).start();
      }
    }
  }
}

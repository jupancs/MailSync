package com.icegreen.greenmail.ndnproxy;

import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.store.SimpleMessageAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class EmailFactory {
  public static void start() throws IOException, ClassNotFoundException {
    final int RETRANSMISSION_AMOUNT = 5;
   
    // Express NDN Interest
    /* ------------------------- MailFolder ------------------------- */
    // ExternalProxy.expressInterest(
    // "/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1"
    // );
    // ExternalProxy.setNDNResult(false);
    //
    // waitForReuslt();
    //
    // // Decode the serialized string to Snapshot
    // String contentString = ExternalProxy.getContentString();
    // String value = contentString;

    ExternalProxy.expressInterest("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/probe");
    ExternalProxy.setNDNResult(false);
    ExternalProxy.setRetransmissionMax(RETRANSMISSION_AMOUNT);
    waitForReuslt("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/probe");
    String contentString = ExternalProxy.getContentString();
    // System.out.println("Content string: " + contentString);
    int mailFolderSize = Integer.valueOf(contentString);
    int timeoutPeriod = 5;

    String value = "";
    for (int i = 0; i < mailFolderSize; i++) {
      ExternalProxy.expressInterest("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v" + i);
      ExternalProxy.setNDNResult(false);
      ExternalProxy.setRetransmissionMax(RETRANSMISSION_AMOUNT);
      waitForReuslt("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v" + i);
      value += ExternalProxy.getContentString();
    }

    System.out.println("======================================================");
    System.out.println("value size: " + value.length());
    System.out.println("======================================================");

    // ExternalProxy.expressInterest(
    // "/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v1"
    // );
    // ExternalProxy.setNDNResult(false);
    // waitForReuslt();
    // contentString = ExternalProxy.getContentString();
    // value = contentString;
    // ExternalProxy.expressInterest(
    // "/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v2"
    // );
    // ExternalProxy.setNDNResult(false);
    // waitForReuslt();
    // contentString = ExternalProxy.getContentString();
    // value += contentString;
    // System.out.println("======================================================");
    // System.out.println("value size: " + value.length());
    // System.out.println("======================================================");

    ObjectInputStream ois;
    if (!contentString.equals("")) {
      // System.out.println(">>> MailFolder content: " + value);
      byte[] decodeByteArray = BaseEncoding.base64().decode(value);
      // System.out.println(">>> Decoded Value " + decodeByteArray.toString());
      ByteArrayInputStream bais = new ByteArrayInputStream(decodeByteArray);
      ois = new ObjectInputStream(bais);
      Snapshot snapshot = (Snapshot) ois.readObject();
      // System.out.println(">>> SnapShot: " + snapshot);
      NDNMailSyncConsumerProducer.mailbox = snapshot;

      /* ------------------------- Attribute ------------------------- */

      int size = snapshot.size;
      System.out.println(">>>> Size >>>> " + size);
      System.out.println(">>> syncAmount: " + String.valueOf(NDNMailSyncConsumerProducer.mailbox.syncAmount));
      System.out.println(">>>Initial Size: " + snapshot.initSize);
      System.out.println(">>>startPos: " + snapshot.syncCheckpoint);
      int initSize = snapshot.initSize;
      int startPos = snapshot.syncCheckpoint;
      /*
       * The emailUIds are stored in increasing order that is UID 1,2,3 .... and every
       * single email will have its UID stored in messageUIDs Array List. MessageID
       * will contain the list of IDs of only emails that were stored in the mobile
       * side. initSize is used to track the initial size of laptop side mailbox and
       * so from that position syncing can occur for messageUIDs. startPos is used to
       * iterate for both the messageUID arraylist and messageIDs Arraylist as it
       * tracks how many of the stored emails are synced. It syncs from the startPos
       * to the startPos + syncAmount and so can sync any number of emails which are a
       * subset of the emails stored on the mobile side.
       */
      for (int i = startPos; i < NDNMailSyncConsumerProducer.mailbox.syncAmount + startPos; i++) {
        String messageID = snapshot.messageID.get(i);
        ExternalProxy.expressInterest("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/attribute/" + messageID);
        ExternalProxy.setRetransmissionMax(RETRANSMISSION_AMOUNT);
        ExternalProxy.setNDNResult(false);

        waitForReuslt("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/attribute/" + messageID);

        String contentStringAttribute = ExternalProxy.getContentString();
        String attributeValue = contentStringAttribute;
        ObjectInputStream attributeOIS;

        // Decode the serialized string to Attribute
        byte[] attributeDecodedByteArray = BaseEncoding.base64().decode(attributeValue);
        ByteArrayInputStream attributeBAIS = new ByteArrayInputStream(attributeDecodedByteArray);
        attributeOIS = new ObjectInputStream(attributeBAIS);
        SimpleMessageAttributes attribute = (SimpleMessageAttributes) attributeOIS.readObject();

        /* ------------------------- MimeMessage ------------------------- */

        // Deal with MimeMessage Interest
        int numberOfMessage = attribute.getSize() / 4000 + 1;
        StringBuilder messageBuilder = new StringBuilder();
        String mimeMessageContentString = null;
        for (int j = 0; j < numberOfMessage; j++) {
          ExternalProxy
              .expressInterest("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MimeMessage/" + messageID + "/v" + j);
          ExternalProxy.setNDNResult(false);
          ExternalProxy.setRetransmissionMax(RETRANSMISSION_AMOUNT);
          waitForReuslt("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MimeMessage/" + messageID + "/v" + j);

          mimeMessageContentString = ExternalProxy.getContentString();
          messageBuilder.append(mimeMessageContentString);
        }

        String valueMimeMessage = messageBuilder.toString();
        ObjectInputStream oisMimeMessage;

        byte[] decodeByteArrayMimeMessage = BaseEncoding.base64().decode(valueMimeMessage);
        ByteArrayInputStream baisMimeMessage = new ByteArrayInputStream(decodeByteArrayMimeMessage);

        try {
          MimeMessage message = new MimeMessage(ExternalProxy.session, baisMimeMessage);
          int uidSize = snapshot.messageUids.length;
          System.out.println("InitSize + i" + initSize + i + "uidSize" + uidSize);
          if (initSize + i < uidSize) {
            System.out.println(">>>>>> UID: " + snapshot.messageUids[initSize + i]);
            NdnFolder.uidToMime.put(snapshot.messageUids[initSize + i], message);
            NdnFolder.uidToAttr.put(snapshot.messageUids[initSize + i], attribute);
          }

        } catch (MessagingException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static void waitForReuslt(String interestName) {
    while (!ExternalProxy.getNDNResult()) {
      // while(ExternalProxy.retransmissionMax >= 0 && !ExternalProxy.getNDNResult()){
      //   retransmitInterest(interestName);
      //   ExternalProxy.retransmissionMax--;
      // }
      synchronized (ExternalProxy.monitor) {
        try {
          ExternalProxy.monitor.wait();
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
  /** 
   * Retransmits interest of the name specified and reduces the retransmission max of External Porxy
   * After the retransmission max is 0 there will not be any retransmission occuring
   * @param name Name of the interest to retransmit
  */
  synchronized public static void retransmitInterest(String name) {
    System.out.println("Retransmitting Interest" + name + ExternalProxy.retransmissionMax);
    ExternalProxy.expressInterest(name);
    ExternalProxy.retransmissionMax--;
    // try {
    //   // Waiting for response for the interest
    //   Thread.sleep(5000);
    // } catch (InterruptedException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
  }

}

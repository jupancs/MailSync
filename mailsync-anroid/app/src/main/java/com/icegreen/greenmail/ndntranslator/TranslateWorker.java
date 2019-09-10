package com.icegreen.greenmail.ndntranslator;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.SimpleMessageAttributes;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import edu.ua.cs.nrl.mailsync.EmailRepository;

public class TranslateWorker {

    public static String probeSize;
    public final static String TAG = "TranslateWorker";

    public static void start(MimeMessage mimeMessage, Context context, long uid, String mimeMessageID) throws
            FolderException, IOException, CouchbaseLiteException, MessagingException {
        // Initialize IMAP-to-NDN translators
        EmailRepository emailRepository = new EmailRepository();
        NdnTranslator ndnTranslator = TranslatorFactory.getNdnTranslator("IMAP", context);

//        DatabaseConfiguration config = new DatabaseConfiguration(context);
//        Database database = new Database("Probe", config);
//
//        NdnDBConnection ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
//                "couchbaseLite",
//                context
//        );

        KeyChain keyChain = ExternalProxy.ndnMailSyncOneThread.keyChain_;
        Name certificateName = ExternalProxy.ndnMailSyncOneThread.certificateName_;




        /**
         * Deal with MailFolder
         *
         */
//    StringBuilder nameBuilder1 = new StringBuilder();
//    nameBuilder1.append("/").append("mailSync").append("/").append(ExternalProxy.userEmail).append("/")
//        .append("v1").append("/inbox").append("/").append("1").append("/MimeMessage")
//        .append("/").append(String.valueOf(mimeMessage.getMessageID()));
//    StringBuilder nameBuilder2 = new StringBuilder();
//    nameBuilder2.append("/").append("mailSync").append("/").append(ExternalProxy.userEmail).append("/")
//        .append("v2").append("/inbox").append("/").append("1").append("/MimeMessage")
//        .append("/").append(String.valueOf(mimeMessage.getMessageID()));
//
//    String mailFolderName1 = nameBuilder1.toString();
//    String mailFolderName2 = nameBuilder2.toString();
//
//    Name mailNdnName1 = new Name(mailFolderName1);
//    Name mailNdnName2 = new Name(mailFolderName2);
//
//    String newMailFolderName1 = mailNdnName1.toUri();
//    String newMailFolderName2 = mailNdnName2.toUri();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        byte[] byteArray;
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println(" Ding Ding");
        Snapshot snapshot = NdnFolder.getSnapshot();
        oos.writeObject(snapshot);
//        Log.d(TAG,"Exists" + snapshot.exists + "Recent Count" + snapshot.recent + "UidValidity" +snapshot.uidvalidity + "Uidnext"
//                +snapshot.uidnext + "\n" + "First unseen" + snapshot.unseen + "Size" +snapshot.size + "Last Size" + NdnFolder.lastSize
//                + "Sync number" + snapshot.syncAmount + "Sync Checkpoint" +snapshot.syncCheckpoint + "InitSize" +snapshot.initSize + "MessageID" +  mimeMessageID);
//        Log.d(TAG,"MessageUID List");
        for(Long num: NdnFolder.messageUidList){
            Log.d(TAG, " " + num);
        }
//        Log.d(TAG,"MessageID List");
//        for(String string: NdnFolder.messgeID){
//            Log.d(TAG, " " + string);
//        }
        System.out.println("messageUids size: : : : : " + NdnFolder.messageUidList.size());
        System.out.println(" Da Da");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^");
        oos.flush();
        byteArray = baos.toByteArray();
        String contentString = BaseEncoding.base64().encode(byteArray);

//    Name mailFolderNdnName1 = new Name("/mailSync/"+ ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v1");
//    Name mailFolderNdnName2 = new Name("/mailSync/"+ ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v2");
        int mailFolderLen = contentString.length();

//    String part1 = contentString.substring(0, len / 2);
//    String part2 = contentString.substring(len / 2);
//
//    Data data1 = new Data(mailFolderNdnName1);
//    Data data2 = new Data(mailFolderNdnName2);
//
//    data1.setContent(new Blob(part1));
//    data2.setContent(new Blob(part2));
//
//    try {
//      keyChain.sign(data1, certificateName);
//      keyChain.sign(data2, certificateName);
//    } catch (SecurityException e) {
//      e.printStackTrace();
//    }
//
//    Blob encoding1 = data1.wireEncode();
//    byte[] result1 = encoding1.getImmutableArray();
//    Blob encoding2 = data2.wireEncode();
//    byte[] result2 = encoding2.getImmutableArray();
//
//    System.out.println("======================================================");
//    System.out.println("total size: " + len);
//    System.out.println("result1 size: " + result1.length);
//    System.out.println("result2 size: " + result2.length);
//    System.out.println("======================================================");
//
//    ndnTranslator.saveData(newMailFolderName1, result1, "MailFolder");
//    ndnTranslator.saveData(newMailFolderName2, result2, "MailFolder");

        /**
         * Deal with probe
         *
         */
        int mailFolderChunkSize = mailFolderLen / 7000 + 1;
        int mailFolderChunkLen = (int) Math.ceil(contentString.length() / (double) mailFolderChunkSize);
        probeSize = String.valueOf(mailFolderChunkSize);

        /**
         * Deal with MailFolder
         *
         */
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("/").append("mailSync").append("/").append(ExternalProxy.userEmail).append("/")
                .append("v1").append("/inbox").append("/").append("1").append("/MimeMessage")
                .append("/").append(String.valueOf(mimeMessageID));


        String[] mailFolderChunks = new String[mailFolderLen / 7000 + 1];
        for (int i = 0; i < mailFolderLen / 7000 + 1; i++) {
            String mailFolderName = "/mailSync/" + ExternalProxy.userEmail + "/v" + i + "/inbox/1/MailFolder/"
                    + String.valueOf(mimeMessageID);
            Name mailFolderNdnName = new Name(mailFolderName);
            mailFolderName = mailFolderNdnName.toUri();
            if (i == mailFolderChunkSize - 1) {
                mailFolderChunks[i] = contentString.substring(i * mailFolderChunkLen, contentString.length());
            } else {
                mailFolderChunks[i] = contentString.substring(i * mailFolderChunkLen, (i + 1) * mailFolderChunkLen);
            }
            mailFolderNdnName = new Name("/mailSync/" + ExternalProxy.userEmail + "/inbox/1/MailFolder/1/v" + i);
            Data data = new Data(mailFolderNdnName);
            data.setContent(new Blob(mailFolderChunks[i]));

            try {
                keyChain.sign(data, certificateName);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            // Get SingedBuffer
            Blob encoding = data.wireEncode();
            byte[] result = encoding.getImmutableArray();

            ndnTranslator.saveData(mailFolderName, result, "MailFolder");
            Log.d(TAG, "MailFolder Saved " + mailFolderName);
        }

        /**
         * Deal with MimeMessage
         *
         */
        Log.d(TAG,"Saving Mimemessage");
        String mimeMessageName = ndnTranslator.generateMimeMessageName(
                "mailSync",
                ExternalProxy.userEmail,
                "inbox",
                "1",
                String.valueOf(mimeMessageID)
        );

        Name mimeMessageNdnName = new Name(mimeMessageName);
        mimeMessageName = mimeMessageNdnName.toUri();

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
        byte[] byteArray2;

        mimeMessage.writeTo(baos2);

        oos2.flush();
        byteArray2 = baos2.toByteArray();
        contentString = BaseEncoding.base64().encode(byteArray2);

        // NDN packet upper bound is 8000
        int messageSize = contentString.length();
        int numberOfChunks = messageSize / 4000 + 1;
        int chunkLength = (int) Math.ceil(contentString.length() / (double) numberOfChunks);
        String[] chunks = new String[numberOfChunks];
        for (int i = 0; i < numberOfChunks; i++) {
            if (i == numberOfChunks - 1) {
                chunks[i] = contentString.substring(i * chunkLength, contentString.length());
            } else {
                chunks[i] = contentString.substring(i * chunkLength, (i + 1) * chunkLength);
            }

            Name name = new Name(mimeMessageName + "/v" + i);
            Data data = new Data(name);
            data.setContent(new Blob(chunks[i]));

            try {
                keyChain.sign(data, certificateName);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            // Get SingedBuffer
            Blob encoding = data.wireEncode();
            byte[] result = encoding.getImmutableArray();

            ndnTranslator.saveData(mimeMessageName + "/v" + i, result, "MimeMessage");
//            Log.d(TAG, "Mimemessage Saved " + mimeMessageName);

        }
        /**
         * Deal with attributes
         *
         */
        String attributeName = ndnTranslator.generateAttributeName(
                "mailSync",
                ExternalProxy.userEmail,
                "inbox",
                "1",
                String.valueOf(mimeMessageID)
        );
        System.out.println("Attribute Name " + attributeName);
        Name attributeNdnName = new Name(attributeName);
        attributeName = attributeNdnName.toUri();

        byte[] attributeData = ndnTranslator.encodeAttribute(
                new SimpleMessageAttributes(mimeMessage, mimeMessage.getReceivedDate(), contentString.length()), attributeNdnName
        );

        ndnTranslator.saveData(attributeName, attributeData, "Attribute");
        Log.d(TAG, "Attribute Saved " + attributeName);

//        System.out.println("Saved Email with UID: " + uid);
        emailRepository.getAllUids();
        emailRepository.notifyStorageCompletion();
        emailRepository.removeIncompleteUids(uid);
        EmailRepository.doneGettingFetched(uid);
        System.out.println("Translate Worker Ended");





    }
}

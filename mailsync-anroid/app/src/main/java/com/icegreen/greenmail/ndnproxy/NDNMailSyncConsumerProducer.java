package com.icegreen.greenmail.ndnproxy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndntranslator.TranslateWorker;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import edu.ua.cs.nrl.mailsync.EmailRepository;
import edu.ua.cs.nrl.mailsync.R;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;


public class NDNMailSyncConsumerProducer implements OnData, OnTimeout,
        OnInterestCallback, OnRegisterFailed {

    public int callbackCount_ = 0;
    public String contentString;
    public KeyChain keyChain_;
    public Name certificateName_;
    public int responseCount_ = 0;

    private double dataTime = 0.0;
    private double sendTime = 0.0;
    private double queryTime = 0.0;
    int count = 1;
    private View view;
    private static final String TAG = "NDNMCP";
    private static int i;
    private EmailRepository emailRepository;

    ProgressBar progressBar;


    public static Snapshot mailFolder;
    private Context context;

    public String probeSizeStr;

    public NDNMailSyncConsumerProducer(KeyChain keyChain, Name certificateName, Context context, View view) {
        keyChain_ = keyChain;
        certificateName_ = certificateName;
        this.context = context;
        this.view = view;
        progressBar = this.view.findViewById(R.id.download_bar);
        if (view == null) {
            Log.d(TAG, "View is null");
        }
        emailRepository = new EmailRepository();
    }

    public void
    onData(Interest interest, Data data) {
        ++callbackCount_;
        Name name = data.getName();
        String adu = name.get(4).toEscapedString();

        System.out.println("Got data packet with name: " + name.toUri());
        System.out.println("adu: " + adu);

        ByteBuffer content = data.getContent().buf();

        if (adu.equals("CAPABILITY")) {
            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
            contentString = charBuffer.toString();
            System.out.println(">>>>> Capability Data <<<<<");
        } else if (adu.equals("MailFolder")) {
            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
            contentString = charBuffer.toString();
            System.out.println(">>>>> MailFolder Data <<<<<" + contentString);
        } else if (adu.equals("attribute")) {
            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
            contentString = charBuffer.toString();
            System.out.println(">>>>> attribute Data <<<<<");
        } else if (adu.equals("MimeMessage")) {
            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
            contentString = charBuffer.toString();
            System.out.println(">>>>> MimeMessage Data <<<<<");
        }

        ExternalProxy.setNDNResult(true);
    }

    public void onTimeout(Interest interest) {
        ++callbackCount_;
        System.out.println("Time out for interest " + interest.getName().toUri());
        ExternalProxy.setNDNResult(true);
    }

    public String getContentString() {
        return contentString;
    }

    public void
    onInterest(Name prefix, Interest interest, Face face,
               long interestFilterId, InterestFilter filter) {
        ++responseCount_;
        System.out.println(Integer.toString(responseCount_) + " Interests received");

        Name name = interest.getName();
        String adu = name.get(4).toEscapedString();

        byte[] contentByte = null;

        NdnDBConnection ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
                "couchbaseLite",
                context
        );
        Data data = new Data(interest.getName());
        System.out.println("Interest recieved with name" + prefix.toString() + " with adu: " + adu + "interest " + interest.toUri());

        if (adu.equals("CAPABILITY")) {
            contentString = "IMAP4rev1 LITERAL+ SORT UIDPLUS";
            data.setContent(new Blob(contentString));

            try {
                keyChain_.sign(data, certificateName_);
            } catch (SecurityException exception) {
                // Don't expect this to happen.
                throw new Error("SecurityException in sign: " + exception.getMessage());
            }

            try {
                face.putData(data);
            } catch (IOException ex) {
                System.out.println("Echo: IOException in sending data " + ex.getMessage());
            }

        } else {
            long queryStartTime = System.nanoTime();
            if (adu.equals("probe")) {
                System.out.println("---------------------------------");
                System.out.println("ProbeSize: " + TranslateWorker.probeSize);
                if (TranslateWorker.probeSize == null) {
                    toast(context, "Cannot Sync any mails");
                }


                System.out.println("---------------------------------");
                probeSizeStr = String.valueOf(TranslateWorker.probeSize);
                try {
                    contentByte = ByteBuffer.wrap(probeSizeStr.getBytes("UTF-8")).array();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (adu.equals("MailFolder")) {
                try {
                    String nameUri = name.toString();
                    String[] arr = nameUri.replaceFirst("^/", "").split("/");

                    StringBuilder keyBuilder = new StringBuilder();
                    keyBuilder.append("/mailSync/").append(arr[1]).append("/").append(arr[6]);

                    System.out.println("arr[1] ==> " + keyBuilder.toString());
                    System.out.println("name ==> " + name.toUri());

                    Query mailFolderQuery = QueryBuilder
                            .select(SelectResult.property("content"))
                            .from(DataSource.database(new Database("MailFolder", ndnDBConnection.getConfig())))
                            .where(Expression.property("name").like(Expression.string(keyBuilder.toString() + "%")));
//          Query mailFolderQuery = QueryBuilder
//              .select(SelectResult.property("content"))
//              .from(DataSource.database(new Database("MailFolder", ndnDBConnection.getConfig())))
//              .where(Expression.property("name").equalTo(Expression.string(name.toUri())));

                    ResultSet mailFolderResult = mailFolderQuery.execute();
                    //The progressbar displaying the syncing has to have a max which will be the number of stored messages in the db
                    //Only those messages can be synced. EmailRepository has the number of stored messages which we get using getStoredMessages
                    //For each result update progress is called which increases the progress of the progress bar and gets the result
                    // to be sent
                    setMax(emailRepository.getStoredMessages());
                    for (Result result : mailFolderResult) {
                        System.out.println("*(*(*(*(*(*(*(*(*((*(");
                        contentByte = result.getBlob("content").getContent();
                        System.out.println("Content Byte is " + contentByte);
                    }

//          List<Result> list= mailFolderResult.allResults();

                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            } else if (adu.equals("attribute")) {
                try {
                    Query attributeQuery = QueryBuilder
                            .select(SelectResult.property("content"))
                            .from(DataSource.database(new Database("Attribute", ndnDBConnection.getConfig())))
                            .where(Expression.property("name").equalTo(Expression.string(name.toString())));
                    ResultSet attributeResult = attributeQuery.execute();

                    for (Result result : attributeResult) {
                        contentByte = result.getBlob("content").getContent();
                    }
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            } else if (adu.equals("MimeMessage")) {
                try {
                    Query mimeMessageQuery = QueryBuilder
                            .select(SelectResult.property("content"))
                            .from(DataSource.database(new Database("MimeMessage", ndnDBConnection.getConfig())))
                            .where(Expression.property("name").equalTo(Expression.string(name.toString())));
                    ResultSet mimeMessageResult = mimeMessageQuery.execute();

                    for (Result result : mimeMessageResult) {
                        contentByte = result.getBlob("content").getContent();
                        i++;
                    }
                    toast(context, "MailSync will sync" + i + "Emails");
                    //if i==syncnumber that means all the emails were synced and the sync number can be reset to 0 and the messageID list can cleared
                    // As both are already sent as mailfolder and were used correctly
                    System.out.println("Sync number is " + NdnFolder.syncNumber + "i = " + i);
                    if (i == NdnFolder.syncNumber) {
                        NdnFolder.syncNumber = 0;
                        NdnFolder.messgeID.clear();
                    }
                    System.out.println("***********************************");
                    System.out.println("content size: " + contentByte.length);
                    System.out.println("***********************************");
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
            long queryEndTime = System.nanoTime();
            queryTime += (queryEndTime - queryStartTime);
            System.out.println(">>> Query ave cost: " + (queryTime / count) / 1000000000.0);
            System.out.println("======================================");

            // Send the data
            long sendStartTime = System.nanoTime();
            if (adu.equals("probe")) {
                System.out.println("***********************************");
                System.out.println("ProbeSize String: " + probeSizeStr);
                System.out.println("***********************************");
                Data probeData = new Data(name);
                probeData.setContent(new Blob(probeSizeStr));
                try {
                    keyChain_.sign(data, certificateName_);
                    face.putData(probeData);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    // Re-construct wireEncoding
                    Blob encoding = new Blob(contentByte, false);
                    System.out.println("Size of the packet is: " + encoding.size());
                    face.send(encoding);
                    updateProgress(i);
                } catch (IOException ex) {
                    System.out.println("Echo: IOException in sending data " + ex.getMessage());
                }
            }

            long sendEndTime = System.nanoTime();
            sendTime += (sendEndTime - sendStartTime);
            System.out.println(">>> Send ave cost: " + (sendTime / count++) / 1000000000.0);
            System.out.println("======================================");
        }

    }

    //Gives Toast about syncing set amount of messages...to be removed soon
    public void toast(final Context context, final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();

            }
        });
    }

    //Sets a max to the progressbar
    public void setMax(final int max) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                progressBar.setMax(max);

            }
        });
    }

    //updates progress of the progressbar and since many threads could be running
    //a lock mechanism is added using synchronized
    synchronized public void updateProgress(final int progress) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                progressBar.setProgress(progress);

            }
        });
    }

    public void
    onRegisterFailed(Name prefix) {
        ++responseCount_;
        System.out.println("Register failed for prefix " + prefix.toUri());
    }

}
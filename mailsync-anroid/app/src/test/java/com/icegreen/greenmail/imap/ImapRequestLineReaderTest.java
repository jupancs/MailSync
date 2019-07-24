package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.ndnproxy.NdnFolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import edu.ua.cs.nrl.mailsync.EmailRepository;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({android.text.TextUtils.class, NdnFolder.class, EmailRepository.class})
public class ImapRequestLineReaderTest {
    private ArrayList<Long> test;
    private ImapRequestLineReader imapRequestLineReader;
    private EmailRepository emailRepository = new EmailRepository();

    @Before
    public void init() {

        test = new ArrayList<>();
        InputStream in = Mockito.mock(InputStream.class);
        OutputStream out = Mockito.mock(OutputStream.class);
        imapRequestLineReader = new ImapRequestLineReader(in, out);
    }

    @After
    public void clear() {
        emailRepository.deleteStoredMessage();
        emailRepository.clearAllUids();
        test.clear();
    }

    @Test
    public void extractUids_Fetch_Line_Normal() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        test.add((long) 178);
        test.add((long) 177);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("178,177", "fetch");
        assertArrayEquals("Should extract 178 and 177 and add them to incompleteUid List",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Fetch_Line_Empty() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
//        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("", "fetch");
        assertArrayEquals("Should not add anything",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Fetch_Line_NonDig() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
//        test.add((long) 178);
//        test.add((long) 177);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("a", "fetch");
        assertArrayEquals("Should not add anything",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_Normal_Few_Uids() {
        PowerMockito.mockStatic(NdnFolder.class);
//        PowerMockito.mockStatic(EmailRepository.class);
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
//        test.add((long) 178);
//        test.add((long) 177);
        test.add((long) 190);
        emailRepository.addIncompleteUids(178);
        emailRepository.addIncompleteUids(177);
        emailRepository.addIncompleteUids(190);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("177", "delete");
        imapRequestLineReader.extractUids("178", "delete");
        assertArrayEquals("Should delete 177 and 178 but keep 190",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_Normal_All_Uids() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        PowerMockito.mockStatic(EmailRepository.class);
        PowerMockito.mockStatic(NdnFolder.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        emailRepository.addIncompleteUids(178);
        emailRepository.addIncompleteUids(177);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("178", "delete");
        imapRequestLineReader.extractUids("177", "delete");

        assertArrayEquals("Array should be empty as 178 and 177 are deleted",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_Normal_UID_Not_Present() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
        PowerMockito.mockStatic(NdnFolder.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        test.add((long) 190);
        emailRepository.addIncompleteUids(190);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("178", "delete");
        imapRequestLineReader.extractUids("177", "delete");
        assertArrayEquals("Should not delete anything and 190 should remain",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_Normal_One_UID_Present_Other_Not() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
        PowerMockito.mockStatic(NdnFolder.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        test.add((long) 190);
        emailRepository.addIncompleteUids(178);
        emailRepository.addIncompleteUids(190);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("178", "delete");
        imapRequestLineReader.extractUids("177", "delete");
        assertArrayEquals("Should remove 178 and keep 190",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_Empty() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        test.add((long) 178);
        test.add((long) 177);
        emailRepository.addIncompleteUids(178);
        emailRepository.addIncompleteUids(177);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("", "delete");
        assertArrayEquals("Should not delete anything",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void extractUids_Delete_Line_NonDig() {
        PowerMockito.mockStatic(android.text.TextUtils.class);
//        EmailRepository emailRepository = Mockito.mock(EmailRepository.class);
//        when(emailRepository.addIncompleteUids(100)).thenCallRealMethod();
        String str = "<FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\\r\\n>";
//        android.text.TextUtils mock = Mockito.mock(TextUtils.class);
//        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
//        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("178")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("177")).thenReturn(true);
        when(android.text.TextUtils.isDigitsOnly("")).thenReturn(false);
        when(android.text.TextUtils.isDigitsOnly("a")).thenReturn(false);
//        when(mock.isDigitsOnly(anyString())).thenReturn(true);
//        when(mock.isDigitsOnly("177")).thenReturn(true);
        test.add((long) 178);
        test.add((long) 177);
        emailRepository.addIncompleteUids(178);
        emailRepository.addIncompleteUids(177);
//        when(android.text.TextUtils.isDigitsOnly("178")).then(Mockito.mock(Boolean.class));
        imapRequestLineReader.extractUids("a", "fetch");
        assertArrayEquals("Should not delete anything",test.toArray(), EmailRepository.getIncompleteUids().toArray());
    }
}
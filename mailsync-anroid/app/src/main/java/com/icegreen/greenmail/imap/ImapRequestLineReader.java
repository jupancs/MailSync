/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.ndnproxy.NdnFolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import edu.ua.cs.nrl.mailsync.EmailRepository;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing lookahead=1
 * on the underlying character stream.
 * TODO need to look at encoding, and whether we should be wrapping an InputStream instead.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ImapRequestLineReader {
    private static final Logger log = LoggerFactory.getLogger(ImapRequestLineReader.class);
    private InputStream input;
    private OutputStream output;

    private boolean nextSeen = false;
    private char nextChar; // unknown
    private StringBuilder buf = new StringBuilder();
    private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r\n");
    private static EmailRepository emailRepository;

    ImapRequestLineReader(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Reads the next regular, non-space character in the current line. Spaces are skipped
     * over, but end-of-line characters will cause a {@link ProtocolException} to be thrown.
     * This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next non-space character.
     * @throws ProtocolException If the end-of-line or end-of-stream is reached.
     */
    public char nextWordChar() throws ProtocolException {
        char next = nextChar();
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        if (next == '\r' || next == '\n') {
            throw new ProtocolException("Missing argument.");
        }

        return next;
    }

    /**
     * Reads the next character in the current line. This method will continue to return
     * the same character until the {@link #consume()} method is called.
     *
     * @return The next character.
     * @throws ProtocolException If the end-of-stream is reached.
     */
    public char nextChar() throws ProtocolException {
        if (!nextSeen) {
            try {
                final int read = input.read();
                final char c = (char) read;
                buf.append(c);
                if(read == -1) {
                    dumpLine();
                    throw new ProtocolException("End of stream");
                }
                nextChar = c;
                nextSeen = true;
            } catch (IOException e) {
                throw new ProtocolException("Error reading from stream.", e);
            }
        }
        return nextChar;
    }

    public void dumpLine() {
        emailRepository=new EmailRepository();
        if(log.isDebugEnabled()) {
            // Replace carriage return to avoid confusing multiline log output
            log.debug("IMAP Line received : <" + CARRIAGE_RETURN.matcher(buf).replaceAll("\\\\r\\\\n")+'>');
        }
        System.out.println("IMAP Line received : <" + CARRIAGE_RETURN.matcher(buf).replaceAll("\\\\r\\\\n")+'>');
        //IMAP Line received : <FETCH 178,177 (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])\r\n>
        //The above line is a normal fetch command for new emails which will help us get the uids of the new emails
        //Gives the pos of the uids in the IMAP fetch command
        //COPY 456 "[Gmail]/Trash"\r\n
        int pos= buf.indexOf(" (UID FLAGS INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS (from reply-to to cc bcc content-type date message-id X-Android-Message-ID subject in-reply-to references)])");
        if(pos!=-1){
            System.out.println("New uids"+buf.substring(5,pos));
            boolean isFirstTime = EmailRepository.checkFirstTime();
            System.out.println("First Time " + isFirstTime);
            if(isFirstTime){
                extractUids(buf.substring(6,pos),"fetch");
            }

        }
        int delpos = buf.indexOf(" \"[Gmail]/Trash\"");
        if(delpos > 4){
            System.out.println("Delete uids"+buf.substring(5,delpos));
            extractUids(buf.substring(5,delpos),"delete");
        }
        System.out.println("Check 1 Before:");
        emailRepository.getAllUids();


        buf.delete(0, buf.length());
    }

    /**
     * Used to extract uids from the list of uids and it added to the InCompleteUIds list
     * Also helps identify the max amount of emails that can be stored signified by the size of
     * dig array
     * After fetching this function either adds it as needed to stored uids if the function is
     * fetch or else deletes the specified uid from the list if the function specified is delete
     * @param a String that is passed that contains the uid
     * @param function function that needs to be performed either fetch or delete
     */
    public void extractUids(String a, String function) {
        emailRepository = new EmailRepository();
        String s = new String (a);
        if(function.equals("fetch")){
            String[]dig=s.split(",");
            if(dig.length > 50){
                System.out.println("Cant store " + dig.length + " many emails");
                return;
            } else {
                System.out.println("This is the string to extract uid from " + a);
                int numOfEmails=0;
                for(String i: dig){
                    numOfEmails++;
                    if(android.text.TextUtils.isDigitsOnly(i)){
                        Long uid = Long.parseLong(i);
                        emailRepository.addIncompleteUids(uid);
                        System.out.println("This uid is added :" + uid);
                    }
                }
                EmailRepository.maxEmailsStored +=numOfEmails;
            }



        } else if (function.equals("delete")) {
            System.out.println("In the process of deleting " + a);
            if(android.text.TextUtils.isDigitsOnly(a)){
                System.out.println("In here");
                Long uid = Long.parseLong(a);
                NdnFolder.deleteUID(uid);
                emailRepository.removeIncompleteUids(uid);
                if(NdnFolder.lastSize>0){
                    NdnFolder.lastSize--;
                }

            }
        }

        System.out.println("Check 2 After:");
        emailRepository.getAllUids();


    }

    /**
     * Moves the request line reader to end of the line, checking that no non-space
     * character are found.
     *
     * @throws ProtocolException If more non-space tokens are found in this line,
     *                           or the end-of-file is reached.
     */
    public void eol() throws ProtocolException {
        char next = nextChar();

        // Ignore trailing spaces.
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        // handle DOS and unix end-of-lines
        if (next == '\r') {
            consume();
            next = nextChar();
        }

        // Check if we found extra characters.
        if (next != '\n') {
            throw new ProtocolException("Expected end-of-line, found more character(s): "+next);
        }
        dumpLine();
    }

    /**
     * Consumes the current character in the reader, so that subsequent calls to the request will
     * provide a new character. This method does *not* read the new character, or check if
     * such a character exists. If no current character has been seen, the method moves to
     * the next character, consumes it, and moves on to the subsequent one.
     *
     * @throws ProtocolException if a the current character can't be obtained (eg we're at
     *                           end-of-file).
     */
    public char consume() throws ProtocolException {
        char current = nextChar();
        nextSeen = false;
        nextChar = 0;
        return current;
    }


    /**
     * Reads and consumes a number of characters from the underlying reader,
     * filling the byte array provided.
     *
     * @param holder A byte array which will be filled with bytes read from the underlying reader.
     * @throws ProtocolException If a char can't be read into each array element.
     */
    public void read(byte[] holder) throws ProtocolException {
        int readTotal = 0;
        try {
            while (readTotal < holder.length) {
                int count = input.read(holder, readTotal, holder.length - readTotal);
                if (count == -1) {
                    throw new ProtocolException("Unexpected end of stream.");
                }
                readTotal += count;
            }
            // Unset the next char.
            nextSeen = false;
            nextChar = 0;
        } catch (IOException e) {
            throw new ProtocolException("Error reading from stream.", e);
        }
    }

    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
    public void commandContinuationRequest()
            throws ProtocolException {
        try {
            output.write('+');
            output.write('\r');
            output.write('\n');
            output.flush();
        } catch (IOException e) {
            throw new ProtocolException("Unexpected exception in sending command continuation request.", e);
        }
    }

    public void consumeLine()
            throws ProtocolException {
        char next = nextChar();
        while (next != '\n') {
            consume();
            next = nextChar();
        }
        consume();
    }
}

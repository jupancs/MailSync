package com.icegreen.greenmail.ipproxy;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class IpMailService {

	private Session session;
	private Store store;
	private Folder folder;

	// hardcoding protocol and the folder
	// it can be parameterized and enhanced as required
	private String protocol = "imaps";
	private String file = "INBOX";

	public IpMailService() {

	}

	public boolean isLoggedIn() {
		return store.isConnected();
	}

	/**
	 * to login to the mail host server
	 */
	public void login(String host, String username, String password)
			throws Exception {
		URLName url = new URLName(protocol, host, 993, file, username, password);
		
		System.out.println("in login(): ");

		if (session == null) {
			Properties props = null;
			try {
				props = System.getProperties();
			} catch (SecurityException sex) {
				props = new Properties();
			}
			session = Session.getInstance(props, null);
		}
		store = session.getStore(url);
		store.connect();
		folder = store.getFolder(url);

		folder.open(Folder.READ_WRITE);
	}
	
//	public void select(String file) 
//			throws Exception{
//		
//		
//	}

	/**
	 * to logout from the mail host server
	 */
	public void logout() throws MessagingException {
		folder.close(false);
		store.close();
		store = null;
		session = null;
	}

	public int getMessageCount() {
		int messageCount = 0;
		try {
			messageCount = folder.getMessageCount();
		} catch (MessagingException me) {
			me.printStackTrace();
		}
		return messageCount;
	}

	public Message[] getMessages() throws MessagingException {
		return folder.getMessages();
	}

}
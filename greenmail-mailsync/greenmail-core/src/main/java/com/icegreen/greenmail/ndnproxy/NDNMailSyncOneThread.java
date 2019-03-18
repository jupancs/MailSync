package com.icegreen.greenmail.ndnproxy;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.util.Blob;

public class NDNMailSyncOneThread {

  public Face face_;
  public KeyChain keyChain_;
  public Name keyName_;
  public Name certificateName_;
  public MemoryIdentityStorage identityStorage_;
  public MemoryPrivateKeyStorage privateKeyStorage_;
  public NDNMailSyncConsumerProducer cp;
  public static Boolean result = false;

  public NDNMailSyncOneThread() {
    try {
      face_ = new Face();
      identityStorage_ = new MemoryIdentityStorage();
      privateKeyStorage_ = new MemoryPrivateKeyStorage();
      keyChain_ = new KeyChain(new IdentityManager(identityStorage_, privateKeyStorage_),
          new SelfVerifyPolicyManager(identityStorage_));
      keyChain_.setFace(face_);

      keyName_ = new Name("/mailSync/DSK-123");

      certificateName_ = keyName_.getSubName(0, keyName_.size() - 1)
          .append("KEY")
          .append(keyName_.get(-1)).append("ID-CERT").append("0");
      identityStorage_.addKey
          (keyName_, KeyType.RSA, new Blob(DefaultKeys.DEFAULT_RSA_PUBLIC_KEY_DER, false));
      privateKeyStorage_.setKeyPairForKeyName(
          keyName_,
          KeyType.RSA,
          DefaultKeys.DEFAULT_RSA_PUBLIC_KEY_DER,
          DefaultKeys.DEFAULT_RSA_PRIVATE_KEY_DER);

      face_.setCommandSigningInfo(keyChain_, certificateName_);
    } catch (SecurityException e) {
      System.out.println("security exception");
    }

    cp = new NDNMailSyncConsumerProducer(keyChain_, certificateName_);

    Name prefix = new Name("/mailSync");
    System.out.println("Register prefix  " + prefix.toUri());
    try {
      face_.registerPrefix(prefix, cp, cp);
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public void expressInterest(String nameUri) {
    Name name = new Name(nameUri);
    try {
      face_.expressInterest(name, cp, cp);
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public String getContentString() {
    return cp.getContentString();
  }

  public void start() {
    try {
      while (true) {
        face_.processEvents();
        Thread.sleep(20);
      }
    } catch (Exception e) {
      System.out.println("exception: " + e.getMessage());
    }
  }
}

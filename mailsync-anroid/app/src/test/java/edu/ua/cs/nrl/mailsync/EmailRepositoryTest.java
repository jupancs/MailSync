package edu.ua.cs.nrl.mailsync;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class EmailRepositoryTest {
    EmailRepository emailRepository;
    @Before
    public void init(){
        emailRepository = new EmailRepository();
        emailRepository.deleteStoredMessage();

    }

    @Test
    public void getIsIncomplete() {
            emailRepository.addIncompleteUids(100);
            assertTrue("Uid added so IsIncomplete should be true",EmailRepository.getIsIncomplete());
            emailRepository.removeIncompleteUids(100);
            assertFalse("All uids removed so isIncomplete should be false",EmailRepository.getIsIncomplete());

    }
    @Test
    public void getIncompleteUids() {
        emailRepository.addIncompleteUids(100);
        emailRepository.addIncompleteUids(200);
        ArrayList<Long> myArray= new ArrayList<>();
        myArray.add((long)100);
        myArray.add((long)200);
        assertArrayEquals("Array should contain 100 and 200",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        Long l = (long)100;
        myArray.remove(l);
        l = (long)200;
        myArray.remove(l);
        emailRepository.removeIncompleteUids(100);
        emailRepository.removeIncompleteUids(200);
        assertArrayEquals("Array should be empty",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void removeIncompleteUids() {
        emailRepository.addIncompleteUids(100);
        emailRepository.addIncompleteUids(200);
        ArrayList<Long> myArray= new ArrayList<>();
        myArray.add((long)100);
        emailRepository.removeIncompleteUids(200);
        assertArrayEquals("Should remove 200 and keep 100",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        assertTrue("isIncomplete should be true",EmailRepository.isIncomplete);
        emailRepository.removeIncompleteUids(200);
        assertTrue("isIncomplete should be true",EmailRepository.isIncomplete);
        assertArrayEquals("Remove 200 which is already removed",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        emailRepository.removeIncompleteUids(100);
        assertFalse("Isincomplete should be false",EmailRepository.isIncomplete);
        myArray.remove((long)100);
        assertArrayEquals("Empty the incomple uid arraylist",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
    }


    @Test
    public void correctUID() {
        assertTrue("Greater than 0 uid should return true",EmailRepository.correctUID(100));
        assertFalse("uid id = 0 should return false",EmailRepository.correctUID(0));
        assertFalse("negative uid should return false",EmailRepository.correctUID(-1));

    }

    @Test
    public void addIncompleteUids() {
        emailRepository.addIncompleteUids(100);
        ArrayList<Long> myArray= new ArrayList<>();
        myArray.add((long)100);
        assertTrue("isIncomplete should be true",EmailRepository.isIncomplete);
        assertArrayEquals("100 is added to the uid list",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        emailRepository.addIncompleteUids(100);
        assertTrue("isIncomplete should be true",EmailRepository.isIncomplete);
        assertArrayEquals("Duplicate 100 is added to the uid list",myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        emailRepository.removeIncompleteUids(100);
    }

    @Test
    public void incrementStoredMessages() {
        emailRepository.incrementStoredMessages();
        assertEquals("Stored messages should be 1", 1, emailRepository.getStoredMessages());
    }

//    @Test
//    public void decrementStoredMessages() {
//        emailRepository.incrementStoredMessages();
//        emailRepository.decrementStoredMessages();
//        assertEquals("Stored messages should be 0", 0, emailRepository.getStoredMessages());
//        emailRepository.decrementStoredMessages();
//        assertEquals("Stored messages should remain at 0", 0, emailRepository.getStoredMessages());
//    }

//    @Test
//    public void deleteAllStoredMessage() {
//    }

    @Test
    public void getStoredMessages() {
        emailRepository.incrementStoredMessages();
        assertEquals("Stored messages should be 1", 1, emailRepository.getStoredMessages());
    }
}

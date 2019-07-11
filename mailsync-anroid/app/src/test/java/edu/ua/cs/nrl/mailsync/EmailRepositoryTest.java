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
        emailRepository.addIncompleteUids(100);
    }

    @Test
    public void getIsIncomplete() {
            assertTrue("Checking incomplete",EmailRepository.getIsIncomplete());
            emailRepository.removeIncompleteUids(100);
            assertTrue("Removed all data",!EmailRepository.getIsIncomplete());

    }
    @Test
    public void getIncompleteUids() {
        emailRepository.addIncompleteUids(100);
        emailRepository.addIncompleteUids(200);
        ArrayList<Long> myArray= new ArrayList<>();
        myArray.add((long)100);
        myArray.add((long)200);
        assertArrayEquals(myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        Long l = (long)100;
        myArray.remove(l);
        l = (long)200;
        myArray.remove(l);
        emailRepository.removeIncompleteUids(100);
        emailRepository.removeIncompleteUids(200);
        assertArrayEquals(myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
    }

    @Test
    public void removeIncompleteUids() {
        emailRepository.addIncompleteUids(100);
        emailRepository.addIncompleteUids(200);
        ArrayList<Long> myArray= new ArrayList<>();
        myArray.add((long)100);
        emailRepository.removeIncompleteUids(200);
        assertArrayEquals(myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        assertTrue(EmailRepository.isIncomplete);
        emailRepository.removeIncompleteUids(200);
        assertTrue(EmailRepository.isIncomplete);
        assertArrayEquals(myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
        emailRepository.removeIncompleteUids(100);
        assertTrue(!EmailRepository.isIncomplete);
        myArray.remove((long)100);
        assertArrayEquals(myArray.toArray(),EmailRepository.getIncompleteUids().toArray());
    }


    @Test
    public void correctUID() {
        assertTrue(EmailRepository.correctUID(100));
        assertTrue(!EmailRepository.correctUID(0));
        assertTrue(!EmailRepository.correctUID(-1));

    }

}

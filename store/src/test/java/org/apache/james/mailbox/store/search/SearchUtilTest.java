package org.apache.james.mailbox.store.search;

import static org.junit.Assert.*;

import org.junit.Test;

public class SearchUtilTest {

    @Test
    public void testSimpleSubject() {
        String subject ="This is my subject";
        assertEquals(subject, SearchUtil.getBaseSubject(subject));
    }
    
    @Test
    public void testReplaceSpacesAndTabsInSubject() {
        String subject ="This   is my\tsubject";
        assertEquals("This is my subject", SearchUtil.getBaseSubject(subject));
    }
    
    @Test
    public void testRemoveTrailingSpace() {
        String subject ="This is my subject ";
        assertEquals("This is my subject", SearchUtil.getBaseSubject(subject));
    }
    
    
    @Test
    public void testRemoveTrailingFwd() {
        String subject ="This is my subject (fwd)";
        assertEquals("This is my subject", SearchUtil.getBaseSubject(subject));
    }
    
    /*
    @Test
    public void testRemoveLeaders() {
        String subject ="[Blah blub] [go] re: This is my subject";
        assertEquals("This is my subject", SearchUtil.getBaseSubject(subject));
    }
    */
}

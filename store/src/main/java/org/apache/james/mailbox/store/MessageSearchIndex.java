package org.apache.james.mailbox.store;

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;

public interface MessageSearchIndex<Id> {

    /**
     * Add the {@link MailboxMembership} to the search index
     * 
     * @param mailbox
     * @param membership
     * @throws MailboxException
     */
    public void add(MailboxSession session, Mailbox<Id> mailbox, MailboxMembership<Id> membership) throws MailboxException;
    
    /**
     * Update the Flags in the search index for the given {@link MessageRange} 
     * 
     * @param mailbox
     * @param range
     * @param flags
     * @throws MailboxException
     */
    public void update(MailboxSession session, Mailbox<Id> mailbox, MessageRange range, Flags flags) throws MailboxException;
    
    /**
     * Delete the data for the given {@link MessageRange} from the search index
     * 
     * @param mailbox
     * @param range
     * @throws MailboxException
     */
    public void delete(MailboxSession session, Mailbox<Id> mailbox, MessageRange range) throws MailboxException;

    /**
     * Return all uids of the previous indexed {@link MailboxMembership}'s which match the {@link SearchQuery}
     * 
     * @param mailbox
     * @param searchQuery
     * @return
     * @throws MailboxException
     */
    public Iterator<Long> search(MailboxSession session, Mailbox<Id> mailbox, SearchQuery searchQuery) throws MailboxException;
}

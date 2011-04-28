/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mailbox.jpa.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.MessageRange.Type;
import org.apache.james.mailbox.jpa.JPATransactionalMapper;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAStreamingMessage;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.openjpa.persistence.ArgumentException;

/**
 * JPA implementation of a {@link MessageMapper}. This class is not thread-safe!
 */
public class JPAMessageMapper extends JPATransactionalMapper implements MessageMapper<Long> {
    
    public JPAMessageMapper(final EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findInMailbox(org.apache.james.mailbox.MessageRange)
     */
    public void findInMailbox(Mailbox<Long> mailbox, MessageRange set, MailboxMembershipCallback<Long> callback) throws MailboxException {
        try {
            List<Message<Long>> results;
            long from = set.getUidFrom();
            final long to = set.getUidTo();
            final int batchSize = set.getBatchSize();
            final Type type = set.getType();

            // when batch is specified fetch data in chunks and send back in
            // batches
            do {
                switch (type) {
                default:
                case ALL:
                    results = findMessagesInMailbox(mailbox, batchSize);
                    break;
                case FROM:
                    results = findMessagesInMailboxAfterUID(mailbox, from, batchSize);
                    break;
                case ONE:
                    results = findMessagesInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(mailbox, from, to, batchSize);
                    break;
                }

                if (results.size() > 0) {
                    callback.onMailboxMembers(results);

                    // move the start UID behind the last fetched message UID
                    from = results.get(results.size() - 1).getUid() + 1;
                }

            } while (results.size() > 0 && batchSize > 0);

        } catch (PersistenceException e) {
            throw new MailboxException("Search of MessageRange " + set + " failed in mailbox " + mailbox, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Message<Long>> findMessagesInMailboxAfterUID(Mailbox<Long> mailbox, long uid, int batchSize) {
        Query query = getEntityManager().createNamedQuery("findMessagesInMailboxAfterUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid);
        
        if(batchSize > 0)
        	query.setMaxResults(batchSize);
        
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Message<Long>> findMessagesInMailboxWithUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxWithUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Message<Long>> findMessagesInMailboxBetweenUIDs(Mailbox<Long> mailbox, long from, long to, int batchSize) {
        Query query = getEntityManager().createNamedQuery("findMessagesInMailboxBetweenUIDs").setParameter("idParam", mailbox.getMailboxId()).setParameter("fromParam", from).setParameter("toParam", to);

        if (batchSize > 0)
            query.setMaxResults(batchSize);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Message<Long>> findMessagesInMailbox(Mailbox<Long> mailbox, int batchSize) {
         Query query = getEntityManager().createNamedQuery("findMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId());
         if(batchSize > 0)
        	 query.setMaxResults(batchSize);
         return query.getResultList();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#expungeMarkedForDeletionInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<Long> expungeMarkedForDeletionInMailbox(Mailbox<Long> mailbox, final MessageRange set) throws MailboxException {
        try {
            final List<Long> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            switch (set.getType()) {
                case ONE:
                    results = findDeletedMessagesInMailboxWithUID(mailbox, from);
                    deleteDeletedMessagesInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    deleteDeletedMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(mailbox, from);
                    deleteDeletedMessagesInMailboxAfterUID(mailbox, from);
                    break;
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(mailbox);
                    deleteDeletedMessagesInMailbox(mailbox);
                    break;
            }
            
            return results.iterator();
        } catch (PersistenceException e) {
            throw new MailboxException("Search of MessageRange " + set + " failed in mailbox " + mailbox, e);
        }
    }


    private int deleteDeletedMessagesInMailbox(Mailbox<Long> mailbox) {
        return getEntityManager().createNamedQuery("deleteDeletedMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).executeUpdate();
    }

    private int deleteDeletedMessagesInMailboxAfterUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("deleteDeletedMessagesInMailboxAfterUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).executeUpdate();
    }

    private int deleteDeletedMessagesInMailboxWithUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("deleteDeletedMessagesInMailboxWithUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).setMaxResults(1).executeUpdate();
    }

    private int deleteDeletedMessagesInMailboxBetweenUIDs(Mailbox<Long> mailbox, long from, long to) {
        return getEntityManager().createNamedQuery("deleteDeletedMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("fromParam", from)
        .setParameter("toParam", to).executeUpdate();
    }

    
    @SuppressWarnings("unchecked")
    private List<Long> findDeletedMessagesInMailbox(Mailbox<Long> mailbox) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> findDeletedMessagesInMailboxAfterUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxAfterUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> findDeletedMessagesInMailboxWithUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxWithUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> findDeletedMessagesInMailboxBetweenUIDs(Mailbox<Long> mailbox, long from, long to) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        try {
            return (Long) getEntityManager().createNamedQuery("countMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getSingleResult();
        } catch (PersistenceException e) {
            throw new MailboxException("Count of messages failed in mailbox " + mailbox, e);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        try {
            return (Long) getEntityManager().createNamedQuery("countUnseenMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getSingleResult();
        } catch (PersistenceException e) {
            throw new MailboxException("Count of useen messages failed in mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<Long> mailbox, Message<Long> message) throws MailboxException {
        try {
            getEntityManager().remove(message);
        } catch (PersistenceException e) {
            throw new MailboxException("Delete of message " + message + " failed in mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    @SuppressWarnings("unchecked")
    public Long findFirstUnseenMessageUid(Mailbox<Long> mailbox)  throws MailboxException {
        try {
            Query query = getEntityManager().createNamedQuery("findUnseenMessagesInMailboxOrderByUid").setParameter("idParam", mailbox.getMailboxId());
            query.setMaxResults(1);
            List<Message<Long>> result = query.getResultList();
            if (result.isEmpty()) {
                return null;
            } else {
                return result.get(0).getUid();
            }
        } catch (PersistenceException e) {
            throw new MailboxException("Search of first unseen message failed in mailbox " + mailbox, e);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    @SuppressWarnings("unchecked")
    public List<Message<Long>> findRecentMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        try {
            Query query = getEntityManager().createNamedQuery("findRecentMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId());
            return query.getResultList();
        } catch (PersistenceException e) {
            throw new MailboxException("Search of recent messages failed in mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#add(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long add(Mailbox<Long> mailbox, Message<Long> message) throws MailboxException {

        try {
            
            // We need to reload a "JPA attached" mailbox, because the provide mailbox is already "JPA detached"
            // If we don't this, we will get an org.apache.openjpa.persistence.ArgumentException.
            ((AbstractJPAMessage) message).setMailbox(getEntityManager().find(JPAMailbox.class, mailbox.getMailboxId()));
            
            getEntityManager().persist(message);
            
            return message.getUid();
        
        } catch (PersistenceException e) {
            throw new MailboxException("Save of message " + message + " failed in mailbox " + mailbox, e);
        } catch (ArgumentException e) {
            throw new MailboxException("Save of message " + message + " failed in mailbox " + mailbox, e);
        }

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, long, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long copy(Mailbox<Long> mailbox, long uid, Message<Long> original) throws MailboxException {
        Message<Long> copy;
        if (original instanceof JPAStreamingMessage) {
            copy = new JPAStreamingMessage((JPAMailbox) mailbox, uid, original);
        } else {
            copy = new JPAMessage((JPAMailbox) mailbox, uid,  original);
        }
        return add(mailbox, copy);
    }
   
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<UpdatedFlags> updateFlags(Mailbox<Long> mailbox, final Flags flags, final boolean value, final boolean replace, MessageRange set) throws MailboxException {

        final List<UpdatedFlags> updatedFlags = new ArrayList<UpdatedFlags>();
        findInMailbox(mailbox, set, new MailboxMembershipCallback<Long>() {

            public void onMailboxMembers(List<Message<Long>> members) throws MailboxException {
                for (final Message<Long> member : members) {
                    Flags originalFlags = member.createFlags();
                    if (replace) {
                        member.setFlags(flags);
                    } else {
                        Flags current = member.createFlags();
                        if (value) {
                            current.add(flags);
                        } else {
                            current.remove(flags);
                        }
                        member.setFlags(current);
                    }
                    Flags newFlags = member.createFlags();
                    getEntityManager().persist(member);
                    updatedFlags.add(new UpdatedFlags(member.getUid(), originalFlags, newFlags));
                }

            }
        });

        return updatedFlags.iterator();

    }
}

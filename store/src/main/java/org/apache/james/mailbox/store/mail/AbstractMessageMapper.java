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
package org.apache.james.mailbox.store.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageMetaData;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;

/**
 * Abstract base class for {@link MessageMapper} implementation which already takes care of most uid / mod-seq handling
 * 
 *
 * @param <Id>
 */
public abstract class AbstractMessageMapper<Id> extends TransactionalMapper implements MessageMapper<Id>{
    
    private final static ConcurrentHashMap<Object, AtomicLong> seqs = new ConcurrentHashMap<Object, AtomicLong>();
    private final static ConcurrentHashMap<Object, AtomicLong> uids = new ConcurrentHashMap<Object, AtomicLong>();

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#getHighestModSeq(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long getHighestModSeq(Mailbox<Id> mailbox) throws MailboxException {
        return retrieveLastUsedModSeq(mailbox).get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#getLastUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long getLastUid(Mailbox<Id> mailbox) throws MailboxException {
        return retrieveLastUid(mailbox).get();
    }

    
    private long nextUid(Mailbox<Id> mailbox) throws MailboxException {       
        return retrieveLastUid(mailbox).incrementAndGet();
    }
    
    
    /**
     * Return the next mod-seq which can be used while append a Message to the {@link Mailbox}.
     * Its important that the returned mod-seq is higher then the last used and that the next call of this method does not return the same mod-swq. 
     * 
     * @param mailbox
     * @return nextUid
     * @throws MailboxException
     */
    private long nextModSeq(Mailbox<Id> mailbox) throws MailboxException {
        return retrieveLastUsedModSeq(mailbox).incrementAndGet();
    }


    /**
     * Retrieve the last used mod-seq for the {@link Mailbox} from cache or via lazy lookup.
     * 
     * @param session
     * @param mailbox
     * @return lastModSeq
     * @throws MailboxException
     */
    protected AtomicLong retrieveLastUsedModSeq(Mailbox<Id> mailbox) throws MailboxException {
        AtomicLong seq = seqs.get(mailbox.getMailboxId());

        if (seq == null) {
            seq = new AtomicLong(higestModSeq(mailbox));
            AtomicLong cachedSeq = seqs.putIfAbsent(mailbox.getMailboxId(), seq);
            if (cachedSeq != null) {
                seq = cachedSeq;
            }
        }

        return seq;
    }
    
    private long higestModSeq(Mailbox<Id> mailbox) throws MailboxException {
        long modSeq = calculateHigestModSeq(mailbox);
        if (modSeq < 1) {
            modSeq = mailbox.getHighestKnownModSeq();
        }
        return modSeq;
    }
    
    /**
     * Retrieve the last uid for the {@link Mailbox} from cache or via lazy lookup.
     * 
     * @param mailbox
     * @return lastUid
     * @throws MailboxException
     */
    protected AtomicLong retrieveLastUid(Mailbox<Id> mailbox) throws MailboxException {
        AtomicLong uid = uids.get(mailbox.getMailboxId());

        if (uid == null) {
            uid = new AtomicLong(lastUid(mailbox));
            AtomicLong cachedUid = uids.putIfAbsent(mailbox.getMailboxId(), uid);
            if (cachedUid != null) {
                uid = cachedUid;
            }
        }

        return uid;
    }
    
    private long lastUid(Mailbox<Id> mailbox) throws MailboxException {
        long uid = calculateLastUid(mailbox);
        if (uid < 1) {
            uid = mailbox.getLastKnownUid();
        }
        return uid;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#expungeMarkedForDeletionInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public Map<Long, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox<Id> mailbox, MessageRange set) throws MailboxException {
        Map<Long, MessageMetaData> data = expungeMarkedForDeletion(mailbox, set);
        if (data.isEmpty() == false) {

            // Increase the mod-sequence  and the uid for this mailbox and save it permanent way
            // See MAILBOX-75 
            saveSequences(mailbox, nextUid(mailbox), nextModSeq(mailbox));

        }
        return data;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<UpdatedFlags> updateFlags(final Mailbox<Id> mailbox, final Flags flags, final boolean value, final boolean replace, MessageRange set) throws MailboxException {
        final List<UpdatedFlags> updatedFlags = new ArrayList<UpdatedFlags>();
        findInMailbox(mailbox, set, new MailboxMembershipCallback<Id>() {

            public void onMailboxMembers(List<Message<Id>> members) throws MailboxException {
                
                long modSeq = -1;
                if (members.isEmpty() == false) {
                    modSeq = nextModSeq(mailbox);
                }
                for (final Message<Id> member : members) {
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
                    if (UpdatedFlags.flagsChanged(originalFlags, newFlags)) {
                        // increase the mod-seq as we changed the flags
                        member.setModSeq(modSeq);
                        save(mailbox, member);

                    }

                    UpdatedFlags uFlags = new UpdatedFlags(member.getUid(), member.getModSeq(), originalFlags, newFlags);
                    
                    updatedFlags.add(uFlags);
                    

                }

            }
        });

        return updatedFlags.iterator();

    }
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#add(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.Message)
     */
    public MessageMetaData add(Mailbox<Id> mailbox, Message<Id> message) throws MailboxException {
        message.setUid(nextUid(mailbox));
        message.setModSeq(nextModSeq(mailbox));
        save(mailbox, message);
        return new SimpleMessageMetaData(message);
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.Message)
     */
    public MessageMetaData copy(Mailbox<Id> mailbox, Message<Id> original) throws MailboxException {
        long uid = nextUid(mailbox);
        long modSeq = nextModSeq(mailbox);
        copy(mailbox, uid, modSeq, original);
        return new SimpleMessageMetaData(uid, modSeq, original.createFlags(), original.getFullContentOctets(), original.getInternalDate());
    }

    

    
    /**
     * Return the higest mod-seq for the given {@link Mailbox}. This method is called in a lazy fashion. So when the first mod-seq is needed for a {@link Mailbox}
     * it will get called to get the higest used. After that it will stored in memory and just increment there on each {@link #nextModSeq(MailboxSession, Mailbox)} call.
     * 
     * @param mailbox
     * @return lastUid
     * @throws MailboxException
     */
    protected abstract long calculateHigestModSeq(Mailbox<Id> mailbox) throws MailboxException;
    
    /**
     * Return the last used uid for the given {@link Mailbox}. This method is called in a lazy fashion. So when the first uid is needed for a {@link Mailbox}
     * it will get called to get the last used. After that it will stored in memory and just increment there on each {@link #nextUid(MailboxSession, Mailbox)} call.
     * 
     * @param session
     * @param mailbox
     * @return lastUid
     * @throws MailboxException
     */
    protected abstract long calculateLastUid(Mailbox<Id> mailbox) throws MailboxException;
    
    
    /**
     * Save the {@link Message} for the given {@link Mailbox}
     * 
     * @param mailbox
     * @param message
     * @throws MailboxException
     */
    protected abstract void save(Mailbox<Id> mailbox, Message<Id> message) throws MailboxException;

    
    /**
     * Copy the Message to the Mailbox, using the given uid and modSeq for the new Message
     * 
     * @param mailbox
     * @param uid
     * @param modSeq
     * @param original
     * @throws MailboxException
     */
    protected abstract void copy(Mailbox<Id> mailbox, long uid, long modSeq, Message<Id> original) throws MailboxException;
    
    
    /**
     * Expunge all Messages which are marked for deletion
     * 
     * @param mailbox
     * @param set
     * @return
     * @throws MailboxException
     */
    protected abstract Map<Long, MessageMetaData> expungeMarkedForDeletion(Mailbox<Id> mailbox, MessageRange set) throws MailboxException;

    /**
     * Save the sequence meta-data for the mailbox in a permanent way
     * 
     * @param mailbox
     * @param lastUid
     * @param highestModSeq
     * @throws MailboxException
     */
    protected abstract void saveSequences(Mailbox<Id> mailbox, long lastUid, long highestModSeq) throws MailboxException;
    
}

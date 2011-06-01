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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.SearchQuery.UidCriterion;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SearchQueryIterator;
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
    protected final MailboxSession mailboxSession;
    protected final MessageSearchIndex<Id> index;
    
    public AbstractMessageMapper(MailboxSession mailboxSession) {
        this(mailboxSession, null);
    }
    
    public AbstractMessageMapper(MailboxSession mailboxSession, MessageSearchIndex<Id> index) {
        this.mailboxSession = mailboxSession;
        this.index = index;
    }
    
    
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

            // Increase the mod-sequence and save it with the uid for this mailbox in a permanent way
            // See MAILBOX-75 
            saveSequences(mailbox, getLastUid(mailbox), nextModSeq(mailbox));

        }
        if (index != null) {
            Iterator<MessageRange> rangeIt = MessageRange.toRanges(new ArrayList<Long>(data.keySet())).iterator();
            while(rangeIt.hasNext()) {
                index.delete(mailboxSession, mailbox, rangeIt.next());
            }
        }
        return data;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<UpdatedFlags> updateFlags(final Mailbox<Id> mailbox, final Flags flags, final boolean value, final boolean replace, final MessageRange set) throws MailboxException {
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

                        

                        if (replace == false && index != null) {
                            index.update(mailboxSession, mailbox, MessageRange.one(member.getUid()), newFlags);
                        }
                    }

                    
                    UpdatedFlags uFlags = new UpdatedFlags(member.getUid(), member.getModSeq(), originalFlags, newFlags);
                    
                    updatedFlags.add(uFlags);
                    

                }
                

                // as it was a replace operation we can just use the given message for update the index
                if (replace && index != null) {
                    index.update(mailboxSession, mailbox, set, flags);
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
        MessageMetaData data = save(mailbox, message);
        if (index != null) {
            index.add(mailboxSession, mailbox, message);
        }
        return data;
        
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.Message)
     */
    public MessageMetaData copy(final Mailbox<Id> mailbox, final Message<Id> original) throws MailboxException {
        long uid = nextUid(mailbox);
        long modSeq = nextModSeq(mailbox);
        final MessageMetaData metaData = copy(mailbox, uid, modSeq, original);  
        if (index != null) {
            // index the copied message by building a wrapper around the original message and the returned metadata
            index.add(mailboxSession, mailbox, new Message<Id>() {

                @Override
                public Flags createFlags() {
                    return original.createFlags();
                }

                @Override
                public InputStream getBodyContent() throws IOException {
                    return original.getBodyContent();
                }

                @Override
                public long getBodyOctets() {
                    return original.getBodyOctets();
                }

                @Override
                public InputStream getFullContent() throws IOException {
                    return original.getFullContent();
                }

                @Override
                public long getFullContentOctets() {
                    return original.getFullContentOctets();
                }

                @Override
                public List<Header> getHeaders() {
                    return original.getHeaders();
                }

                @Override
                public Date getInternalDate() {
                    return metaData.getInternalDate();
                }

                @Override
                public Id getMailboxId() {
                    return mailbox.getMailboxId();
                }

                @Override
                public String getMediaType() {
                    return original.getMediaType();
                }

                @Override
                public long getModSeq() {
                    return metaData.getModSeq();
                }

                @Override
                public List<Property> getProperties() {
                    return original.getProperties();
                }

                @Override
                public String getSubType() {
                    return original.getSubType();
                }

                @Override
                public Long getTextualLineCount() {
                    return original.getTextualLineCount();
                }

                @Override
                public long getUid() {
                    return metaData.getUid();
                }

                @Override
                public boolean isAnswered() {
                    return original.isAnswered();
                }

                @Override
                public boolean isDeleted() {
                    return original.isDeleted();

                }

                @Override
                public boolean isDraft() {
                    return original.isDraft();

                }

                @Override
                public boolean isFlagged() {
                    return original.isFlagged();
                }

                @Override
                public boolean isRecent() {
                    return original.isRecent();
                }

                @Override
                public boolean isSeen() {
                    return original.isSeen();
                }

                @Override
                public void setFlags(Flags flags) {
                    throw new UnsupportedOperationException("Read-Only Message");
                }

                @Override
                public void setModSeq(long modSeq) {
                    throw new UnsupportedOperationException("Read-Only Message");
                }

                @Override
                public void setUid(long uid) {
                    throw new UnsupportedOperationException("Read-Only Message");
                }

                @Override
                public int compareTo(Message<Id> o) {
                    return (int) (metaData.getUid() - o.getUid());
                }
                
            
            });
        } 
        
        return metaData;
    }

    

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#search(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> search(Mailbox<Id> mailbox, SearchQuery query) throws MailboxException {
        if (index == null) {
            List<Criterion> crits = query.getCriterias();

            if (crits.size() == 1  && crits.get(0) instanceof UidCriterion) {
                final List<Long> uids = new ArrayList<Long>();
                UidCriterion uidCrit = (UidCriterion) crits.get(0);
                NumericRange[] ranges = uidCrit.getOperator().getRange();
                for (int i = 0; i < ranges.length; i++) {
                    NumericRange r = ranges[i];
                    findInMailbox(mailbox, MessageRange.range(r.getLowValue(), r.getHighValue()), new MailboxMembershipCallback<Id>() {

                        public void onMailboxMembers(List<Message<Id>> list) throws MailboxException {
                            for (int i = 0; i < list.size(); i++) {
                                long uid = list.get(i).getUid();
                                if (uids.contains(uid) == false) {
                                    uids.add(uid);
                                }
                            }
                        }
                    });
                }
                Collections.sort(uids);
                return uids.iterator();
                
               
            } else {
                
                final List<Message<Id>> hits = new ArrayList<Message<Id>>();

                findInMailbox(mailbox, MessageRange.all(), new MailboxMembershipCallback<Id>() {

                    public void onMailboxMembers(List<Message<Id>> list) throws MailboxException {
                        for (int i = 0; i < list.size(); i++) {
                            Message<Id> m = list.get(i);
                            if (hits.contains(m) == false) {
                                hits.add(m);
                            }
                        }
                    }
                });
                Collections.sort(hits);
                
                Iterator<Message<?>> it = new Iterator<Message<?>>() {
                    final Iterator<Message<Id>> it = hits.iterator();
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    public Message<?> next() {
                        return it.next();
                    }

                    public void remove() {
                        it.remove();
                    }
                    
                };
                
                if (mailboxSession == null) {
                    return new SearchQueryIterator(it, query);
                } else {
                    return new SearchQueryIterator(it, query, mailboxSession.getLog());
                }
            }
        } else {
            return index.search(mailboxSession, mailbox, query);
        }

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
     * Save the {@link Message} for the given {@link Mailbox} and return the {@link MessageMetaData} 
     * 
     * @param mailbox
     * @param message
     * @return metaData
     * @throws MailboxException
     */
    protected abstract MessageMetaData save(Mailbox<Id> mailbox, Message<Id> message) throws MailboxException;

    
    /**
     * Copy the Message to the Mailbox, using the given uid and modSeq for the new Message
     * 
     * @param mailbox
     * @param uid
     * @param modSeq
     * @param original
     * @return metaData
     * @throws MailboxException
     */
    protected abstract MessageMetaData copy(Mailbox<Id> mailbox, long uid, long modSeq, Message<Id> original) throws MailboxException;
    
    
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

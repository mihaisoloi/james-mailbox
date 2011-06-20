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

package org.apache.james.mailbox.inmemory.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageMetaData;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.AbstractMessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;

public class InMemoryMessageMapper extends AbstractMessageMapper<Long> {

    private Map<Long, Map<Long, Message<Long>>> mailboxByUid;
    private static final int INITIAL_SIZE = 256;
    
    public InMemoryMessageMapper(MailboxSession session) {
        super(session);
        this.mailboxByUid = new ConcurrentHashMap<Long, Map<Long, Message<Long>>>(INITIAL_SIZE);
    }
    
    private Map<Long, Message<Long>> getMembershipByUidForMailbox(Mailbox<Long> mailbox) {
        Map<Long, Message<Long>> membershipByUid = mailboxByUid.get(mailbox.getMailboxId());
        if (membershipByUid == null) {
            membershipByUid = new ConcurrentHashMap<Long, Message<Long>>(INITIAL_SIZE);
            mailboxByUid.put(mailbox.getMailboxId(), membershipByUid);
        }
        return membershipByUid;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        return getMembershipByUidForMailbox(mailbox).size();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        long count = 0;
        for(Message<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
            if (!member.isSeen()) {
                count++;
            }
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#delete(org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<Long> mailbox, Message<Long> message) throws MailboxException {
        getMembershipByUidForMailbox(mailbox).remove(message.getUid());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findInMailbox(java.lang.Object, org.apache.james.mailbox.MessageRange)
     */
    public void findInMailbox(Mailbox<Long> mailbox, MessageRange set, MessageCallback<Long> callback) throws MailboxException {
        final List<Message<Long>> results;
        final int batchSize = set.getBatchSize();
        final MessageRange.Type type = set.getType();
        switch (type) {
            case ALL:
                results = new ArrayList<Message<Long>>(getMembershipByUidForMailbox(mailbox).values());
                break;
            case FROM:
                results = new ArrayList<Message<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<Message<Long>> it=results.iterator();it.hasNext();) {
                   if (it.next().getUid()< set.getUidFrom()) {
                       it.remove(); 
                   }
                }
                break;
            case RANGE:
                results = new ArrayList<Message<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<Message<Long>> it=results.iterator();it.hasNext();) {
                   final long uid = it.next().getUid();
                if (uid<set.getUidFrom() || uid>set.getUidTo()) {
                       it.remove(); 
                   }
                }
                break;
            case ONE:
                results  = new ArrayList<Message<Long>>(1);
                final Message<Long> member = getMembershipByUidForMailbox(mailbox).get(set.getUidFrom());
                if (member != null) {
                    results.add(member);
                }
                break;
            default:
                results = new ArrayList<Message<Long>>();
                break;
        }
        Collections.sort(results);
        
        if(batchSize > 0) {
	        int i = 0;
	        while(i*batchSize < results.size()) {
	        	callback.onMessages(results.subList(i*batchSize, (i+1)*batchSize < results.size() ? (i+1)*batchSize : results.size()));
	        	i++;
	        }
        } else {
        	callback.onMessages(results);
        }
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#expungeMarkedForDeletionInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public Map<Long, MessageMetaData> expungeMarkedForDeletion(final Mailbox<Long> mailbox, MessageRange set) throws MailboxException {
        final Map<Long, MessageMetaData> filteredResult = new HashMap<Long, MessageMetaData>();

        findInMailbox(mailbox, set, new MessageCallback<Long>() {

            public void onMessages(List<Message<Long>> results) throws MailboxException {
                for (final Iterator<Message<Long>> it = results.iterator(); it.hasNext();) {
                    Message<Long> member = it.next();
                    if (member.isDeleted()) {
                        filteredResult.put(member.getUid(), new SimpleMessageMetaData(member));

                        delete(mailbox, member);
                    }
                }
            }
        });

        return filteredResult;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    public List<Long> findRecentMessageUidsInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        final List<Long> results = new ArrayList<Long>();
        for(Message<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
            if (member.isRecent()) {
                results.add(member.getUid());
            }
        }
        Collections.sort(results);
        
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Long> mailbox) throws MailboxException {
        List<Message<Long>> memberships = new ArrayList<Message<Long>>(getMembershipByUidForMailbox(mailbox).values());
        Collections.sort(memberships);
        for (int i = 0;  i < memberships.size(); i++) {
            Message<Long> m = memberships.get(i);
            if (m.isSeen() == false) {
                return m.getUid();
            }
        }
        return null;
    }

    public void deleteAll() {
        mailboxByUid.clear();
    }

    /**
     * Do nothing
     */
    public void endRequest() {
        // Do nothing
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, long, long, org.apache.james.mailbox.store.mail.model.Message)
     */
    protected MessageMetaData copy(Mailbox<Long> mailbox, long uid, long modSeq, Message<Long> original) throws MailboxException {
        SimpleMessage<Long> message = new SimpleMessage<Long>(mailbox, original);
        message.setUid(uid);
        message.setModSeq(modSeq);
        Flags flags = original.createFlags();
        
        // Mark message as recent as it is a copy
        flags.add(Flag.RECENT);
        message.setFlags(flags);
        return save(mailbox, message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#calculateHigestModSeq(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long calculateHigestModSeq(Mailbox<Long> mailbox) throws MailboxException {
        return 0;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#calculateLastUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long calculateLastUid(Mailbox<Long> mailbox) throws MailboxException {
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.Message)
     */
    protected MessageMetaData save(Mailbox<Long> mailbox, Message<Long> message) throws MailboxException {
        SimpleMessage<Long> copy = new SimpleMessage<Long>(mailbox, message);
        copy.setUid(message.getUid());
        copy.setModSeq(message.getModSeq());
        getMembershipByUidForMailbox(mailbox).put(message.getUid(), copy);
        
        return new SimpleMessageMetaData(message);
    }

    /**
     * Do nothing
     */
    protected void begin() throws MailboxException {
        
    }

    /**
     * Do nothing
     */
    protected void commit() throws MailboxException {
        
    }


    /**
     * Do nothing
     */
    protected void rollback() throws MailboxException {        
    }

    /**
     * Do nothing
     */
    protected void saveSequences(Mailbox<Long> mailbox, long lastUid, long highestModSeq) throws MailboxException {
        // Nothing todo as its a in memory implementation
        
    }
}

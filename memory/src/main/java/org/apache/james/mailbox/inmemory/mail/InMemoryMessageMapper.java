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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.inmemory.mail.model.SimpleMailboxMembership;
import org.apache.james.mailbox.store.SearchQueryIterator;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.MailboxMembershipComparator;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;

public class InMemoryMessageMapper extends NonTransactionalMapper implements MessageMapper<Long> {

    private Map<Long, Map<Long, MailboxMembership<Long>>> mailboxByUid;
    private static final int INITIAL_SIZE = 256;
    
    public InMemoryMessageMapper() {
        this.mailboxByUid = new ConcurrentHashMap<Long, Map<Long, MailboxMembership<Long>>>(INITIAL_SIZE);
    }
    
    private Map<Long, MailboxMembership<Long>> getMembershipByUidForMailbox(Mailbox<Long> mailbox) {
        Map<Long, MailboxMembership<Long>> membershipByUid = mailboxByUid.get(mailbox.getMailboxId());
        if (membershipByUid == null) {
            membershipByUid = new ConcurrentHashMap<Long, MailboxMembership<Long>>(INITIAL_SIZE);
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
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
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
    public void delete(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws MailboxException {
        getMembershipByUidForMailbox(mailbox).remove(message.getUid());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findInMailbox(java.lang.Object, org.apache.james.mailbox.MessageRange)
     */
    @SuppressWarnings("unchecked")
    public void findInMailbox(Mailbox<Long> mailbox, MessageRange set, MailboxMembershipCallback<Long> callback) throws MailboxException {
        final List<MailboxMembership<Long>> results;
        final int batchSize = set.getBatchSize();
        final MessageRange.Type type = set.getType();
        switch (type) {
            case ALL:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                break;
            case FROM:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   if (it.next().getUid()< set.getUidFrom()) {
                       it.remove(); 
                   }
                }
                break;
            case RANGE:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   final long uid = it.next().getUid();
                if (uid<set.getUidFrom() || uid>set.getUidTo()) {
                       it.remove(); 
                   }
                }
                break;
            case ONE:
                results  = new ArrayList<MailboxMembership<Long>>(1);
                final MailboxMembership member = getMembershipByUidForMailbox(mailbox).get(set.getUidFrom());
                if (member != null) {
                    results.add(member);
                }
                break;
            default:
                results = new ArrayList<MailboxMembership<Long>>();
                break;
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        
        if(batchSize > 0) {
	        int i = 0;
	        while(i*batchSize < results.size()) {
	        	callback.onMailboxMembers(results.subList(i*batchSize, (i+1)*batchSize < results.size() ? (i+1)*batchSize : results.size()));
	        	i++;
	        }
        } else {
        	callback.onMailboxMembers(results);
        }
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#expungeMarkedForDeletionInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<Long> expungeMarkedForDeletionInMailbox(final Mailbox<Long> mailbox, MessageRange set) throws MailboxException {
        final List<Long> filteredResult = new ArrayList<Long>();

        findInMailbox(mailbox, set, new MailboxMembershipCallback<Long>() {

            public void onMailboxMembers(List<MailboxMembership<Long>> results) throws MailboxException {
                for (final Iterator<MailboxMembership<Long>> it = results.iterator(); it.hasNext();) {
                    MailboxMembership<Long> member = it.next();
                    if (member.isDeleted()) {
                        delete(mailbox, member);
                        filteredResult.add(member.getUid());
                    }
                }
            }
        });

        return filteredResult.iterator();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox(Mailbox<Long> mailbox) throws MailboxException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
            if (member.isRecent()) {
                results.add(member);
            }
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Long> mailbox) throws MailboxException {
        List<MailboxMembership<Long>> memberships = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
        Collections.sort(memberships, MailboxMembershipComparator.INSTANCE);
        for (int i = 0;  i < memberships.size(); i++) {
            MailboxMembership<Long> m = memberships.get(i);
            if (m.isSeen() == false) {
                return m.getUid();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#save(org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long add(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws MailboxException {
        getMembershipByUidForMailbox(mailbox).put(message.getUid(), message);
        return message.getUid();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#searchMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> searchMailbox(Mailbox<Long> mailbox, SearchQuery query) throws MailboxException {
        List<MailboxMembership<?>> memberships = new ArrayList<MailboxMembership<?>>(getMembershipByUidForMailbox(mailbox).values());
        Collections.sort(memberships, MailboxMembershipComparator.INSTANCE);

        return new SearchQueryIterator(memberships.iterator(), query);
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
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long copy(Mailbox<Long> mailbox, long uid, MailboxMembership<Long> original) throws MailboxException {        
        SimpleMailboxMembership membership = new SimpleMailboxMembership(mailbox.getMailboxId(), uid, (SimpleMailboxMembership) original);
        return add(mailbox, membership);
    }

    public Iterator<UpdatedFlags> updateFlags(final Mailbox<Long> mailbox, final Flags flags, final boolean value, final boolean replace, MessageRange set) throws MailboxException {
        final List<UpdatedFlags> updatedFlags = new ArrayList<UpdatedFlags>();

        findInMailbox(mailbox, set, new MailboxMembershipCallback<Long>() {
			
			public void onMailboxMembers(List<MailboxMembership<Long>> members)
					throws MailboxException {
				for (final MailboxMembership<Long> member:members) {
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
		            
		            add(mailbox, member);
		            
		            updatedFlags.add(new UpdatedFlags(member.getUid(),originalFlags, newFlags));
		        }
				
			}
		});
        
        return updatedFlags.iterator();       
    }
}

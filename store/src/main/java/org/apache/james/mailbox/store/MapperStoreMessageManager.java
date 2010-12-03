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

package org.apache.james.mailbox.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * Abstract base class for {@link org.apache.james.mailbox.MessageManager} implementations.
 * 
 * This class provides a high-level api, and is most times the best to just extend
 * 
 *
 */
public abstract class MapperStoreMessageManager<Id> extends StoreMessageManager<Id> {

   
    private MessageMapperFactory<Id> mapperFactory;

    
    public MapperStoreMessageManager(MessageMapperFactory<Id> mapperFactory, final UidProvider<Id> uidProvider, final MailboxEventDispatcher dispatcher, final Mailbox<Id> mailbox) throws MailboxException {
        super(uidProvider, dispatcher, mailbox);
        this.mapperFactory = mapperFactory;
    }


    @Override
    protected long appendMessageToStore(final MailboxMembership<Id> message, MailboxSession session) throws MailboxException {
        final MessageMapper<Id> mapper = mapperFactory.getMessageMapper(session);
        return mapperFactory.getMessageMapper(session).execute(new Mapper.Transaction<Long>() {

            public Long run() throws MailboxException {
                return mapper.save(getMailboxEntity(), message);
            }
            
        });
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#getMessageCount(org.apache.james.mailbox.MailboxSession)
     */
    public long getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        return mapperFactory.getMessageMapper(mailboxSession).countMessagesInMailbox(getMailboxEntity());
    }




    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#getMessages(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MessageResult.FetchGroup, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<MessageResult> getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        final List<MailboxMembership<Id>> rows = mapperFactory.getMessageMapper(mailboxSession).findInMailbox(getMailboxEntity(), set);
        return new ResultIterator<Id>(rows.iterator(), fetchGroup);
    }

    /**
     * Return a List which holds all uids of recent messages and optional reset the recent flag on the messages for the uids
     * 
     * @param reset
     * @param mailboxSession
     * @return list
     * @throws MailboxException
     */
    protected List<Long> recent(final boolean reset, MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(mailboxSession);
        
        return messageMapper.execute(new Mapper.Transaction<List<Long>>() {

            public List<Long> run() throws MailboxException {
                final List<MailboxMembership<Id>> members = messageMapper.findRecentMessagesInMailbox(getMailboxEntity(), -1);
                final List<Long> results = new ArrayList<Long>();

                for (MailboxMembership<Id> member:members) {
                    results.add(member.getUid());
                    if (reset) {
                        member.unsetRecent();
                        
                        // only call save if we need to
                        messageMapper.save(getMailboxEntity(), member);
                    }
                }
                return results;
            }
            
        });
        
    }

    @Override
    protected Iterator<Long> deleteMarkedInMailbox(final MessageRange range, final MailboxSession session) throws MailboxException {
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

        return messageMapper.execute(new Mapper.Transaction<Iterator<Long>>() {

            public Iterator<Long> run() throws MailboxException {
                final Collection<Long> uids = new TreeSet<Long>();

                final List<MailboxMembership<Id>> members = messageMapper.findMarkedForDeletionInMailbox(getMailboxEntity(), range);
                for (MailboxMembership<Id> message:members) {
                    uids.add(message.getUid());
                    messageMapper.delete(getMailboxEntity(), message);
                    
                }  
                return uids.iterator();
            }
            
        });       
    }


    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.AbstractStoreMessageManager#updateFlags(javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<UpdatedFlag> updateFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(mailboxSession);

        return messageMapper.execute(new Mapper.Transaction<Iterator<UpdatedFlag>>(){

            public Iterator<UpdatedFlag> run() throws MailboxException {
                final List<UpdatedFlag> updatedFlags = new ArrayList<UpdatedFlag>();

                final List<MailboxMembership<Id>> members = messageMapper.findInMailbox(getMailboxEntity(), set);
                for (final MailboxMembership<Id> member:members) {
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
                    messageMapper.save(getMailboxEntity(), member);
                    updatedFlags.add(new UpdatedFlag(member.getUid(),originalFlags, newFlags));
                }
                
                return updatedFlags.iterator();
            }
            
        });
       
    }




    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#search(org.apache.james.mailbox.SearchQuery, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> search(SearchQuery query, MailboxSession mailboxSession) throws MailboxException {
        return mapperFactory.getMessageMapper(mailboxSession).searchMailbox(getMailboxEntity(), query);    
    }


    private Iterator<Long> copy(final List<MailboxMembership<Id>> originalRows, final MailboxSession session) throws MailboxException {
        try {
            final List<Long> copiedRows = new ArrayList<Long>();
            final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

            for (final MailboxMembership<Id> originalMessage:originalRows) {
               copiedRows.add(messageMapper.execute(new Mapper.Transaction<Long>() {

                    public Long run() throws MailboxException {

                        return messageMapper.copy(getMailboxEntity(), originalMessage);
                        
                    }
                    
                }));
            }
            return copiedRows.iterator();
        } catch (MessagingException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.AbstractStoreMessageManager#copy(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.store.AbstractStoreMessageManager, org.apache.james.mailbox.MailboxSession)
     */
    protected Iterator<Long> copy(MessageRange set, StoreMessageManager<Id> toMailbox, MailboxSession session) throws MailboxException {
        MapperStoreMessageManager<Id> to = (MapperStoreMessageManager<Id>) toMailbox;
        try {
            MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);
            final List<MailboxMembership<Id>> originalRows = messageMapper.findInMailbox(getMailboxEntity(), set);
            return to.copy(originalRows, session);

        } catch (MessagingException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.AbstractStoreMessageManager#countUnseenMessagesInMailbox(org.apache.james.mailbox.MailboxSession)
     */
    protected long countUnseenMessagesInMailbox(MailboxSession session) throws MailboxException {
        MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);
        return messageMapper.countUnseenMessagesInMailbox(getMailboxEntity());
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.AbstractStoreMessageManager#findFirstUnseenMessageUid(org.apache.james.mailbox.MailboxSession)
     */
    protected Long findFirstUnseenMessageUid(MailboxSession session) throws MailboxException{
        MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);
        return messageMapper.findFirstUnseenMessageUid(getMailboxEntity());
    }

}
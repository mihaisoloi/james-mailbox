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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.util.SharedFileInputStream;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.SearchQuery.UidCriterion;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.store.transaction.Mapper.MailboxMembershipCallback;
import org.apache.james.mailbox.util.MailboxEventDispatcher;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * Abstract base class for {@link org.apache.james.mailbox.MessageManager} implementations. This abstract
 * class take care of dispatching events to the registered {@link MailboxListener} and so help
 * with handling concurrent {@link MailboxSession}'s. So this is a perfect starting point when writing your 
 * own implementation and don't want to depend on {@link MessageMapper}.
 *
 */
public abstract class StoreMessageManager<Id> implements org.apache.james.mailbox.MessageManager{


    private final Mailbox<Id> mailbox;
    
    private final MailboxEventDispatcher dispatcher;    
    
    protected final UidProvider<Id> uidProvider;

    protected MessageMapperFactory<Id> mapperFactory;
    
    public StoreMessageManager(final MessageMapperFactory<Id> mapperFactory, final UidProvider<Id> uidProvider, final MailboxEventDispatcher dispatcher, final Mailbox<Id> mailbox) throws MailboxException {
        this.mailbox = mailbox;
        this.dispatcher = dispatcher;
        this.uidProvider = uidProvider;
        this.mapperFactory = mapperFactory;
    }
    
    
    
    /**
     * Return the {@link MailboxEventDispatcher} for this Mailbox
     * 
     * @return dispatcher
     */
    protected MailboxEventDispatcher getDispatcher() {
        return dispatcher;
    }
    
    /**
     * Return the underlying {@link Mailbox}
     * 
     * @param session
     * @return mailbox
     * @throws MailboxException
     */
    
    public Mailbox<Id> getMailboxEntity() throws MailboxException {
        return mailbox;
    }
    

    
    private Flags getPermanentFlags(MailboxSession session) {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        if (areUserFlagsPermanent(session)) {
            permanentFlags.add(Flags.Flag.USER);
        }
        return permanentFlags;
    }

    /**
     * Sub-classes should override this and return true if they support to store user flags(keywords) permanent.
     * 
     * The default is to return false
     * 
     * @param session
     * @return supported 
     */
    protected boolean areUserFlagsPermanent(MailboxSession session) {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#expunge(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        List<Long> uids = new ArrayList<Long>();
        Iterator<Long> uidIt = deleteMarkedInMailbox(set, mailboxSession);
        while(uidIt.hasNext()) {
            long uid = uidIt.next();
            uids.add(uid);
        }
        dispatcher.expunged(mailboxSession, uids, new StoreMailboxPath<Id>(getMailboxEntity()));
        return uids.iterator();    
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#appendMessage(byte[], java.util.Date, org.apache.james.mailbox.MailboxSession, boolean, javax.mail.Flags)
     */
    public long appendMessage(final InputStream msgIn, final Date internalDate,
            final MailboxSession mailboxSession,final boolean isRecent, final Flags flagsToBeSet)
    throws MailboxException {
        File file = null;
        SharedFileInputStream tmpMsgIn = null;
        try {
            // Create a temporary file and copy the message to it. We will work with the file as
            // source for the InputStream
            file = File.createTempFile("imap", ".msg");
            FileOutputStream out = new FileOutputStream(file);
            
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = msgIn.read(buf)) != -1) {
                out.write(buf, 0, i);
            }
            out.flush();
            out.close();
            
            tmpMsgIn = new SharedFileInputStream(file);
           
            final int size = tmpMsgIn.available();
            final int bodyStartOctet = bodyStartOctet(tmpMsgIn);

            // Disable line length... This should be handled by the smtp server component and not the parser itself
            // https://issues.apache.org/jira/browse/IMAP-122
            MimeEntityConfig config = new MimeEntityConfig();
            config.setMaximalBodyDescriptor(true);
            config.setMaxLineLen(-1);
            final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);
           
            parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
            parser.parse(tmpMsgIn.newStream(0, -1));
            final List<Header> headers = new ArrayList<Header>();
            
            int lineNumber = 0;
            int next = parser.next();
            while (next != MimeTokenStream.T_BODY
                    && next != MimeTokenStream.T_END_OF_STREAM
                    && next != MimeTokenStream.T_START_MULTIPART) {
                if (next == MimeTokenStream.T_FIELD) {
                    String fieldValue = parser.getField().getBody();
                    if (fieldValue.endsWith("\r\f")) {
                        fieldValue = fieldValue.substring(0,fieldValue.length() - 2);
                    }
                    if (fieldValue.startsWith(" ")) {
                        fieldValue = fieldValue.substring(1);
                    }
                    final Header header 
                        = createHeader(++lineNumber, parser.getField().getName(), 
                            fieldValue);
                    headers.add(header);
                }
                next = parser.next();
            }
            final MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
            final PropertyBuilder propertyBuilder = new PropertyBuilder();
            final String mediaType;
            final String mediaTypeFromHeader = descriptor.getMediaType();
            final String subType;
            if (mediaTypeFromHeader == null) {
                mediaType = "text";
                subType = "plain";
            } else {
                mediaType = mediaTypeFromHeader;
                subType = descriptor.getSubType();
            }
            propertyBuilder.setMediaType(mediaType);
            propertyBuilder.setSubType(subType);
            propertyBuilder.setContentID(descriptor.getContentId());
            propertyBuilder.setContentDescription(descriptor.getContentDescription());
            propertyBuilder.setContentLocation(descriptor.getContentLocation());
            propertyBuilder.setContentMD5(descriptor.getContentMD5Raw());
            propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding());
            propertyBuilder.setContentLanguage(descriptor.getContentLanguage());
            propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType());
            propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters());
            propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters());
            // Add missing types
            final String codeset = descriptor.getCharset();
            if (codeset == null) {
                if ("TEXT".equalsIgnoreCase(mediaType)) {
                    propertyBuilder.setCharset("us-ascii");
                }
            } else {
                propertyBuilder.setCharset(codeset);
            }
            
            final String boundary = descriptor.getBoundary();
            if (boundary != null) {
                propertyBuilder.setBoundary(boundary);
            }   
            if ("text".equalsIgnoreCase(mediaType)) {
                final CountingInputStream bodyStream = new CountingInputStream(parser.getInputStream());
                bodyStream.readAll();
                long lines = bodyStream.getLineCount();
                
                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE)  {
                    final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                    epilogueStream.readAll();
                    lines+=epilogueStream.getLineCount();
                }
                propertyBuilder.setTextualLineCount(lines);
            }
            
            final Flags flags;
            if (flagsToBeSet == null) {
                flags = new Flags();
            } else {
                flags = flagsToBeSet;

                // Check if we need to trim the flags
                trimFlags(flags, mailboxSession);

            }
            if (isRecent) {
                flags.add(Flags.Flag.RECENT);
            }
            long nextUid = uidProvider.nextUid(mailboxSession, getMailboxEntity());
            final Message<Id> message = createMessage(nextUid, internalDate, size, bodyStartOctet, tmpMsgIn.newStream(0, -1), flags, headers, propertyBuilder);
            long uid = appendMessageToStore(message, mailboxSession);
                        
            dispatcher.added(mailboxSession, Arrays.asList(uid), new StoreMailboxPath<Id>(getMailboxEntity()));
            return uid;
        } catch (IOException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MimeException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        } finally {
            if (tmpMsgIn != null) {
                try {
                    tmpMsgIn.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
            // delete the temporary file if one was specified
            if (file != null) {
                file.delete();
            }
        }

    }
    
    /**
     * Return the position in the given {@link InputStream} at which the Body of the 
     * Message starts
     * 
     * @param msgIn
     * @return bodyStartOctet
     * @throws IOException
     */
    private int bodyStartOctet(InputStream msgIn) throws IOException{
        // we need to pushback maximal 3 bytes
        PushbackInputStream in = new PushbackInputStream(msgIn,3);
        
        int bodyStartOctet = in.available();
        int i = -1;
        int count = 0;
        while ((i = in.read()) != -1 && in.available() > 4) {
            if (i == 0x0D) {
                int a = in.read();
                if (a == 0x0A) {
                    int b = in.read();

                    if (b == 0x0D) {
                        int c = in.read();

                        if (c == 0x0A) {
                            bodyStartOctet = count+4;
                            break;
                        }
                        in.unread(c);
                    }
                    in.unread(b);
                }
                in.unread(a);
            }
            count++;
        }
        
        return bodyStartOctet;
    }

    /**
     * Create a new {@link MailboxMembership} for the given data
     * 
     * @param uid
     * @param internalDate
     * @param size
     * @param bodyStartOctet
     * @param documentIn
     * @param flags
     * @param headers
     * @param propertyBuilder
     * @return membership
     * @throws MailboxException 
     */
    protected abstract Message<Id> createMessage(long uid, Date internalDate, final int size, int bodyStartOctet, 
            final InputStream documentIn, final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException;
    
    /**
     * Create a new {@link Header} for the given data
     * 
     * @param lineNumber
     * @param name
     * @param value
     * @return header
     */
    protected abstract Header createHeader(int lineNumber, String name, String value);
    
    public void addListener(MailboxListener listener) throws MailboxException {
        dispatcher.addMailboxListener(listener);
    }
    

    /**
     * This mailbox is writable
     */
    public boolean isWriteable(MailboxSession session) {
        return true;
    }
    
    
    /**
     * @see {@link Mailbox#getMetaData(boolean, MailboxSession, FetchGroup)}
     */
    public MetaData getMetaData(boolean resetRecent, MailboxSession mailboxSession, 
            org.apache.james.mailbox.MessageManager.MetaData.FetchGroup fetchGroup) throws MailboxException {
        final List<Long> recent = recent(resetRecent, mailboxSession);
        final Flags permanentFlags = getPermanentFlags(mailboxSession);
        final long uidValidity = getMailboxEntity().getUidValidity();
        final long uidNext = uidProvider.lastUid(mailboxSession, mailbox) +1;
        final long messageCount = getMessageCount(mailboxSession);
        final long unseenCount;
        final Long firstUnseen;
        switch (fetchGroup) {
            case UNSEEN_COUNT:
                unseenCount = countUnseenMessagesInMailbox(mailboxSession);
                firstUnseen = null;
                break;
            case FIRST_UNSEEN:
                firstUnseen = findFirstUnseenMessageUid(mailboxSession);
                unseenCount = 0;
                break;
            default:
                firstUnseen = null;
                unseenCount = 0;
                break;
        }
        return new MailboxMetaData(recent, permanentFlags, uidValidity, uidNext, messageCount, unseenCount, firstUnseen, isWriteable(mailboxSession));
    }

 
    // Check if this mailbox supports user flags and if not remove all of them before 
    // pass this to the mapper
    private void trimFlags(Flags flags, MailboxSession session) {
        
        Flags permFlags = getPermanentFlags(session);
        
        Flag[] systemFlags = flags.getSystemFlags();
        for (int i = 0; i <  systemFlags.length; i++) {
            Flag f = systemFlags[i];
            if (permFlags.contains(f) == false) {
                flags.remove(f);
            }
        }
        if (permFlags.contains(Flags.Flag.USER) == false) {
            String[] uFlags = flags.getUserFlags();
            for (int i = 0; i <uFlags.length; i++) {
                String uFlag = uFlags[i];
                if (permFlags.contains(uFlag) == false) {
                    flags.remove(uFlag);
                }
            }
        }
      
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#setFlags(javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Map<Long, Flags> setFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
       
        final SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();

        trimFlags(flags, mailboxSession);
        
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(mailboxSession);
      
        Iterator<UpdatedFlags> it = messageMapper.execute(new Mapper.Transaction<Iterator<UpdatedFlags>>() {

            public Iterator<UpdatedFlags> run() throws MailboxException {
                return messageMapper.updateFlags(getMailboxEntity(),flags, value, replace, set);
            }
        });
        
        final SortedMap<Long, UpdatedFlags> uFlags = new TreeMap<Long, UpdatedFlags>();

        while (it.hasNext()) {
            UpdatedFlags flag = it.next();
            newFlagsByUid.put(flag.getUid(), flag.getNewFlags());
            uFlags.put(flag.getUid(), flag);
        }
        
        dispatcher.flagsUpdated(mailboxSession, new ArrayList<Long>(uFlags.keySet()), new StoreMailboxPath<Id>(getMailboxEntity()), new ArrayList<UpdatedFlags>(uFlags.values()));

        return newFlagsByUid;
    }



    /**
     * Copy the {@link MessageSet} to the {@link MapperStoreMessageManager}
     * 
     * @param set
     * @param toMailbox
     * @param session
     * @throws MailboxException
     */
    public List<MessageRange> copyTo(MessageRange set, StoreMessageManager<Id> toMailbox, MailboxSession session) throws MailboxException {
        try {
            Iterator<Long> copiedUids = copy(set, toMailbox, session);
            List<Long> uids = new ArrayList<Long>();
            while(copiedUids.hasNext()) {
                long uid = copiedUids.next();
                uids.add(uid);
            }
            dispatcher.added(session, uids, new StoreMailboxPath<Id>(toMailbox.getMailboxEntity()));

            return MessageRange.toRanges(uids);
        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }
    
    protected long appendMessageToStore(final Message<Id> message, MailboxSession session) throws MailboxException {
        final MessageMapper<Id> mapper = mapperFactory.getMessageMapper(session);
        return mapperFactory.getMessageMapper(session).execute(new Mapper.Transaction<Long>() {

            public Long run() throws MailboxException {
                return mapper.add(getMailboxEntity(), message);
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
    public Iterator<MessageResult> getMessages(final MessageRange set, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxException {

        class InterceptingCallback implements MessageCallback {
            Iterator<MessageResult> iterator;

            public void onMessages(Iterator<MessageResult> it) throws MailboxException {
                iterator = it;
            }

            public Iterator<MessageResult> getIterator() {
        	if (iterator == null) {
        	    iterator = new ResultIterator<Id>(null, null);
        	}
                return iterator;
            }
        }

        // if we are intercepting callback - let's make it effective
        MessageRange nonBatchedSet = set.getUnlimitedRange();

        // intercepting callback
        InterceptingCallback callback = new InterceptingCallback();
        this.getMessages(nonBatchedSet, fetchGroup, mailboxSession, callback);

        return callback.getIterator();
    }
   
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageManager#getMessages(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MessageResult.FetchGroup, org.apache.james.mailbox.MailboxSession, int, org.apache.james.mailbox.MessageManager.MessageCallback)
     */
    public void getMessages(MessageRange set, final FetchGroup fetchGroup, MailboxSession mailboxSession, final MessageCallback messageCallback) throws MailboxException {

        mapperFactory.getMessageMapper(mailboxSession).findInMailbox(getMailboxEntity(), set, new MailboxMembershipCallback<Id>() {
            public void onMailboxMembers(List<Message<Id>> rows) throws MailboxException {
                messageCallback.onMessages(new ResultIterator<Id>(rows.iterator(), fetchGroup));
            }
        });
    }

    /**
     * Return a List which holds all uids of recent messages and optional reset
     * the recent flag on the messages for the uids
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
                final List<Message<Id>> members = messageMapper.findRecentMessagesInMailbox(getMailboxEntity());
                final List<Long> results = new ArrayList<Long>();

                for (Message<Id> member:members) {
                    results.add(member.getUid());
                    if (reset) {
                        
                        // only call save if we need to
                        messageMapper.updateFlags(getMailboxEntity(), new Flags(Flag.RECENT), false, false, MessageRange.one(member.getUid()));
                    }
                }
                return results;
            }
            
        });
        
    }

    
    protected Iterator<Long> deleteMarkedInMailbox(final MessageRange range, final MailboxSession session) throws MailboxException {
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

        return messageMapper.execute(new Mapper.Transaction<Iterator<Long>>() {

            public Iterator<Long> run() throws MailboxException {
                return messageMapper.expungeMarkedForDeletionInMailbox(getMailboxEntity(), range);
            }
            
        });       
    }




    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#search(org.apache.james.mailbox.SearchQuery, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> search(SearchQuery query, MailboxSession mailboxSession) throws MailboxException {
        MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(mailboxSession);
        List<Criterion> crits = query.getCriterias();

        if (crits.size() == 1  && crits.get(0) instanceof UidCriterion) {
            final List<Long> uids = new ArrayList<Long>();
            UidCriterion uidCrit = (UidCriterion) crits.get(0);
            NumericRange[] ranges = uidCrit.getOperator().getRange();
            for (int i = 0; i < ranges.length; i++) {
                NumericRange r = ranges[i];
                messageMapper.findInMailbox(getMailboxEntity(), MessageRange.range(r.getLowValue(), r.getHighValue()), new MailboxMembershipCallback<Id>() {

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

            messageMapper.findInMailbox(getMailboxEntity(), MessageRange.all(), new MailboxMembershipCallback<Id>() {

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
            
            return new SearchQueryIterator(new Iterator<Message<?>>() {
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
                
            }, query, mailboxSession.getLog());
        }
        
    }


    private Iterator<Long> copy(final List<Message<Id>> originalRows, final MailboxSession session) throws MailboxException {
        try {
            final List<Long> copiedRows = new ArrayList<Long>();
            final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

            for (final Message<Id> originalMessage:originalRows) {
               copiedRows.add(messageMapper.execute(new Mapper.Transaction<Long>() {

                    public Long run() throws MailboxException {
                        long uid = uidProvider.nextUid(session, getMailboxEntity());
                        return messageMapper.copy(getMailboxEntity(), uid, originalMessage);
                        
                    }
                    
                }));
            }
            return copiedRows.iterator();
        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.AbstractStoreMessageManager#copy(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.store.AbstractStoreMessageManager, org.apache.james.mailbox.MailboxSession)
     */
    protected Iterator<Long> copy(MessageRange set, final StoreMessageManager<Id> to, final MailboxSession session) throws MailboxException {
        try {
            MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

            final List<Long> copiedMessages = new ArrayList<Long>();
            messageMapper.findInMailbox(getMailboxEntity(), set, new MailboxMembershipCallback<Id>() {

                public void onMailboxMembers(List<Message<Id>> originalRows) throws MailboxException {
                    Iterator<Long> ids = to.copy(originalRows, session);
                    while (ids.hasNext())
                        copiedMessages.add(ids.next());
                }
            });
            return copiedMessages.iterator();

        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }



    /**
     * Return the count of unseen messages
     * 
     * @param mailbox
     * @param session
     * @return
     * @throws MailboxException
     */
    protected long countUnseenMessagesInMailbox(MailboxSession session) throws MailboxException {
        MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);
        return messageMapper.countUnseenMessagesInMailbox(getMailboxEntity());
    }



    /**
     * Return the uid of the first unseen message or null of none is found
     * 
     * @param mailbox
     * @param session
     * @return uid
     * @throws MailboxException
     */
    protected Long findFirstUnseenMessageUid(MailboxSession session) throws MailboxException{
        MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);
        return messageMapper.findFirstUnseenMessageUid(getMailboxEntity());
    }
}

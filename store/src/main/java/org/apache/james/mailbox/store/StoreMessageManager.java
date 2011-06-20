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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedFileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageMetaData;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.streaming.BodyOffsetInputStream;
import org.apache.james.mailbox.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.message.Header;
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
public class StoreMessageManager<Id> implements org.apache.james.mailbox.MessageManager{

    protected final static Flags MINIMAL_PERMANET_FLAGS;
    static {
        MINIMAL_PERMANET_FLAGS = new Flags();
        MINIMAL_PERMANET_FLAGS.add(Flags.Flag.ANSWERED);
        MINIMAL_PERMANET_FLAGS.add(Flags.Flag.DELETED);
        MINIMAL_PERMANET_FLAGS.add(Flags.Flag.DRAFT);
        MINIMAL_PERMANET_FLAGS.add(Flags.Flag.FLAGGED);
        MINIMAL_PERMANET_FLAGS.add(Flags.Flag.SEEN);
    }

    private final Mailbox<Id> mailbox;
    
    private final MailboxEventDispatcher<Id> dispatcher;    
    
    protected final MessageMapperFactory<Id> mapperFactory;

    protected final MessageSearchIndex<Id> index;
    
    public StoreMessageManager(final MessageMapperFactory<Id> mapperFactory, final MessageSearchIndex<Id> index, final MailboxEventDispatcher<Id> dispatcher, final Mailbox<Id> mailbox) throws MailboxException {
        this.mailbox = mailbox;
        this.dispatcher = dispatcher;
        this.mapperFactory = mapperFactory;
        this.index = index;
    }
    
    
    
    /**
     * Return the {@link MailboxEventDispatcher} for this Mailbox
     * 
     * @return dispatcher
     */
    protected MailboxEventDispatcher<Id> getDispatcher() {
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
    

    /**
     * Return {@link Flags} which are permanent stored by the mailbox. By default this are the following flags:
     * <br>
     *  {@link Flag#ANSWERED}, {@link Flag#DELETED}, {@link Flag#DRAFT}, {@link Flag#FLAGGED}, {@link Flag#RECENT}, {@link Flag#SEEN}
     * <br>
     * 
     * Which in fact does not allow to permanent store user flags / keywords. 
     * 
     * If the sub-class does allow to store "any" user flag / keyword it MUST override this method and add {@link Flag#USER} to the list
     * of returned {@link Flags}. If only a special set of user flags / keywords should be allowed just add them directly.
     * 
     * @param session
     * @return flags
     */
    protected Flags getPermanentFlags(MailboxSession session) {
        return MINIMAL_PERMANET_FLAGS;
    }

    
    

    /**
     * Return true. If an subclass don't want to store mod-sequences in a permanent way just override this
     * and return false
     * 
     * @return true
     */
    public boolean isModSeqPermanent(MailboxSession session) {
        return true;
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#expunge(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        Map<Long, MessageMetaData> uids = deleteMarkedInMailbox(set, mailboxSession);
     
        dispatcher.expunged(mailboxSession, uids, getMailboxEntity());
        return uids.keySet().iterator();    
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#appendMessage(byte[], java.util.Date, org.apache.james.mailbox.MailboxSession, boolean, javax.mail.Flags)
     */
    public long appendMessage(final InputStream msgIn, Date internalDate,
            final MailboxSession mailboxSession,final boolean isRecent, final Flags flagsToBeSet)
    throws MailboxException {
        File file = null;
        TeeInputStream tmpMsgIn = null;
        BodyOffsetInputStream bIn = null;
        FileOutputStream out = null;
        SharedFileInputStream contentIn = null;
        
        try {
            // Create a temporary file and copy the message to it. We will work with the file as
            // source for the InputStream
            file = File.createTempFile("imap", ".msg");
            out = new FileOutputStream(file);
            
            tmpMsgIn = new TeeInputStream(msgIn, out);
           
            bIn = new BodyOffsetInputStream(tmpMsgIn);
            // Disable line length... This should be handled by the smtp server component and not the parser itself
            // https://issues.apache.org/jira/browse/IMAP-122
            MimeEntityConfig config = new MimeEntityConfig();
            config.setMaximalBodyDescriptor(true);
            config.setMaxLineLen(-1);
            final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);
           
            parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
            parser.parse(bIn);
            final Header header = new Header();
            
            int next = parser.next();
            while (next != MimeTokenStream.T_BODY
                    && next != MimeTokenStream.T_END_OF_STREAM
                    && next != MimeTokenStream.T_START_MULTIPART) {
                if (next == MimeTokenStream.T_FIELD) {
                    header.addField(parser.getField());
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
                bodyStream.close();
                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE)  {
                    final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                    epilogueStream.readAll();
                    lines+=epilogueStream.getLineCount();
                    epilogueStream.close();

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
            if (internalDate == null) {
                internalDate = new Date();
            }
            byte[] discard = new byte[4096];
            while(tmpMsgIn.read(discard) != -1) {
                // consume the rest of the stream so everything get copied to the file now
                // via the TeeInputStream
            }
            int bodyStartOctet = (int) bIn.getBodyStartOffset();
            if (bodyStartOctet == -1) {
                bodyStartOctet = 0;
            }
            contentIn = new SharedFileInputStream(file);
            final int size = (int) file.length();

            final Message<Id> message = createMessage(internalDate, size, bodyStartOctet, contentIn, flags, propertyBuilder);
            MessageMetaData data = appendMessageToStore(message, mailboxSession);
                       
            Map<Long, MessageMetaData> uids = new HashMap<Long, MessageMetaData>();
            uids.put(data.getUid(), data);
            dispatcher.added(mailboxSession, uids, getMailboxEntity());
            return data.getUid();
        } catch (IOException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MimeException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        } finally {
            IOUtils.closeQuietly(bIn);
            IOUtils.closeQuietly(tmpMsgIn);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(contentIn);

            // delete the temporary file if one was specified
            if (file != null) {
                file.delete();
            }
        }

    }


    /**
     * Create a new {@link MailboxMembership} for the given data
     * 
     * @param internalDate
     * @param size
     * @param bodyStartOctet
     * @param content
     * @param flags
     * @return membership
     * @throws MailboxException 
     */
    protected Message<Id> createMessage(Date internalDate, final int size, int bodyStartOctet, 
            final SharedInputStream content, final Flags flags, final PropertyBuilder propertyBuilder) throws MailboxException {
        return new SimpleMessage<Id>(internalDate, size, bodyStartOctet, content, flags, propertyBuilder, getMailboxEntity().getMailboxId());
    }
    
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
        final long uidNext = mapperFactory.getMessageMapper(mailboxSession).getLastUid(mailbox) +1;
        final long highestModSeq =  mapperFactory.getMessageMapper(mailboxSession).getHighestModSeq(mailbox);
        final long messageCount; 
        final long unseenCount;
        final Long firstUnseen;
        switch (fetchGroup) {
            case UNSEEN_COUNT:
                unseenCount = countUnseenMessagesInMailbox(mailboxSession);
                messageCount = getMessageCount(mailboxSession);
                firstUnseen = null;
                break;
            case FIRST_UNSEEN:
                firstUnseen = findFirstUnseenMessageUid(mailboxSession);
                messageCount = getMessageCount(mailboxSession); 
                unseenCount = 0;
                break;
            case NO_UNSEEN:
                firstUnseen = null;
                unseenCount = 0;
                messageCount = getMessageCount(mailboxSession);
                break;
            default:
                firstUnseen = null;
                unseenCount = 0;
                messageCount = -1;
                break;
        }
        return new MailboxMetaData(recent, permanentFlags, uidValidity, uidNext,highestModSeq, messageCount, unseenCount, firstUnseen, isWriteable(mailboxSession), isModSeqPermanent(mailboxSession));
    }

 
    /**
     * Check if the given {@link Flags} contains {@link Flags} which are not included in the returned {@link Flags} of {@link #getPermanentFlags(MailboxSession)}.
     * If any are found, these are removed from the given {@link Flags} instance. The only exception is the {@link Flag#RECENT} flag.
     * 
     * This flag is never removed!
     * 
     * @param flags
     * @param session
     */
    private void trimFlags(Flags flags, MailboxSession session) {
        
        Flags permFlags = getPermanentFlags(session);
        
        Flag[] systemFlags = flags.getSystemFlags();
        for (int i = 0; i <  systemFlags.length; i++) {
            Flag f = systemFlags[i];
            
            if (f != Flag.RECENT && permFlags.contains(f) == false) {
                flags.remove(f);
            }
        }
        // if the permFlags contains the special USER flag we can skip this as all user flags are allowed
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
        
        dispatcher.flagsUpdated(mailboxSession, new ArrayList<Long>(uFlags.keySet()), getMailboxEntity(), new ArrayList<UpdatedFlags>(uFlags.values()));

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
            Map<Long, MessageMetaData> copiedUids = copy(set, toMailbox, session);
            dispatcher.added(session, copiedUids, toMailbox.getMailboxEntity());

            return MessageRange.toRanges(new ArrayList<Long>(copiedUids.keySet()));
        } catch (MailboxException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }
    
    protected MessageMetaData appendMessageToStore(final Message<Id> message, MailboxSession session) throws MailboxException {
        final MessageMapper<Id> mapper = mapperFactory.getMessageMapper(session);
        return mapperFactory.getMessageMapper(session).execute(new Mapper.Transaction<MessageMetaData>() {

            public MessageMetaData run() throws MailboxException {
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

        mapperFactory.getMessageMapper(mailboxSession).findInMailbox(getMailboxEntity(), set, new org.apache.james.mailbox.store.mail.MessageMapper.MessageCallback<Id>() {
            public void onMessages(List<Message<Id>> rows) throws MailboxException {
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
                final List<Long> members = messageMapper.findRecentMessageUidsInMailbox(getMailboxEntity());

                // Conver to MessageRanges so we may be able to optimize the flag update
                List<MessageRange> ranges = MessageRange.toRanges(members);
                for (MessageRange range:ranges) {
                    if (reset) {
                        // only call save if we need to
                        messageMapper.updateFlags(getMailboxEntity(), new Flags(Flag.RECENT), false, false, range);
                    }
                }
                return members;
            }
            
        });
        
    }

    
    protected Map<Long, MessageMetaData> deleteMarkedInMailbox(final MessageRange range, final MailboxSession session) throws MailboxException {
        final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

        return messageMapper.execute(new Mapper.Transaction<Map<Long, MessageMetaData>>() {

            public Map<Long, MessageMetaData> run() throws MailboxException {
                return messageMapper.expungeMarkedForDeletionInMailbox(getMailboxEntity(), range);
            }
            
        });       
    }




    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#search(org.apache.james.mailbox.SearchQuery, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> search(SearchQuery query, MailboxSession mailboxSession) throws MailboxException {
        return index.search(mailboxSession, getMailboxEntity(), query);
    }


    private Iterator<MessageMetaData> copy(final List<Message<Id>> originalRows, final MailboxSession session) throws MailboxException {
        try {
            final List<MessageMetaData> copiedRows = new ArrayList<MessageMetaData>();
            final MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

            for (final Message<Id> originalMessage:originalRows) {
               MessageMetaData data = messageMapper.execute(new Mapper.Transaction<MessageMetaData>() {

                    public MessageMetaData run() throws MailboxException {
                        return messageMapper.copy(getMailboxEntity(), originalMessage);
                        
                    }
                    
                });
               copiedRows.add(data);
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
    private Map<Long, MessageMetaData> copy(MessageRange set, final StoreMessageManager<Id> to, final MailboxSession session) throws MailboxException {
        try {
            MessageMapper<Id> messageMapper = mapperFactory.getMessageMapper(session);

            final Map<Long, MessageMetaData> copiedMessages = new HashMap<Long, MessageMetaData>();
            messageMapper.findInMailbox(getMailboxEntity(), set, new org.apache.james.mailbox.store.mail.MessageMapper.MessageCallback<Id>() {

                public void onMessages(List<Message<Id>> originalRows) throws MailboxException {
                    Iterator<MessageMetaData> ids = to.copy(originalRows, session);
                    while (ids.hasNext()) {
                        MessageMetaData data = ids.next();
                        copiedMessages.put(data.getUid(), data);
                    }
                }
            });
            return copiedMessages;

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

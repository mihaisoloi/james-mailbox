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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.util.SharedFileInputStream;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.mailbox.store.streaming.CountingInputStream;
import org.apache.james.mailbox.util.MailboxEventDispatcher;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

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
    
    public StoreMessageManager(final UidProvider<Id> uidProvider, final MailboxEventDispatcher dispatcher, final Mailbox<Id> mailbox) throws MailboxException {
        this.mailbox = mailbox;
        this.dispatcher = dispatcher;
        this.uidProvider = uidProvider;
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
    

    
    private Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    /**
     * Return a List which holds all uids of recent messages and optional reset the recent flag on the messages for the uids
     * 
     * @param reset
     * @param mailboxSession
     * @return list
     * @throws MailboxException
     */
    protected abstract List<Long> recent(final boolean reset, MailboxSession mailboxSession) throws MailboxException;


    protected abstract Iterator<Long> deleteMarkedInMailbox(MessageRange range, MailboxSession session) throws MailboxException;
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#expunge(org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        List<Long> uids = new ArrayList<Long>();
        Iterator<Long> uidIt = deleteMarkedInMailbox(set, mailboxSession);
        while(uidIt.hasNext()) {
            long uid = uidIt.next();
            dispatcher.expunged(uid, mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()));
            uids.add(uid);
        }
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
            }
            if (isRecent) {
                flags.add(Flags.Flag.RECENT);
            }
            final MailboxMembership<Id> message = createMessage(internalDate, size, bodyStartOctet, tmpMsgIn.newStream(0, -1), flags, headers, propertyBuilder);
            long uid = appendMessageToStore(message, mailboxSession);
                        
            dispatcher.added(uid, mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()));
            return uid;
        } catch (IOException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MessagingException e) {
            throw new MailboxException("Unable to parse message", e);
        } catch (MimeException e) {
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
    
    protected abstract long appendMessageToStore(MailboxMembership<Id> message, MailboxSession session) throws MailboxException;

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
    protected abstract MailboxMembership<Id> createMessage(Date internalDate, final int size, int bodyStartOctet, 
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
        final Flags permanentFlags = getPermanentFlags();
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

    /**
     * Return the uid of the first unseen message or null of none is found
     * 
     * @param mailbox
     * @param session
     * @return uid
     * @throws MailboxException
     */
    protected abstract Long findFirstUnseenMessageUid(MailboxSession session) throws MailboxException;

    /**
     * Return the count of unseen messages
     * 
     * @param mailbox
     * @param session
     * @return
     * @throws MailboxException
     */
    protected abstract long countUnseenMessagesInMailbox(MailboxSession session) throws MailboxException;
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox#setFlags(javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange, org.apache.james.mailbox.MailboxSession)
     */
    public Map<Long, Flags> setFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
       
        final SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();

        Iterator<UpdatedFlag> it = updateFlags(flags, value, replace, set, mailboxSession);
        while (it.hasNext()) {
            UpdatedFlag flag = it.next();
            dispatcher.flagsUpdated(flag.getUid(), mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()), flag.getOldFlags(), flag.getNewFlags());
            newFlagsByUid.put(flag.getUid(), flag.getNewFlags());
        }
        return newFlagsByUid;
    }

    /**
     * Update the Flags for the given {@link MessageRange} 
     * 
     * @param flags
     * @param value
     * @param replace
     * @param set
     * @param mailboxSession
     * @return
     * @throws MailboxException
     */
    protected abstract Iterator<UpdatedFlag> updateFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException;



    /**
     * Copy the {@link MessageSet} to the {@link MapperStoreMessageManager}
     * 
     * @param set
     * @param toMailbox
     * @param session
     * @throws MailboxException
     */
    public void copyTo(MessageRange set, StoreMessageManager<Id> toMailbox, MailboxSession session) throws MailboxException {
        try {
            Iterator<Long> copiedUids = copy(set, toMailbox, session);
            long highest = 0;
            while(copiedUids.hasNext()) {
                long uid = copiedUids.next();
                if (highest < uid) {
                    highest = uid;
                }
                dispatcher.added(uid, session.getSessionId(), new StoreMailboxPath<Id>(toMailbox.getMailboxEntity()));
            }
        } catch (MessagingException e) {
            throw new MailboxException("Unable to parse message", e);
        }
    }
    
    
    /**
     * Copy the messages in the given {@link MessageRange} to the given {@link StoreMessageManager} and return a {@link Iterator} which
     * holds the uids of the copied messages
     * 
     * @param range
     * @param toMailbox
     * @param session
     * @return uids
     * @throws MailboxException
     */
    protected abstract Iterator<Long> copy(MessageRange range, StoreMessageManager<Id> toMailbox, MailboxSession session) throws MailboxException;
}

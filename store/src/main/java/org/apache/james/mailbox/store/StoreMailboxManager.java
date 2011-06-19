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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxMetaData;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxQuery;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.RequestAware;
import org.apache.james.mailbox.StandardMailboxMetaDataComparator;
import org.apache.james.mailbox.MailboxMetaData.Selectability;
import org.apache.james.mailbox.MailboxPathLocker.LockAwareExecution;
import org.apache.james.mailbox.MailboxSession.SessionType;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.SimpleMailbox;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;
import org.slf4j.Logger;

/**
 * This abstract base class of an {@link MailboxManager} implementation provides a high-level api for writing your own
 * {@link MailboxManager} implementation. If you plan to write your own {@link MailboxManager} its most times so easiest 
 * to extend just this class.
 * 
 * If you need a more low-level api just implement {@link MailboxManager} directly
 *
 * @param <Id>
 */
public abstract class StoreMailboxManager<Id> implements MailboxManager {
    
    public static final char SQL_WILDCARD_CHAR = '%';
    
    private final MailboxEventDispatcher<Id> dispatcher = new MailboxEventDispatcher<Id>();
    private AbstractDelegatingMailboxListener delegatingListener = null;  
    private final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;    
    
    private final Authenticator authenticator;
    private final static Random RANDOM = new Random();
    

    private MailboxPathLocker locker;

    private MessageSearchIndex<Id> index;

    
    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator, final MailboxPathLocker locker) {
        this.authenticator = authenticator;
        this.locker = locker;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;       
    }
   
    /**
     * Init the {@link MailboxManager}
     * 
     * @throws MailboxException
     */
    @SuppressWarnings("rawtypes")
    public void init() throws MailboxException{
        // The dispatcher need to have the delegating listener added
        dispatcher.addMailboxListener(getDelegationListener());
        
        if (index == null) {
            index = new SimpleMessageSearchIndex<Id>(mailboxSessionMapperFactory);
        }
        if (index instanceof ListeningMessageSearchIndex) {
            addGlobalListener((ListeningMessageSearchIndex) index, null);
        }
    }
    
    public AbstractDelegatingMailboxListener getDelegationListener() {
        if (delegatingListener == null) {
            delegatingListener = new HashMapDelegatingMailboxListener();
        }
        return delegatingListener;
    }
    
    public MessageSearchIndex<Id> getMessageSearchIndex() {
        return index;
    }
    
    public MailboxEventDispatcher<Id> getEventDispatcher() {
        return dispatcher;
    }
    
    public MailboxSessionMapperFactory<Id> getMapperFactory() {
        return mailboxSessionMapperFactory;
    }
    
    
    /**
     * Set the {@link AbstractDelegatingMailboxListener} to use with this {@link MailboxManager} instance. If none is set here a {@link HashMapDelegatingMailboxListener} instance will
     * be created lazy
     * 
     * @param delegatingListener
     */
    public void setDelegatingMailboxListener(AbstractDelegatingMailboxListener delegatingListener) {
    	if(this.delegatingListener != null)
    		this.delegatingListener.close();
        this.delegatingListener = delegatingListener;
        dispatcher.addMailboxListener(this.delegatingListener);
    }
    
    public void setMessageSearchIndex(MessageSearchIndex<Id> index) {
        this.index = index;
    }

    /**
     * Generate an return the next uid validity 
     * 
     * @return uidValidity
     */
    protected int randomUidValidity() {
        return Math.abs(RANDOM.nextInt());
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#createSystemSession(java.lang.String, org.slf4j.Logger)
     */
    public MailboxSession createSystemSession(String userName, Logger log) {
        return createSession(userName, null, log, SessionType.System);
    }

    /**
     * Create Session 
     * 
     * @param userName
     * @param log
     * @return session
     */
    private MailboxSession createSession(String userName, String password, Logger log, SessionType type) {
        return new SimpleMailboxSession(randomId(), userName, password, log, new ArrayList<Locale>(), getDelimiter(), type);
    }

    /**
     * Generate and return the next id to use
     * 
     * @return id
     */
    protected long randomId() {
        return RANDOM.nextLong();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#getDelimiter()
     */
    public char getDelimiter() {
        return MailboxConstants.DEFAULT_DELIMITER;
    }

    /**
     * Log in the user with the given userid and password
     * 
     * @param userid the username
     * @param passwd the password
     * @return success true if login success false otherwise
     */
    private boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.slf4j.Logger)
     */
    public MailboxSession login(String userid, String passwd, Logger log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, passwd, log, SessionType.User);
        } else {
            throw new BadCredentialsException();
        }
    }
    
    /**
     * Close the {@link MailboxSession} if not null
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        if (session != null) {
            session.close();
        }
    }
  
    /**
     * Create a {@link MapperStoreMessageManager} for the given Mailbox
     * 
     * @param mailboxRow
     * @return storeMailbox
     */
    protected abstract StoreMessageManager<Id> createMessageManager(Mailbox<Id> mailboxRow, MailboxSession session) throws MailboxException;

    /**
     * Create a Mailbox for the given namespace
     * 
     * @param namespaceName
     * @throws MailboxException
     */
    protected org.apache.james.mailbox.store.mail.model.Mailbox<Id> doCreateMailbox(MailboxPath mailboxPath, final MailboxSession session) throws MailboxException {
        return new SimpleMailbox<Id>(mailboxPath, randomUidValidity(), 0, 0);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#getMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public org.apache.james.mailbox.MessageManager getMailbox(MailboxPath mailboxPath, MailboxSession session)
    throws MailboxException {
    	final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        Mailbox<Id> mailboxRow = mapper.findMailboxByPath(mailboxPath);

        if (mailboxRow == null) {
            session.getLog().info("Mailbox '" + mailboxPath + "' not found.");
            throw new MailboxNotFoundException(mailboxPath);

        } else {
            session.getLog().debug("Loaded mailbox " + mailboxPath);
            
            StoreMessageManager<Id>  m = createMessageManager(mailboxRow, session);
            return m;
        }
    }

    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#createMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void createMailbox(MailboxPath mailboxPath, final MailboxSession mailboxSession)
    throws MailboxException {
        mailboxSession.getLog().debug("createMailbox " + mailboxPath);
        final int length = mailboxPath.getName().length();
        if (length == 0) {
            mailboxSession.getLog().warn("Ignoring mailbox with empty name");
        } else {
            if (mailboxPath.getName().charAt(length - 1) == getDelimiter())
                mailboxPath.setName(mailboxPath.getName().substring(0, length - 1));
            if (mailboxExists(mailboxPath, mailboxSession))
                throw new MailboxExistsException(mailboxPath.toString());
            // Create parents first
            // If any creation fails then the mailbox will not be created
            // TODO: transaction
            for (final MailboxPath mailbox : mailboxPath.getHierarchyLevels(getDelimiter()))

                locker.executeWithLock(mailboxSession, mailbox, new LockAwareExecution<Void>() {

                    public Void execute() throws MailboxException {
                        if (!mailboxExists(mailbox, mailboxSession)) {
                            final org.apache.james.mailbox.store.mail.model.Mailbox<Id> m = doCreateMailbox(mailbox, mailboxSession);
                            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
                            mapper.execute(new TransactionalMapper.VoidTransaction() {

                                public void runVoid() throws MailboxException {
                                    mapper.save(m);
                                }

                            });
                            
                            // notify listeners
                            dispatcher.mailboxAdded(mailboxSession, m);
                        }
                        return null;

                    }
                });

        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#deleteMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void deleteMailbox(final MailboxPath mailboxPath, final MailboxSession session) throws MailboxException {
        session.getLog().info("deleteMailbox " + mailboxPath);
        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);

        Mailbox<Id> mailbox = mapper.execute(new Mapper.Transaction<Mailbox<Id>>() {

            public Mailbox<Id> run() throws MailboxException {
                final Mailbox<Id> mailbox = mapper.findMailboxByPath(mailboxPath);
                if (mailbox == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }
                
                // We need to create a copy of the mailbox as maybe we can not refer to the real
                // mailbox once we remove it 
                Mailbox<Id> m = new Mailbox<Id>() {
                    private Id id = mailbox.getMailboxId();
                    private String namespace = mailbox.getNamespace();
                    private String name = mailbox.getName();
                    private String user = mailbox.getUser();
                    private long uidVal = mailbox.getUidValidity();
                    private long lastUid = mailbox.getLastKnownUid();
                    private long lastSeq = mailbox.getHighestKnownModSeq();
                    
                    
                    public Id getMailboxId() {
                        return id;
                    }

                    @Override
                    public String getNamespace() {
                        return namespace;
                    }

                    @Override
                    public void setNamespace(String namespace) {
                        this.namespace = namespace;
                    }

                    @Override
                    public String getUser() {
                        return user;
                    }

                    @Override
                    public void setUser(String user) {
                        this.user = user;
                    }

                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public void setName(String name) {
                        this.name = name;
                        
                    }

                    @Override
                    public long getUidValidity() {
                        return uidVal;
                    }

                    @Override
                    public long getLastKnownUid() {
                        return lastUid;
                    }

                    @Override
                    public long getHighestKnownModSeq() {
                        return lastSeq;
                    }
                    
                };
                mapper.delete(mailbox);
                return m;
            }

        });

        dispatcher.mailboxDeleted(session, mailbox);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#renameMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void renameMailbox(final MailboxPath from, final MailboxPath to, final MailboxSession session) throws MailboxException {
        final Logger log = session.getLog();
        if (log.isDebugEnabled())
            log.debug("renameMailbox " + from + " to " + to);
        if (mailboxExists(to, session)) {
            throw new MailboxExistsException(to.toString());
        }

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        mapper.execute(new Mapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                // TODO put this into a serilizable transaction
                final Mailbox<Id> mailbox = mapper.findMailboxByPath(from);
                if (mailbox == null) {
                    throw new MailboxNotFoundException(from);
                }
                mailbox.setNamespace(to.getNamespace());
                mailbox.setUser(to.getUser());
                mailbox.setName(to.getName());
                mapper.save(mailbox);

                dispatcher.mailboxRenamed(session, from, mailbox);

                // rename submailboxes
                final MailboxPath children = new MailboxPath(MailboxConstants.USER_NAMESPACE, from.getUser(), from.getName() + getDelimiter() + "%");
                locker.executeWithLock(session, children, new LockAwareExecution<Void>() {
                    
                    public Void execute() throws MailboxException {
                        final List<Mailbox<Id>> subMailboxes = mapper.findMailboxWithPathLike(children);
                        for (Mailbox<Id> sub : subMailboxes) {
                            final String subOriginalName = sub.getName();
                            final String subNewName = to.getName() + subOriginalName.substring(from.getName().length());
                            final MailboxPath fromPath = new MailboxPath(children, subOriginalName);
                            sub.setName(subNewName);
                            mapper.save(sub);
                            dispatcher.mailboxRenamed(session, fromPath, sub);

                            if (log.isDebugEnabled())
                                log.debug("Rename mailbox sub-mailbox " + subOriginalName + " to " + subNewName);
                        }
                        return null;

                    }
                });
    
               
                
            }

        });

    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#copyMessages(org.apache.james.mailbox.MessageRange, org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    @SuppressWarnings("unchecked")
	public List<MessageRange> copyMessages(MessageRange set, MailboxPath from, MailboxPath to, MailboxSession session) throws MailboxException {
        StoreMessageManager<Id> toMailbox = (StoreMessageManager<Id>) getMailbox(to, session);
        StoreMessageManager<Id> fromMailbox = (StoreMessageManager<Id>) getMailbox(from, session);
        return fromMailbox.copyTo(set, toMailbox, session);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#search(org.apache.james.mailbox.MailboxQuery, org.apache.james.mailbox.MailboxSession)
     */
    public List<MailboxMetaData> search(final MailboxQuery mailboxExpression, MailboxSession session)
    throws MailboxException {
        final char localWildcard = mailboxExpression.getLocalWildcard();
        final char freeWildcard = mailboxExpression.getFreeWildcard();
        final String baseName = mailboxExpression.getBase().getName();
        final int baseLength;
        if (baseName == null) {
            baseLength = 0;
        } else {
            baseLength = baseName.length();
        }
        final String combinedName = mailboxExpression.getCombinedName()
                                    .replace(freeWildcard, SQL_WILDCARD_CHAR)
                                    .replace(localWildcard, SQL_WILDCARD_CHAR);
        final MailboxPath search = new MailboxPath(mailboxExpression.getBase(), combinedName);

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        final List<Mailbox<Id>> mailboxes = mapper.findMailboxWithPathLike(search);
        final List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxes.size());
        for (Mailbox<Id> mailbox: mailboxes) {
            final String name = mailbox.getName();
            if (name.startsWith(baseName)) {
                final String match = name.substring(baseLength);
                if (mailboxExpression.isExpressionMatch(match)) {
                    final MailboxMetaData.Children inferiors; 
                    if (mapper.hasChildren(mailbox, session.getPathDelimiter())) {
                        inferiors = MailboxMetaData.Children.HAS_CHILDREN;
                    } else {
                        inferiors = MailboxMetaData.Children.HAS_NO_CHILDREN;
                    }
                    MailboxPath mailboxPath = new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), name);
                    results.add(new SimpleMailboxMetaData(mailboxPath, getDelimiter(), inferiors, Selectability.NONE));
                }
            }
        }
        Collections.sort(results, new StandardMailboxMetaDataComparator());
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#mailboxExists(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public boolean mailboxExists(MailboxPath mailboxPath, MailboxSession session) throws MailboxException {
        try {
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            mapper.findMailboxByPath(mailboxPath);
            return true;
        } catch (MailboxNotFoundException e) {
            return false;
        }

    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#addListener(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxListener, org.apache.james.mailbox.MailboxSession)
     */
    public void addListener(MailboxPath path, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addListener(path, listener, session);
    }

     /**
     * End processing of Request for session
     */
    public void endProcessingRequest(MailboxSession session) {
        if (mailboxSessionMapperFactory instanceof RequestAware) {
            ((RequestAware)mailboxSessionMapperFactory).endProcessingRequest(session);
        }
    }

    /**
     * Do nothing. Sub classes should override this if needed
     */
    public void startProcessingRequest(MailboxSession session) {
        // do nothing
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#list(org.apache.james.mailbox.MailboxSession)
     */
    public List<MailboxPath> list(MailboxSession session) throws MailboxException {
        List<MailboxPath> mList = new ArrayList<MailboxPath>();
        List<Mailbox<Id>> mailboxes = mailboxSessionMapperFactory.getMailboxMapper(session).list();
        for (int i = 0; i < mailboxes.size(); i++) {
            Mailbox<Id> m = mailboxes.get(i);
            mList.add(new MailboxPath(m.getNamespace(), m.getUser(), m.getName()));
        }
        return Collections.unmodifiableList(mList);
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListenerSupport#addGlobalListener(org.apache.james.mailbox.MailboxListener, org.apache.james.mailbox.MailboxSession)
     */
    public void addGlobalListener(MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addGlobalListener(listener, session);
    }
    
    

}

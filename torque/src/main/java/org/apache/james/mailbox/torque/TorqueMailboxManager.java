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

package org.apache.james.mailbox.torque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxMetaData;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxQuery;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.StandardMailboxMetaDataComparator;
import org.apache.james.mailbox.MailboxMetaData.Selectability;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.SimpleMailboxSession;
import org.apache.james.mailbox.torque.om.MailboxRow;
import org.apache.james.mailbox.torque.om.MailboxRowPeer;
import org.apache.james.mailbox.util.SimpleMailboxMetaData;
import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

/**
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class TorqueMailboxManager implements MailboxManager {
    
    private static final char SQL_WILDCARD_CHAR = '%';

    private final ReentrantReadWriteLock lock;

    private final Map<String, TorqueMailbox> mailboxes;

    
    public TorqueMailboxManager(final Authenticator authenticator) {
        this.authenticator = authenticator;
        this.lock = new ReentrantReadWriteLock();
        mailboxes = new HashMap<String, TorqueMailbox>();
    }

    
    private final Authenticator authenticator;
    private final static Random RANDOM = new Random();

    private Log log = LogFactory.getLog("org.apache.james.imap");

    
    protected Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
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
     * @see org.apache.james.mailbox.MailboxManager#createSystemSession(java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession createSystemSession(String userName, Log log) {
        return createSession(userName, null, log);
    }

    /**
     * Create Session 
     * 
     * @param userName
     * @param log
     * @return session
     */
    private SimpleMailboxSession createSession(String userName, String password, Log log) {
        return new SimpleMailboxSession(randomId(), userName, password, log, new ArrayList<Locale>());
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
    public final char getDelimiter() {
        return MailboxConstants.DEFAULT_DELIMITER;
    }

    /**
     * Log in the user with the given userid and password
     * 
     * @param userid the username
     * @param passwd the password
     * @return success true if login success false otherwise
     */
    public boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, passwd, log);
        } else {
            throw new BadCredentialsException();
        }
    }
    
    /**
     * Default do nothing. Should be overridden by subclass if needed
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        // Do nothing by default
    }
    public MessageManager getMailbox(MailboxPath path, MailboxSession session)
            throws MailboxException {
        return doGetMailbox(getName(path));
    }

    private String getName(MailboxPath path) {
        StringBuffer sb = new StringBuffer();
        sb.append(path.getNamespace());
        sb.append(MailboxConstants.DEFAULT_DELIMITER);

        if (path.getUser() != null) {
            sb.append(path.getUser());
            sb.append(MailboxConstants.DEFAULT_DELIMITER);
        }
        sb.append(path.getName());
        return sb.toString();
    }
    private TorqueMailbox doGetMailbox(String mailboxName)
            throws MailboxException {
        try {
            synchronized (mailboxes) {
                MailboxRow mailboxRow = MailboxRowPeer
                        .retrieveByName(mailboxName);

                if (mailboxRow != null) {
                    getLog().debug("Loaded mailbox " + mailboxName);

                    TorqueMailbox torqueMailbox = (TorqueMailbox) mailboxes
                            .get(mailboxName);
                    if (torqueMailbox == null) {
                        torqueMailbox = new TorqueMailbox(mailboxRow, lock);
                        mailboxes.put(mailboxName, torqueMailbox);
                    }

                    return torqueMailbox;
                } else {
                    getLog().info("Mailbox '" + mailboxName + "' not found.");
                    throw new MailboxNotFoundException(mailboxName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException("parsing of message failed");
        }
    }

    public void createMailbox(MailboxPath path, MailboxSession mailboxSession)
            throws MailboxException {
        String namespaceName = getName(path);
        createMailbox(namespaceName, mailboxSession);
        
    }
    
    private void createMailbox(String namespaceName, MailboxSession mailboxSession) throws MailboxException {
        getLog().debug("createMailbox " + namespaceName);
        final int length = namespaceName.length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        } else if (namespaceName.charAt(length - 1) == MailboxConstants.DEFAULT_DELIMITER) {
            createMailbox(namespaceName.substring(0, length - 1), mailboxSession);
        } else {
            synchronized (mailboxes) {
                // Create root first
                // If any creation fails then mailbox will not be created
                // TODO: transaction
                int index = namespaceName.indexOf(MailboxConstants.DEFAULT_DELIMITER);
                int count = 0;
                while (index >= 0) {
                    // Until explicit namespace support is added,
                    // this workaround prevents the namespaced elements being
                    // created
                    // TODO: add explicit support for namespaces
                    if (index > 0 && count++ > 1) {
                        final String mailbox = namespaceName
                                .substring(0, index);
                        if (!mailboxExists(getMailboxPath(mailbox), mailboxSession)) {
                            doCreate(mailbox);
                        }
                    }
                    index = namespaceName.indexOf(MailboxConstants.DEFAULT_DELIMITER, ++index);
                }
                if (mailboxExists(getMailboxPath(namespaceName), mailboxSession)) {
                    throw new MailboxExistsException(namespaceName); 
                } else {
                    doCreate(namespaceName);
                }
            }
        }
    }

    private void doCreate(String namespaceName) throws MailboxException {
        MailboxRow mr = new MailboxRow();
        mr.setName(namespaceName);
        mr.setLastUid(0);
        mr.setUidValidity(randomUidValidity());
        try {
            mr.save();
        } catch (TorqueException e) {
            throw new MailboxException("save failed");
        }
    }

    public void deleteMailbox(MailboxPath path, MailboxSession session)
            throws MailboxException {
        String mailboxName = getName(path);

        getLog().info("deleteMailbox " + mailboxName);
        synchronized (mailboxes) {
            try {
                // TODO put this into a serilizable transaction
                MailboxRow mr = MailboxRowPeer.retrieveByName(mailboxName);
                if (mr == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }
                MailboxRowPeer.doDelete(mr);
                TorqueMailbox mailbox = (TorqueMailbox) mailboxes
                        .remove(mailboxName);
                if (mailbox != null) {
                    mailbox.deleted(session);
                }
            } catch (TorqueException e) {
                throw new MailboxException("delete failed");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void renameMailbox(MailboxPath fromPath, MailboxPath toPath, MailboxSession session)
            throws MailboxException {
        String from = getName(fromPath);
        String to = getName(toPath);

        getLog().debug("renameMailbox " + from + " to " + to);
        try {
            synchronized (mailboxes) {
                if (mailboxExists(toPath, session)) {
                    throw new MailboxExistsException(to);
                }
                // TODO put this into a serilizable transaction
                final MailboxRow mr;

                mr = MailboxRowPeer.retrieveByName(from);

                if (mr == null) {
                    throw new MailboxNotFoundException(from);
                }
                mr.setName(to);
                mr.save();

                changeMailboxName(from, to, mr, session);

                // rename submailbox
                Criteria c = new Criteria();
                c.add(MailboxRowPeer.NAME,
                        (Object) (from + MailboxConstants.DEFAULT_DELIMITER + "%"),
                        Criteria.LIKE);
                List l = MailboxRowPeer.doSelect(c);
                for (Iterator iter = l.iterator(); iter.hasNext();) {
                    MailboxRow sub = (MailboxRow) iter.next();
                    String subOrigName = sub.getName();
                    String subNewName = to + subOrigName.substring(from.length());
                    sub.setName(subNewName);
                    sub.save();
                    changeMailboxName(subOrigName, subNewName, sub, session);
                    getLog().info(
                            "renameMailbox sub-mailbox " + subOrigName + " to "
                                    + subNewName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException("save failed");
        }
    }

    private void changeMailboxName(String from, String to, final MailboxRow mr, MailboxSession session) {
        TorqueMailbox torqueMailbox = (TorqueMailbox) mailboxes.remove(from);
        if (torqueMailbox != null) {
            torqueMailbox.reportRenamed(from, mr, session);
            mailboxes.put(to, torqueMailbox);
        }
    }

    public void copyMessages(MessageRange set, MailboxPath from, MailboxPath to,
            MailboxSession session) throws MailboxException {
        TorqueMailbox toMailbox = doGetMailbox(getName(to));
        TorqueMailbox fromMailbox = doGetMailbox(getName(from));
        fromMailbox.copyTo(set, toMailbox, session);
    }

    @SuppressWarnings("unchecked")
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

        Criteria criteria = new Criteria();
        criteria.add(MailboxRowPeer.NAME, (Object) getName(search), Criteria.LIKE);
        try {
            List mailboxRows = MailboxRowPeer.doSelect(criteria);
            List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxRows.size());
            for (Iterator iter = mailboxRows.iterator(); iter.hasNext();) {
                final MailboxRow mailboxRow = (MailboxRow) iter.next();
                final MailboxPath sPath = getMailboxPath(mailboxRow.getName());
                final String name = sPath.getName();
                if (name.startsWith(baseName)) {
                    final String match = name.substring(baseLength);
                    if (mailboxExpression.isExpressionMatch(match)) {
                        final MailboxMetaData.Children inferiors; 
                        if (hasChildren(mailboxRow.getName())) {
                            inferiors = MailboxMetaData.Children.HAS_CHILDREN;
                        } else {
                            inferiors = MailboxMetaData.Children.HAS_NO_CHILDREN;
                        }
                        results.add(new SimpleMailboxMetaData(sPath, MailboxConstants.DEFAULT_DELIMITER_STRING, inferiors, Selectability.NONE));
                    }
                }
            }
            Collections.sort(results, new StandardMailboxMetaDataComparator());
            return results;
        } catch (TorqueException e) {
            throw new MailboxException("search failed");
        }

    }

    /**
     * Does the mailbox with the given name have inferior child mailboxes?
     * @param name not null
     * @return true when the mailbox has children, false otherwise
     * @throws TorqueException
     */
    @SuppressWarnings("unchecked")
    private boolean hasChildren(String name) throws TorqueException {
        final Criteria criteria = new Criteria();
        criteria.add(MailboxRowPeer.NAME, (Object)(name + MailboxConstants.DEFAULT_DELIMITER + SQL_WILDCARD_CHAR), Criteria.LIKE);
        final List mailboxes = MailboxRowPeer.doSelect(criteria);
        return !mailboxes.isEmpty();
    }
    
    public boolean mailboxExists(MailboxPath path, MailboxSession session)
            throws MailboxException {
        Criteria c = new Criteria();
        String mailboxName = getName(path);
        c.add(MailboxRowPeer.NAME, mailboxName);
        CountHelper countHelper = new CountHelper();
        int count;
        try {
            synchronized (mailboxes) {
                count = countHelper.count(c);
                if (count == 0) {
                    mailboxes.remove(mailboxName);
                    return false;
                } else {
                    if (count == 1) {
                        return true;
                    } else {
                        throw new MailboxException("found " + count + " mailboxes");
                    }
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException("search failed");
        }
    }

    public MailboxPath getMailboxPath(String name) {
        String nameParts[] = name.split("\\" +MailboxConstants.DEFAULT_DELIMITER_STRING,3);
        if (nameParts.length < 3) {
            return new MailboxPath(nameParts[0], null, nameParts[1]);
        }
        return new MailboxPath(nameParts[0], nameParts[1], nameParts[2]);

    }
    public void deleteEverything() throws MailboxException {
        try {
            MailboxRowPeer.doDelete(new Criteria().and(
                    MailboxRowPeer.MAILBOX_ID, new Integer(-1),
                    Criteria.GREATER_THAN));
            mailboxes.clear();
        } catch (TorqueException e) {
            throw new MailboxException("save failed");
        }
    }

    public void addListener(MailboxPath path, MailboxListener listener, MailboxSession session) throws MailboxException {
        final TorqueMailbox mailbox = doGetMailbox(getName(path));
        mailbox.addListener(listener);
    }

    public void endProcessingRequest(MailboxSession session) {
        
    }

    public void startProcessingRequest(MailboxSession session) {
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#list(org.apache.james.mailbox.MailboxSession)
     */
    public List<MailboxPath> list(MailboxSession session) throws MailboxException {
        Criteria c = new Criteria();
        c.setAll();
        try {
            List<MailboxPath> pList = new ArrayList<MailboxPath>();
            List mailboxes = MailboxRowPeer.doSelect(c);
            for (int i = 0; i < mailboxes.size(); i++) {
                final MailboxRow mailboxRow = (MailboxRow) mailboxes.get(i);
                final MailboxPath sPath = getMailboxPath(mailboxRow.getName());
                pList.add(sPath);
            }
            return pList;
        } catch (TorqueException e) {
            throw new MailboxException("select of all mailboxes failed", e);

        }
    }


}

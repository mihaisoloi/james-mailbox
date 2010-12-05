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
package org.apache.james.mailbox.jcr.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageRange.Type;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.jcr.AbstractJCRMapper;
import org.apache.james.mailbox.jcr.MailboxSessionJCRRepository;
import org.apache.james.mailbox.jcr.mail.model.JCRMessage;
import org.apache.james.mailbox.store.SearchQueryIterator;
import org.apache.james.mailbox.store.UpdatedFlag;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;

/**
 * JCR implementation of a {@link MessageMapper}. The implementation store each message as 
 * a seperate child node under the mailbox
 *
 */
public class JCRMessageMapper extends AbstractJCRMapper implements MessageMapper<String> {

    private UidProvider<String> uidGenerator;
    
    /**
     * Store the messages directly in the mailbox: .../mailbox/
     */
    public final static int MESSAGE_SCALE_NONE = 0;

    /**
     * Store the messages under a year directory in the mailbox:
     * .../mailbox/2010/
     */
    public final static int MESSAGE_SCALE_YEAR = 1;

    /**
     * Store the messages under a year/month directory in the mailbox:
     * .../mailbox/2010/05/
     */
    public final static int MESSAGE_SCALE_MONTH = 2;

    /**
     * Store the messages under a year/month/day directory in the mailbox:
     * .../mailbox/2010/05/01/
     */
    public final static int MESSAGE_SCALE_DAY = 3;

    /**
     * Store the messages under a year/month/day/hour directory in the mailbox:
     * .../mailbox/2010/05/02/11
     */
    public final static int MESSAGE_SCALE_HOUR = 4;

    /**
     * Store the messages under a year/month/day/hour/min directory in the
     * mailbox: .../mailbox/2010/05/02/11/59
     */
    public final static int MESSAGE_SCALE_MINUTE = 5;

    private final int scaleType;

    /**
     * Construct a new {@link JCRMessageMapper} instance
     * 
     * @param repos {@link MailboxSessionJCRRepository} to use
     * @param session {@link MailboxSession} to which the mapper is bound
     * @param logger Log
     */
    public JCRMessageMapper(final MailboxSessionJCRRepository repos, MailboxSession session, final UidProvider<String> uidGenerator, final Log logger, int scaleType) {
        super(repos, session, logger);
        this.uidGenerator = uidGenerator;
        this.scaleType = scaleType;
    }
    
    public JCRMessageMapper(final MailboxSessionJCRRepository repos, MailboxSession session, final UidProvider<String> uidGenerator, final Log logger) {
        this(repos, session, uidGenerator, logger, MESSAGE_SCALE_DAY);
    }
    
    /**
     * Return the path to the mailbox. This path is escaped to be able to use it in xpath queries
     * 
     * See http://wiki.apache.org/jackrabbit/EncodingAndEscaping
     * 
     * @param mailbox
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private String getMailboxPath(Mailbox<String> mailbox) throws ItemNotFoundException, RepositoryException {
        return ISO9075.encodePath(getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath());
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<String> mailbox) throws MailboxException {
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message) order by @" + JCRMessage.UID_PROPERTY;
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator nodes = result.getNodes();
            long count = nodes.getSize();
            if (count == -1) {
                count = 0;
                while(nodes.hasNext()) {
                    nodes.nextNode();
                    count++;
                }
            } 
            return count;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to count messages in mailbox " + mailbox, e);
        }
       
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MessageMapper#countUnseenMessagesInMailbox
     * ()
     */
    public long countUnseenMessagesInMailbox(Mailbox<String> mailbox) throws MailboxException {
        
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator nodes = result.getNodes();
            long count = nodes.getSize();
            
            if (count == -1) {
                count = 0;
                while(nodes.hasNext()) {
                    nodes.nextNode();
                    
                    count++;
                }
            } 
            return count;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to count unseen messages in mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<String> mailbox, MailboxMembership<String> message) throws MailboxException {
        JCRMessage membership = (JCRMessage) message;
        if (membership.isPersistent()) {
            try {

                getSession().getNodeByIdentifier(membership.getId()).remove();
            } catch (RepositoryException e) {
                throw new MailboxException("Unable to delete message " + message + " in mailbox " + mailbox, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MessageMapper#findInMailbox(org.apache
     * .james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<String>> findInMailbox(Mailbox<String> mailbox, MessageRange set) throws MailboxException {
        try {
            final List<MailboxMembership<String>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findMessagesInMailbox(mailbox);
                    break;
                case FROM:
                    results = findMessagesInMailboxAfterUID(mailbox, from);
                    break;
                case ONE:
                    results = findMessageInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to search MessageRange " + set + " in mailbox " + mailbox, e);
        }
    }

    private List<MailboxMembership<String>> findMessagesInMailboxAfterUID(Mailbox<String> mailbox, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + uid + "] order by @" + JCRMessage.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessageInMailboxWithUID(Mailbox<String> mailbox, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + "=" + uid + "]";

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query query = manager.createQuery(queryString, Query.XPATH);
        query.setLimit(1);
        QueryResult result = query.execute();
        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxBetweenUIDs(Mailbox<String> mailbox, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + from + " and @" + JCRMessage.UID_PROPERTY + "<=" + to + "] order by @" + JCRMessage.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findMessagesInMailbox(Mailbox<String> mailbox) throws RepositoryException {        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message) order by @" + JCRMessage.UID_PROPERTY;
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailboxAfterUID(Mailbox<String> mailbox, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + uid + " and @" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
 
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessageInMailboxWithUID(Mailbox<String> mailbox, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + "=" + uid + " and @" + JCRMessage.DELETED_PROPERTY+ "='true']";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query query = manager.createQuery(queryString, Query.XPATH);
        query.setLimit(1);
        QueryResult result = query.execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMessage member = new JCRMessage(iterator.nextNode(), getLogger());
            list.add(member);
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxBetweenUIDs(Mailbox<String> mailbox, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + from + " and @" + JCRMessage.UID_PROPERTY + "<=" + to + " and @" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
       
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailbox(Mailbox<String> mailbox) throws RepositoryException {
        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMessage member = new JCRMessage(iterator.nextNode(), getLogger());
            list.add(member);
        }
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MessageMapper#findMarkedForDeletionInMailbox
     * (org.apache.james.mailbox.MessageRange)
     */
    public List<MailboxMembership<String>> findMarkedForDeletionInMailbox(Mailbox<String> mailbox, MessageRange set) throws MailboxException {
        try {
            final List<MailboxMembership<String>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(mailbox);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(mailbox, from);
                    break;
                case ONE:
                    results = findDeletedMessageInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to search MessageRange " + set + " in mailbox " + mailbox, e);
        }
    }

    /*
     * 
     * TODO: Maybe we should better use an ItemVisitor and just traverse through the child nodes. This could be a way faster
     * 
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox
     * ()
     */
    public List<MailboxMembership<String>> findRecentMessagesInMailbox(Mailbox<String> mailbox) throws MailboxException {
        
        try {
 
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.RECENT_PROPERTY +"='true'] order by @" + JCRMessage.UID_PROPERTY;
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            Query query = manager.createQuery(queryString, Query.XPATH);
            QueryResult result = query.execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMessage(iterator.nextNode(), getLogger()));
            }
            return list;

        } catch (RepositoryException e) {
            throw new MailboxException("Unable to search recent messages in mailbox " + mailbox, e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<String> mailbox) throws MailboxException {
        try {
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;

            QueryManager manager = getSession().getWorkspace().getQueryManager();
            
            Query query = manager.createQuery(queryString, Query.XPATH);
            query.setLimit(1);
            QueryResult result = query.execute();

            NodeIterator iterator = result.getNodes();
            if(iterator.hasNext()) {
                return new JCRMessage(iterator.nextNode(), getLogger()).getUid();
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to find first unseen message in mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long add(Mailbox<String> mailbox, MailboxMembership<String> message) throws MailboxException {
        final JCRMessage membership = (JCRMessage) message;
        try {

            Node messageNode = null;

            if (membership.isPersistent()) {
                messageNode = getSession().getNodeByIdentifier(membership.getId());
            }

            if (messageNode == null) {
               
                Date date = message.getInternalDate();
                if (date == null) {
                    date = new Date();
                }

                // extracte the date from the message to create node structure
                // later
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                final String year = convertIntToString(cal.get(Calendar.YEAR));
                final String month = convertIntToString(cal.get(Calendar.MONTH) + 1);
                final String day = convertIntToString(cal.get(Calendar.DAY_OF_MONTH));
                final String hour = convertIntToString(cal.get(Calendar.HOUR_OF_DAY));
                final String min = convertIntToString(cal.get(Calendar.MINUTE));

                Node mailboxNode = getSession().getNodeByIdentifier(mailbox.getMailboxId());
                Node node = mailboxNode;

                if (scaleType > MESSAGE_SCALE_NONE) {
                    // we lock the whole mailbox with all its childs while
                    // adding the folder structure for the date

                    if (scaleType >= MESSAGE_SCALE_YEAR) {
                        node = JcrUtils.getOrAddFolder(node, year);

                        if (scaleType >= MESSAGE_SCALE_MONTH) {
                            node = JcrUtils.getOrAddFolder(node, month);

                            if (scaleType >= MESSAGE_SCALE_DAY) {
                                node = JcrUtils.getOrAddFolder(node, day);

                                if (scaleType >= MESSAGE_SCALE_HOUR) {
                                    node = JcrUtils.getOrAddFolder(node, hour);

                                    if (scaleType >= MESSAGE_SCALE_MINUTE) {
                                        node = JcrUtils.getOrAddFolder(node, min);
                                    }
                                }
                            }
                        }
                    }

                }

                final long nextUid = uidGenerator.nextUid(mSession, mailbox);

                messageNode = mailboxNode.addNode(String.valueOf(nextUid), "nt:file");
                messageNode.addMixin("jamesMailbox:message");
                try {
                    membership.merge(messageNode);
                    messageNode.setProperty(JCRMessage.UID_PROPERTY, nextUid);

                } catch (IOException e) {
                    throw new RepositoryException("Unable to merge message in to tree", e);
                }
                return nextUid;
                
            } else {
                membership.merge(messageNode);
                return membership.getUid();
            }
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to save message " + message + " in mailbox " + mailbox, e);
        } catch (IOException e) {
            throw new MailboxException("Unable to save message " + message + " in mailbox " + mailbox, e);
        }

    }

    /**
     * Convert the given int value to a String. If the int value is smaller then
     * 9 it will prefix the String with 0.
     * 
     * @param value
     * @return stringValue
     */
    private String convertIntToString(int value) {
        if (value <= 9) {
            return "0" + String.valueOf(value);
        } else {
            return String.valueOf(value);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#searchMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> searchMailbox(Mailbox<String> mailbox, SearchQuery query) throws MailboxException {
        try {
            final Query xQuery = formulateXPath(mailbox, query);
            
            QueryResult result = xQuery.execute();
            
            final NodeIterator it = result.getNodes();

            
            // Lazy build the JCRMessage instances
            return new SearchQueryIterator(new Iterator<MailboxMembership<?>>() {

                public boolean hasNext() {
                    return it.hasNext();
                }

                public MailboxMembership<?> next() {
                    return new JCRMessage(it.nextNode(), getLogger());
                    
                }

                public void remove() {
                    it.remove();
                }
                
            }, query, getLogger());
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to search messages for query " + query + " in mailbox " + mailbox, e);
        }
    }

    
    /**
     * Generate the XPath query for the SearchQuery
     * 
     * @param uuid
     * @param query
     * @return xpathQuery
     * @throws RepositoryException 
     * @throws ItemNotFoundException 
     */
    private Query formulateXPath(Mailbox<String> mailbox, SearchQuery query) throws ItemNotFoundException, RepositoryException {
        final StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)");
        final List<Criterion> criteria = query.getCriterias();
        boolean range = false;
        int rangeLength = -1;
        if (criteria.size() == 1) {
            final Criterion firstCriterion = criteria.get(0);
            if (firstCriterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) firstCriterion;
                final NumericRange[] ranges = uidCriterion.getOperator().getRange();
                rangeLength = ranges.length;
                for (int i = 0; i < ranges.length; i++) {
                    final long low = ranges[i].getLowValue();
                    final long high = ranges[i].getHighValue();
                    if (i > 0) {
                        queryBuilder.append(" and ");
                    } else {
                        queryBuilder.append("[");
                    }
                    if (low == Long.MAX_VALUE) {
                        range = true;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"<=").append(high);
                    } else if (low == high) {
                        range = false;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"=").append(low);
                    } else {
                        range = true;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"<=").append(high).append(" and @" + JCRMessage.UID_PROPERTY + ">=").append(low);
                    }
                }
            }
        }
        if (rangeLength > 0) queryBuilder.append("]");
        
        if (rangeLength != 0 || range) {
            queryBuilder.append(" order by @" + JCRMessage.UID_PROPERTY);
        }
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query xQuery = manager.createQuery(queryBuilder.toString(), Query.XPATH);
        
        // Check if we only need to fetch 1 message, if so we can set a limit to speed up things
        if (rangeLength == 1 && range == false) {
            xQuery.setLimit(1);
        }
        return xQuery;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(java.lang.Object, long, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long copy(Mailbox<String> mailbox, MailboxMembership<String> oldmessage) throws MailboxException{
        try {
            long uid = uidGenerator.nextUid(mSession, mailbox);
            String newMessagePath = getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath() + NODE_DELIMITER + String.valueOf(uid);
            getSession().getWorkspace().copy(((JCRMessage)oldmessage).getNode().getPath(), getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath() + NODE_DELIMITER + String.valueOf(uid));
            Node node = getSession().getNode(newMessagePath);
            node.setProperty(JCRMessage.MAILBOX_UUID_PROPERTY, mailbox.getMailboxId());
            node.setProperty(JCRMessage.UID_PROPERTY, uid);
            return uid;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to copy message " +oldmessage + " in mailbox " + mailbox, e);
        }
    }
    
   
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<UpdatedFlag> updateFlags(Mailbox<String> mailbox, Flags flags, boolean value, boolean replace, MessageRange set) throws MailboxException {
        final List<UpdatedFlag> updatedFlags = new ArrayList<UpdatedFlag>();

        final List<MailboxMembership<String>> members = findInMailbox(mailbox, set);
        for (final MailboxMembership<String> member:members) {
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
            
            JCRMessage membership = (JCRMessage) member;
            if (membership.isPersistent()) {
                try {
                    Node messageNode = getSession().getNodeByIdentifier(membership.getId());
                    membership.merge(messageNode);
                } catch (RepositoryException e) {
                    throw new MailboxException("Unable to update flags for message " + membership + " in mailbox " + mailbox, e);

                } catch (IOException e) {
                    throw new MailboxException("Unable to update flags for message " + membership + " in mailbox " + mailbox, e);

                }
            }

            
            updatedFlags.add(new UpdatedFlag(member.getUid(),originalFlags, newFlags));
        }
        
        return updatedFlags.iterator();       
    }
    

}

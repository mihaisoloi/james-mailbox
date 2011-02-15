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
package org.apache.james.mailbox.store.lucene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.AllCriterion;
import org.apache.james.mailbox.SearchQuery.ContainsOperator;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.DateOperator;
import org.apache.james.mailbox.SearchQuery.FlagCriterion;
import org.apache.james.mailbox.SearchQuery.HeaderCriterion;
import org.apache.james.mailbox.SearchQuery.HeaderOperator;
import org.apache.james.mailbox.SearchQuery.NumericOperator;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.UnsupportedSearchException;
import org.apache.james.mailbox.store.MessageSearchIndex;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.BodyDescriptor;
import org.apache.james.mime4j.message.Header;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * Lucene based {@link MessageSearchIndex} which offers message searching.
 * 

 * @param <Id>
 */
public class LuceneMessageSearchIndex<Id> implements MessageSearchIndex<Id>{

    /**
     * Default max query results
     */
    public final static int DEFAULT_MAX_QUERY_RESULTS = 100000;
    
    /**
     * {@link Field} which will contain the unique index of the {@link Document}
     */
    public final static String ID_FIELD ="id";
    
    /**
     * {@link Field} which will contain uid of the {@link MailboxMembership}
     */
    public final static String UID_FIELD = "uid";
    
    /**
     * {@link Field} which will contain the {@link Flags} of the {@link MailboxMembership}
     */
    public final static String FLAGS_FIELD = "flags";
  
    /**
     * {@link Field} which will contain the size of the {@link MailboxMembership}
     */
    public final static String SIZE_FIELD = "size";

    /**
     * {@link Field} which will contain the body of the {@link MailboxMembership}
     */
    public final static String BODY_FIELD = "body";
    
    
    public final static String PREFIX_HEADER_FIELD ="header_";
    
    /**
     * {@link Field} which will contain the whole message header of the {@link MailboxMembership}
     */
    public final static String HEADERS_FIELD ="headers";
    
    /**
     * {@link Field} which will contain the internalDate of the {@link MailboxMembership}
     */
    public final static String INTERNAL_DATE_FIELD ="internaldate";
 
    /**
     * {@link Field} which will contain the id of the {@link Mailbox}
     */
    public final static String MAILBOX_ID_FIELD ="mailboxid";

    
    private final static String MEDIA_TYPE_TEXT = "text"; 
    private final static String MEDIA_TYPE_MESSAGE = "message"; 

    private final IndexWriter writer;
    
    private int maxQueryResults = DEFAULT_MAX_QUERY_RESULTS;
    
    private final static Sort UID_SORT = new Sort(new SortField(UID_FIELD, SortField.LONG));
    
    public LuceneMessageSearchIndex(Directory directory) throws CorruptIndexException, LockObtainFailedException, IOException {
        this(new IndexWriter(directory, createAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED));
    }
    
   
    public LuceneMessageSearchIndex(IndexWriter writer) {
        this.writer = writer;
    }
    
    /**
     * Set the max count of results which will get returned from a query
     * 
     * @param maxQueryResults
     */
    public void setMaxQueryResults(int maxQueryResults) {
        this.maxQueryResults = maxQueryResults;
    }
    /**
     * Create a {@link Analyzer} which is used to index the {@link MailboxMembership}'s
     * 
     * @return analyzer
     */
    public static Analyzer createAnalyzer() {
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(new ImapSearchAnalyzer());
        return wrapper;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.MessageSearchIndex#search(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> search(MailboxSession session, Mailbox<Id> mailbox, SearchQuery searchQuery) throws MailboxException {
        List<Long> uids = new ArrayList<Long>();
        IndexSearcher searcher = null;

        try {
            searcher = new IndexSearcher(writer.getReader());
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(MAILBOX_ID_FIELD, mailbox.getMailboxId().toString())), BooleanClause.Occur.MUST);
            query.add(createQuery(searchQuery), BooleanClause.Occur.MUST);
            
            // query for all the documents sorted by uid
            TopDocs docs = searcher.search(query, null, maxQueryResults, UID_SORT);
            ScoreDoc[] sDocs = docs.scoreDocs;
            for (int i = 0; i < sDocs.length; i++) {
                long uid = Long.valueOf(searcher.doc(sDocs[i].doc).get(UID_FIELD));
                uids.add(uid);
            }
        } catch (IOException e) {
            throw new MailboxException("Unable to search the mailbox", e);
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
        }
        return uids.iterator();
    }

    /**
     * Create a new {@link Document} for the given {@link MailboxMembership}
     * 
     * @param membership
     * @return document
     */
    public static Document createDocument(MailboxMembership<?> membership) throws MailboxException{
        final Document doc = new Document();
        // TODO: Better handling
        doc.add(new Field(MAILBOX_ID_FIELD, membership.getMailboxId().toString(), Store.NO, Index.NOT_ANALYZED));
        
        
        doc.add(new NumericField(UID_FIELD,Store.YES, true).setLongValue(membership.getUid()));
        
        // create an unqiue key for the document which can be used later on updates to find the document
        doc.add(new Field(ID_FIELD, membership.getMailboxId().toString() +"-" + Long.toString(membership.getUid()), Store.NO, Index.NOT_ANALYZED));
        
        // add flags
        indexFlags(membership.createFlags(), doc);

        // store the internal date with resulution of a DAY
        doc.add(new NumericField(INTERNAL_DATE_FIELD,Store.NO, true).setLongValue(DateTools.round(membership.getInternalDate().getTime(),DateTools.Resolution.DAY)));
        doc.add(new NumericField(SIZE_FIELD,Store.NO, true).setLongValue(membership.getMessage().getFullContentOctets()));
        
        // content handler which will index the headers and the body of the message
        SimpleContentHandler handler = new SimpleContentHandler() {
            
            
            /**
             * Add the headers to the Document
             */
            public void headers(Header header) {
                
                Iterator<org.apache.james.mime4j.parser.Field> fields = header.iterator();
                while(fields.hasNext()) {
                   org.apache.james.mime4j.parser.Field f = fields.next();
                    doc.add(new Field(HEADERS_FIELD, f.toString() ,Store.NO, Index.ANALYZED));
                    doc.add(new Field(PREFIX_HEADER_FIELD + f.getName(), f.getBody() ,Store.NO, Index.ANALYZED));
                }
           
            }
            


            /**
             * Add the body parts to the Document
             */
            public void bodyDecoded(BodyDescriptor desc, InputStream in) throws IOException {
                String mediaType = desc.getMediaType();
                String charset = desc.getCharset();
                if (MEDIA_TYPE_TEXT.equalsIgnoreCase(mediaType) || MEDIA_TYPE_MESSAGE.equalsIgnoreCase(mediaType)) {
                    // TODO: maybe we want to limit the length here ?
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(in, out);
                    doc.add(new Field(BODY_FIELD,  out.toString(charset),Store.NO, Index.ANALYZED));
                    
                }
            }
        };
        
        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(handler);
        try {
            // paese the message to index headers and body
            parser.parse(membership.getMessage().getFullContent());
        } catch (MimeException e) {
            // This should never happen as it was parsed before too without problems.            
            throw new MailboxException("Unable to index content of message", e);
        } catch (IOException e) {
            // This should never happen as it was parsed before too without problems.
            // anyway let us just skip the body and headers in the index
            throw new MailboxException("Unable to index content of message", e);
        }


        return doc;
    }
    /**
     * Create a {@link Query} based on the given {@link SearchQuery}
     * 
     * @param searchQuery
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createQuery(SearchQuery searchQuery) throws UnsupportedSearchException {
        List<Criterion> crits = searchQuery.getCriterias();
        BooleanQuery booleanQuery = new BooleanQuery();

        for (int i = 0; i < crits.size(); i++) {
            booleanQuery.add(createQuery(crits.get(i)), BooleanClause.Occur.MUST);
        }
        return booleanQuery;

    }

    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.InternalDateCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createInternalDateQuery(SearchQuery.InternalDateCriterion crit) throws UnsupportedSearchException {
        DateOperator op = crit.getOperator();
        Calendar cal = Calendar.getInstance();
        cal.set(op.getYear(), op.getMonth() - 1, op.getDay());         
        long value = DateTools.round(cal.getTimeInMillis(), DateTools.Resolution.DAY);
        
        switch(op.getType()) {
        case ON:
            return NumericRangeQuery.newLongRange(INTERNAL_DATE_FIELD ,value, value, true, true);
        case BEFORE: 
            return NumericRangeQuery.newLongRange(INTERNAL_DATE_FIELD ,0L, value, true, false);
        case AFTER: 
            return NumericRangeQuery.newLongRange(INTERNAL_DATE_FIELD ,value, Long.MAX_VALUE, false, true);
        default:
            throw new UnsupportedSearchException();
        }
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.SizeCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createSizeQuery(SearchQuery.SizeCriterion crit) throws UnsupportedSearchException {
        NumericOperator op = crit.getOperator();
        switch (op.getType()) {
        case EQUALS:
            return NumericRangeQuery.newLongRange(SIZE_FIELD, op.getValue(), op.getValue(), true, true);
        case GREATER_THAN:
            return NumericRangeQuery.newLongRange(SIZE_FIELD, op.getValue(), Long.MAX_VALUE, false, true);
        case LESS_THAN:
            return NumericRangeQuery.newLongRange(SIZE_FIELD, Long.MIN_VALUE, op.getValue(), true, false);
        default:
            throw new UnsupportedSearchException();
        }
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.HeaderCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createHeaderQuery(SearchQuery.HeaderCriterion crit) throws UnsupportedSearchException {
        HeaderOperator op = crit.getOperator();
        String fieldName = PREFIX_HEADER_FIELD + crit.getHeaderName();
        if (op instanceof SearchQuery.ContainsOperator) {
            ContainsOperator cop = (ContainsOperator) op;
            return new TermQuery(new Term(fieldName, cop.getValue()));
        } else if (op instanceof SearchQuery.ExistsOperator){
            return new PrefixQuery(new Term(fieldName, ""));
        } else {
            // Operator not supported
            throw new UnsupportedSearchException();
        }
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.UidCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createUidQuery(SearchQuery.UidCriterion crit) throws UnsupportedSearchException {
        NumericRange[] ranges = crit.getOperator().getRange();
        BooleanQuery rangesQuery = new BooleanQuery();
        for (int i = 0; i < ranges.length; i++) {
            NumericRange range = ranges[i];
            rangesQuery.add(NumericRangeQuery.newLongRange(UID_FIELD, range.getLowValue(), range.getHighValue(), true, true), BooleanClause.Occur.SHOULD);
        }
        return rangesQuery;
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.FlagCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createFlagQuery(SearchQuery.FlagCriterion crit) throws UnsupportedSearchException {
        Flag flag = crit.getFlag();
        String value = flag.toString();
        TermQuery query = new TermQuery(new Term(FLAGS_FIELD, value));
        if (crit.getOperator().isSet()) {   
            return query;
        } else {
            // lucene does not support simple NOT queries so we do some nasty hack here
            BooleanQuery bQuery = new BooleanQuery();
            bQuery.add(new PrefixQuery(new Term(UID_FIELD, "")), BooleanClause.Occur.MUST);
            bQuery.add(query, BooleanClause.Occur.MUST_NOT);
            return bQuery;
        }
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.TextCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createTextQuery(SearchQuery.TextCriterion crit) throws UnsupportedSearchException {
        switch(crit.getType()) {
        case BODY:
            return new TermQuery(new Term(BODY_FIELD, crit.getOperator().getValue().toLowerCase(Locale.US)));
        case FULL: 
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(BODY_FIELD, crit.getOperator().getValue().toLowerCase(Locale.US))), BooleanClause.Occur.SHOULD);
            query.add(new TermQuery(new Term(HEADERS_FIELD, crit.getOperator().getValue().toLowerCase(Locale.US))), BooleanClause.Occur.SHOULD);
            return query;
        default:
            throw new UnsupportedSearchException();
        }
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.AllCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createAllQuery(SearchQuery.AllCriterion crit) throws UnsupportedSearchException{
        return NumericRangeQuery.newLongRange(UID_FIELD, Long.MIN_VALUE, Long.MAX_VALUE, true, true);
    }
    
    /**
     * Return a {@link Query} which is build based on the given {@link SearchQuery.ConjunctionCriterion}
     * 
     * @param crit
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createConjunctionQuery(SearchQuery.ConjunctionCriterion crit) throws UnsupportedSearchException {
        BooleanClause.Occur occur;
        switch (crit.getType()) {
        case AND:
            occur = BooleanClause.Occur.MUST;
            break;
        case OR:
            occur = BooleanClause.Occur.SHOULD;
            break;
        case NOR:
            occur = BooleanClause.Occur.MUST_NOT;
            break;
        default:
            throw new UnsupportedSearchException();
        }
        List<Criterion> crits = crit.getCriteria();
        BooleanQuery conQuery = new BooleanQuery();
        for (int i = 0; i < crits.size(); i++) {
            conQuery.add(createQuery(crits.get(i)), occur);
        }
        return conQuery;
    }
    
    /**
     * Return a {@link Query} which is builded based on the given {@link Criterion}
     * 
     * @param criterion
     * @return query
     * @throws UnsupportedSearchException
     */
    public static Query createQuery(Criterion criterion) throws UnsupportedSearchException {
        if (criterion instanceof SearchQuery.InternalDateCriterion) {
            SearchQuery.InternalDateCriterion crit = (SearchQuery.InternalDateCriterion) criterion;
            return createInternalDateQuery(crit);
        } else if (criterion instanceof SearchQuery.SizeCriterion) {
            SearchQuery.SizeCriterion crit = (SearchQuery.SizeCriterion) criterion;
            return createSizeQuery(crit);
        } else if (criterion instanceof SearchQuery.HeaderCriterion) {
            HeaderCriterion crit = (HeaderCriterion) criterion;
            return createHeaderQuery(crit);
        } else if (criterion instanceof SearchQuery.UidCriterion) {
            SearchQuery.UidCriterion crit = (SearchQuery.UidCriterion) criterion;
            return createUidQuery(crit);
        } else if (criterion instanceof SearchQuery.FlagCriterion) {
            FlagCriterion crit = (FlagCriterion) criterion;
            return createFlagQuery(crit);
        } else if (criterion instanceof SearchQuery.TextCriterion) {
            SearchQuery.TextCriterion crit = (SearchQuery.TextCriterion) criterion;
            return createTextQuery(crit);
        } else if (criterion instanceof SearchQuery.AllCriterion) {
            return createAllQuery((AllCriterion) criterion);
        } else if (criterion instanceof SearchQuery.ConjunctionCriterion) {
            SearchQuery.ConjunctionCriterion crit = (SearchQuery.ConjunctionCriterion) criterion;
            return createConjunctionQuery(crit);
        }
        throw new UnsupportedSearchException();

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.MessageSearchIndex#add(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public void add(MailboxSession session, Mailbox<Id> mailbox, MailboxMembership<Id> membership) throws MailboxException {
        Document doc = createDocument(membership);
        try {
            writer.addDocument(doc);
        } catch (CorruptIndexException e) {
            throw new MailboxException("Unable to add message to index", e);
        } catch (IOException e) {
            throw new MailboxException("Unable to add message to index", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.MessageSearchIndex#update(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange, javax.mail.Flags)
     */
    public void update(MailboxSession session, Mailbox<Id> mailbox, MessageRange range, Flags f) throws MailboxException {
        try {
            IndexSearcher searcher = new IndexSearcher(writer.getReader());
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(MAILBOX_ID_FIELD, mailbox.getMailboxId().toString())), BooleanClause.Occur.MUST);
            query.add(NumericRangeQuery.newLongRange(UID_FIELD, range.getUidFrom(), range.getUidTo(), true, true), BooleanClause.Occur.MUST);
            TopDocs docs = searcher.search(query, 100000);
            ScoreDoc[] sDocs = docs.scoreDocs;
            for (int i = 0; i < sDocs.length; i++) {
                Document doc = searcher.doc(sDocs[i].doc);
                doc.removeFields(FLAGS_FIELD);
                indexFlags(f, doc);
                writer.updateDocument(new Term(ID_FIELD, doc.get(ID_FIELD)), doc);
            }
        } catch (IOException e) {
            throw new MailboxException("Unable to add messages in index", e);

        }
        
    }

    /**
     * Index the {@link Flags} and add it to the {@link Document}
     * 
     * @param f
     * @param doc
     */
    private static void indexFlags(Flags f, Document doc) {
        Flag[] flags = f.getSystemFlags();
        for (int a = 0; a < flags.length; a++) {
            doc.add(new Field(FLAGS_FIELD, flags[a].toString(),Store.NO, Index.NOT_ANALYZED));
        }
        
        String[] userFlags = f.getUserFlags();
        for (int a = 0; a < userFlags.length; a++) {
            doc.add(new Field(FLAGS_FIELD, userFlags[a],Store.NO, Index.NOT_ANALYZED));
        }
    }
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.MessageSearchIndex#delete(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public void delete(MailboxSession session, Mailbox<Id> mailbox, MessageRange range) throws MailboxException {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term(MAILBOX_ID_FIELD, mailbox.getMailboxId().toString())), BooleanClause.Occur.MUST);
        query.add(NumericRangeQuery.newLongRange(UID_FIELD, range.getUidFrom(), range.getUidTo(), true, true), BooleanClause.Occur.MUST);
        
        try {
            writer.deleteDocuments(query);
        } catch (CorruptIndexException e) {
            throw new MailboxException("Unable to delete message from index", e);

        } catch (IOException e) {
            throw new MailboxException("Unable to delete message from index", e);
        }
    }
    


}

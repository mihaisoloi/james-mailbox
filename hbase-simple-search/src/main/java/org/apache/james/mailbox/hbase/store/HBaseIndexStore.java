package org.apache.james.mailbox.hbase.store;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.hbase.store.endpoint.RowFilteringProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.apache.james.mailbox.hbase.store.HBaseNames.COLUMN_FAMILY;
import static org.apache.james.mailbox.hbase.store.MessageFields.FLAGS_FIELD;

public class HBaseIndexStore {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseIndexStore.class);
    private static HBaseIndexStore store;
    private static HTableInterface table;

    private HBaseIndexStore() {
    }

    public static synchronized HBaseIndexStore getInstance(final Configuration configuration)
            throws IOException {
        if (store == null) {
            store = new HBaseIndexStore();
            HBaseAdmin admin = new HBaseAdmin(configuration);

            HTableDescriptor htd = new HTableDescriptor(HBaseNames.INDEX_TABLE.name);
            HColumnDescriptor columnDescriptor = new HColumnDescriptor(COLUMN_FAMILY.name);
            htd.addFamily(columnDescriptor);
            admin.createTable(htd);
            table = new HTable(configuration, HBaseNames.INDEX_TABLE.name);
        }
        return store;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * writes the rows as puts in HBase where the qualifier is composed of the mailID
     *
     * @param puts
     * @throws IOException
     */
    public void storeMail(List<Put> puts) throws IOException {
        for (Put put : puts) {
            table.put(put);
        }
    }

    Iterator<Long> retrieveMails(final byte[] mailboxId) throws Throwable {
        Map<byte[], Set<Long>> results = table.coprocessorExec(RowFilteringProtocol.class, mailboxId,
                Bytes.add(mailboxId, new byte[]{(byte) 0xFF}),
                new Batch.Call<RowFilteringProtocol, Set<Long>>() {
                    @Override
                    public Set<Long> call(RowFilteringProtocol instance) throws IOException {
                        return instance.filterByMailbox(mailboxId);
                    }
                });

        return extractMessageIds(results);
    }

    public ResultScanner retrieveMails(byte[] mailboxId, long messageId) throws IOException {
        Preconditions.checkArgument(messageId != 0l);
        Scan scan = new Scan();
        scan.addColumn(COLUMN_FAMILY.name, Bytes.toBytes(messageId));
        RowFilter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,
                new BinaryPrefixComparator(mailboxId));
        scan.setFilter(filter);
        return table.getScanner(scan);
    }

    public Iterator<Long> retrieveMails(final byte[] mailboxId,
                                        final ArrayListMultimap<MessageFields, String> queries)
            throws Throwable {
        if (queries.isEmpty())
            return retrieveMails(mailboxId);

        Map<byte[], Set<Long>> results = table.coprocessorExec(RowFilteringProtocol.class, mailboxId,
                Bytes.add(mailboxId, new byte[]{(byte) 0xFF}),
                new Batch.Call<RowFilteringProtocol, Set<Long>>() {
                    @Override
                    public Set<Long> call(RowFilteringProtocol instance) throws IOException {
                        return instance.filterByQueries(mailboxId, queries);
                    }
                });

        return extractMessageIds(results);
    }

    private Iterator<Long> extractMessageIds(Map<byte[], Set<Long>> results){
        Set<Long> uids = Sets.newHashSet();
        for (Map.Entry<byte[], Set<Long>> entry : results.entrySet()) {
            uids.addAll(entry.getValue());
        }
        return uids.iterator();
    }

    public void deleteMail(byte[] row, long messageId) throws IOException {
        Delete delete = new Delete(row);
        delete.deleteColumn(COLUMN_FAMILY.name, Bytes.toBytes(messageId));
        table.delete(delete);
    }

    public void flushToStore() throws IOException {
        table.flushCommits();
    }

    public Result retrieveFlags(byte[] mailboxId, long messageId) throws IOException {
        Get get = new Get(Bytes.add(mailboxId, new byte[]{FLAGS_FIELD.id}));
        get.addColumn(COLUMN_FAMILY.name, Bytes.toBytes(messageId));
        return table.get(get);
    }

    public void updateFlags(byte[] row, long messageId, String flags) throws IOException {
        Put put = new Put(row);
        put.add(COLUMN_FAMILY.name, Bytes.toBytes(messageId), Bytes.toBytes(flags));
        table.put(put);
    }


}

The project is basically an inverted index in an HBase table to search through the mails in a mailbox.

The structure of the index is as follows.

   1. mailboxID  is an java.util.UUID
   2. the fields are now Enums, and what is stored is a byte that identifies that enum field.
   3. each of the terms in the fields are tokenized using the lucene org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer, but some fields are not tokenized due to their nature(SENT_DATE for example)

The row is composed of all the above byte arrays concatenated, so that searching can be done very fast through the HBase table, as well as lookup on the specific mailbox and field in the mail. The mailID is the qualifier in the static column family(only one column family) so that mail id's are found with relative ease.

This is for the mail document in itself, the flags are stored in a single row in the table(one row for each mailbox) and can be found easily by a scan. Each of the rows now has an empty value, where in the possible future we'll be able to store data related to the term frequency in the document.

What works currently are the searches based on the text, flags, headers, all criterions. These are implemented using Filters but I will be switching to Coprocessors till next Monday due to the benefit they provide of less data transfer over the network and distributed processing on each region. 

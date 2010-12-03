package org.apache.james.mailbox.jcr;

import org.apache.james.imap.functional.suite.Search;

public class SearchTest extends Search{

    public SearchTest() throws Exception {
        super(JCRHostSystem.build());
    }
}

package org.apache.james.mailbox.jcr;

import org.apache.james.imap.functional.suite.Rename;

public class RenameTest extends Rename {

    public RenameTest() throws Exception {
        super(JCRHostSystem.build());
    }

}


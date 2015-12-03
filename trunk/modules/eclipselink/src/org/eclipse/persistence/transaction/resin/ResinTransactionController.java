/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Caucho - initial implementation from Resin
 ******************************************************************************/  
package org.eclipse.persistence.transaction.resin;

import javax.transaction.TransactionManager;
import org.eclipse.persistence.transaction.JTATransactionController;

public class ResinTransactionController extends JTATransactionController {
    public static final String JNDI_TRANSACTION_MANAGER_NAME = "java:comp/TransactionManager";

    protected TransactionManager acquireTransactionManager() throws Exception {
        return (TransactionManager)jndiLookup(JNDI_TRANSACTION_MANAGER_NAME);
    }
}

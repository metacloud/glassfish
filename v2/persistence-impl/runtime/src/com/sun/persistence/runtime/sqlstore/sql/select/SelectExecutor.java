/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */


package com.sun.persistence.runtime.sqlstore.sql.select;

import com.sun.org.apache.jdo.pm.PersistenceManagerInternal;
import com.sun.persistence.runtime.query.QueryInternal;

import java.util.List;

/**
 * @author Mitesh Meswani
 */
public interface SelectExecutor {

    /**
     * Execute this query in context of the given pm.
     * @param pm The given <code>PersistenceManager</code>
     * @param userParams Parameters to the query
     * @return result of query execution.
     */
    List execute(PersistenceManagerInternal pm, QueryInternal userParams);
}
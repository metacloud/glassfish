/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.gjc.spi;

import javax.resource.spi.*;
import javax.resource.*;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import javax.transaction.xa.XAResource;
import java.util.Set;
import java.util.Hashtable;
import java.util.Iterator;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import java.sql.SQLException;
import javax.resource.spi.security.PasswordCredential;

import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.base.ConnectionHolder;
import com.sun.logging.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.resource.BadConnectionEventListener;

/**
 * <code>ManagedConnection</code> implementation for Generic JDBC Connector.
 *
 * @author Evani Sai Surya Kiran
 * @version 1.0, 02/07/22
 */
public class ManagedConnection implements javax.resource.spi.ManagedConnection,
        javax.resource.spi.LazyEnlistableManagedConnection,
        javax.resource.spi.DissociatableManagedConnection {

    public static final int ISNOTAPOOLEDCONNECTION = 0;
    public static final int ISPOOLEDCONNECTION = 1;
    public static final int ISXACONNECTION = 2;

    protected boolean isDestroyed = false;
    protected boolean isUsable = true;
    protected int connectionCount = 0;

    protected int connectionType = ISNOTAPOOLEDCONNECTION;
    protected PooledConnection pc = null;
    protected java.sql.Connection actualConnection = null;
    protected Hashtable connectionHandles;
    protected PrintWriter logWriter;
    protected PasswordCredential passwdCredential;
    private javax.resource.spi.ManagedConnectionFactory mcf = null;
    protected XAResource xar = null;

    protected ConnectionHolder myLogicalConnection = null;  

    //GJCINT
    protected int lastTransactionIsolationLevel;
    protected boolean isClean = true;

    protected boolean transactionInProgress = false;

    protected ConnectionEventListener listener = null;

    protected ConnectionEvent ce = null;

    private boolean defaultAutoCommitValue = true;
    private boolean lastAutoCommitValue = defaultAutoCommitValue;

    private boolean markedForRemoval = false;

    //private boolean statemntWrapping;
    private int statementTimeout;

    protected final static Logger _logger;

    static {
        _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);
    }

    protected StringManager localStrings =
            StringManager.getManager(DataSourceObjectBuilder.class);

    /**
     * Constructor for <code>ManagedConnection</code>. The pooledConn parameter is expected
     * to be null and sqlConn parameter is the actual connection in case where
     * the actual connection is got from a non pooled datasource object. The
     * pooledConn parameter is expected to be non null and sqlConn parameter
     * is expected to be null in the case where the datasource object is a
     * connection pool datasource or an xa datasource.
     *
     * @param pooledConn <code>PooledConnection</code> object in case the
     *                   physical connection is to be obtained from a pooled
     *                   <code>DataSource</code>; null otherwise
     * @param sqlConn    <code>java.sql.Connection</code> object in case the physical
     *                   connection is to be obtained from a non pooled <code>DataSource</code>;
     *                   null otherwise
     * @param passwdCred object conatining the
     *                   user and password for allocating the connection
     * @throws ResourceException if the <code>ManagedConnectionFactory</code> object
     *                           that created this <code>ManagedConnection</code> object
     *                           is not the same as returned by <code>PasswordCredential</code>
     *                           object passed
     */
    public ManagedConnection(PooledConnection pooledConn, java.sql.Connection sqlConn,
                             PasswordCredential passwdCred, javax.resource.spi.ManagedConnectionFactory mcf)
            throws ResourceException {
        if (pooledConn == null && sqlConn == null) {

            String i18nMsg = localStrings.getString(
                    "jdbc.conn_obj_null");
            throw new ResourceException(i18nMsg);
        }
                  
        if (connectionType == ISNOTAPOOLEDCONNECTION) {
            actualConnection = sqlConn;
        }

        pc = pooledConn;
        connectionHandles = new Hashtable();
        passwdCredential = passwdCred;

        this.mcf = mcf;
        if (passwdCredential != null &&
                this.mcf.equals(passwdCredential.getManagedConnectionFactory()) == false) {

            String i18nMsg = localStrings.getString(
                    "jdbc.mc_construct_err");
            throw new ResourceException(i18nMsg);
        }
        logWriter = mcf.getLogWriter();
        ce = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener <code>ConnectionEventListener</code>
     * @see <code>removeConnectionEventListener</code>
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        this.listener = listener;
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a <code>ManagedConnection</code> instance.
     *
     * @param connection <code>ConnectionHolder30</code> to be associated with
     *                   this <code>ManagedConnection</code> instance
     * @throws ResourceException if the physical connection is no more
     *                           valid or the connection handle passed is null
     */
    public void associateConnection(Object connection) throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In associateConnection");
        }
        checkIfValid();
        if (connection == null) {
            String i18nMsg = localStrings.getString(
                    "jdbc.conn_handle_null");
            throw new ResourceException(i18nMsg);
        }
        ConnectionHolder ch = (ConnectionHolder) connection;

        com.sun.gjc.spi.ManagedConnection mc = ch.getManagedConnection();
        isClean = false;

        ch.associateConnection(actualConnection, this);
        /**
         * The expectation from the above method is that the connection holder
         * replaces the actual sql connection it holds with the sql connection
         * handle being passed in this method call. Also, it replaces the reference
         * to the ManagedConnection instance with this ManagedConnection instance.
         * Any previous statements and result sets also need to be removed.
         */


        ch.setActive(true);
        incrementCount();

        //mc will be null in case we are lazily associating 
        if (mc != null) {
            mc.decrementCount();
        }
    }

    /**
     * Application server calls this method to force any cleanup on the
     * <code>ManagedConnection</code> instance. This method calls the invalidate
     * method on all ConnectionHandles associated with this <code>ManagedConnection</code>.
     *
     * @throws ResourceException if the physical connection is no more valid
     */
    public void cleanup() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In cleanup");
        }
        checkIfValid();

        /**
         * may need to set the autocommit to true for the non-pooled case.
         */
        //GJCINT
        isClean = true;

    }

    /**
     * This method removes all the connection handles from the table
     * of connection handles and invalidates all of them so that any
     * operation on those connection handles throws an exception.
     *
     * @throws ResourceException if there is a problem in retrieving
     *                           the connection handles
     */
    protected void invalidateAllConnectionHandles() throws ResourceException {
        Set handles = connectionHandles.keySet();
        Iterator iter = handles.iterator();
        try {
            while (iter.hasNext()) {
                ConnectionHolder ch = (ConnectionHolder) iter.next();
                ch.invalidate();
            }
        } catch (java.util.NoSuchElementException nsee) {
            throw new ResourceException("Could not find the connection handle: " + nsee.getMessage());
        }
        connectionHandles.clear();
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException if there is an error in closing the physical connection
     */
    public void destroy() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In destroy");
        }
        //GJCINT
        if (isDestroyed) {
            return;
        }

        try {
            if (connectionType == ISXACONNECTION || connectionType == ISPOOLEDCONNECTION) {
                pc.close();
                pc = null;
                actualConnection = null;
            } else {
                actualConnection.close();
                actualConnection = null;
            }
        } catch (SQLException sqle) {
            isDestroyed = true;
            passwdCredential = null;
            connectionHandles = null;
            String i18nMsg = localStrings.getString(
                    "jdbc.error_in_destroy");
            ResourceException re = new ResourceException(
                    i18nMsg + sqle.getMessage(), sqle);
            throw re;
        }
        isDestroyed = true;
        passwdCredential = null;
        connectionHandles = null;
    }

    /**
     * Creates a new connection handle for the underlying physical
     * connection represented by the <code>ManagedConnection</code> instance.
     *
     * @param sub       <code>Subject</code> parameter needed for authentication
     * @param cxReqInfo <code>ConnectionRequestInfo</code> carries the user
     *                  and password required for getting this connection.
     * @return Connection    the connection handle <code>Object</code>
     * @throws ResourceException if there is an error in allocating the
     *                           physical connection from the pooled connection
     * @throws javax.resource.spi.SecurityException
     *                           if there is a mismatch between the
     *                           password credentials or reauthentication is requested
     */
    public Object getConnection(Subject sub, javax.resource.spi.ConnectionRequestInfo cxReqInfo)
            throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In getConnection");
        }
        checkIfValid();
        /** Appserver any way doesnt bother about re-authentication today. So commenting this out now.
         com.sun.gjc.spi.ConnectionRequestInfo cxRequestInfo = (com.sun.gjc.spi.ConnectionRequestInfo) cxReqInfo;
         PasswordCredential passwdCred = SecurityUtils.getPasswordCredential(this.mcf, sub, cxRequestInfo);

         if(SecurityUtils.isPasswordCredentialEqual(this.passwdCredential, passwdCred) == false) {
         throw new javax.resource.spi.SecurityException("Re-authentication not supported");
         }
         **/

        //GJCINT
        getActualConnection();
        ManagedConnectionFactory spiMCF = (ManagedConnectionFactory) mcf;
        resetConnectionProperties(spiMCF);
        myLogicalConnection = spiMCF.getJdbcObjectsFactory().getConnection(
                actualConnection, this, cxReqInfo, spiMCF.isStatementWrappingEnabled());

        incrementCount();
        isClean = false;

        myLogicalConnection.setActive(true);

        return myLogicalConnection;
    }

    /**
     * Resett connection properties as connections are pooled by application server<br>
     *
     * @param spiMCF
     * @throws ResourceException
     */
    private void resetConnectionProperties(ManagedConnectionFactory spiMCF) throws ResourceException {
        /**
         * The following code in the if statement first checks if this ManagedConnection
         * is clean or not. If it is, it resets the transaction isolation level to what
         * it was when it was when this ManagedConnection was cleaned up depending on the
         * ConnectionRequestInfo passed.
         */
        if (isClean) {
            spiMCF.resetIsolation(this, lastTransactionIsolationLevel);
        }

        // reset the autocommit value of the connection if application has modified it.
        resetAutoCommit();

        String statementTimeoutString = spiMCF.getStatementTimeout();
        if (statementTimeoutString != null ){
            int timeoutValue = Integer.valueOf(statementTimeoutString);
	    if(timeoutValue >= 0){
                statementTimeout = timeoutValue;
            }
        }
    }

    /**
     * To reset AutoCommit of actual connection.
     * If the last-auto-commit value is different from default-auto-commit value, reset will happen.
     * If there is a transaction in progress (because of connection sharing), reset will not happen.
     *
     * @throws ResourceException
     */
    private void resetAutoCommit() throws ResourceException {
        if (defaultAutoCommitValue != getLastAutoCommitValue() && !(isTransactionInProgress())) {
            try {
                actualConnection.setAutoCommit(defaultAutoCommitValue);
            } catch (SQLException sqle) {
                String i18nMsg = localStrings.getString(
                        "jdbc.error_during_setAutoCommit");
                throw new ResourceException(i18nMsg + sqle.getMessage(), sqle);
            }
            setLastAutoCommitValue(defaultAutoCommitValue);
        }
    }

    /**
     * Returns an <code>LocalTransaction</code> instance. The <code>LocalTransaction</code> interface
     * is used by the container to manage local transactions for a RM instance.
     *
     * @return <code>LocalTransaction</code> instance
     * @throws ResourceException if the physical connection is not valid
     */
    public javax.resource.spi.LocalTransaction getLocalTransaction() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In getLocalTransaction");
        }
        checkIfValid();
        return new com.sun.gjc.spi.LocalTransaction(this);
    }

    /**
     * Gets the log writer for this <code>ManagedConnection</code> instance.
     *
     * @return <code>PrintWriter</code> instance associated with this
     *         <code>ManagedConnection</code> instance
     * @throws ResourceException if the physical connection is not valid
     * @see <code>setLogWriter</code>
     */
    public PrintWriter getLogWriter() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In getLogWriter");
        }
        checkIfValid();

        return logWriter;
    }

    /**
     * Gets the metadata information for this connection's underlying EIS
     * resource manager instance.
     *
     * @return <code>ManagedConnectionMetaData</code> instance
     * @throws ResourceException if the physical connection is not valid
     */
    public javax.resource.spi.ManagedConnectionMetaData getMetaData() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In getMetaData");
        }
        checkIfValid();

        return new com.sun.gjc.spi.ManagedConnectionMetaData(this);
    }

    /**
     * Returns an <code>XAResource</code> instance.
     *
     * @return <code>XAResource</code> instance
     * @throws ResourceException     if the physical connection is not valid or
     *                               there is an error in allocating the
     *                               <code>XAResource</code> instance
     * @throws NotSupportedException if underlying datasource is not an
     *                               <code>XADataSource</code>
     */
    public XAResource getXAResource() throws ResourceException {
        if (logWriter != null) {
            logWriter.println("In getXAResource");
        }
        checkIfValid();

        if (connectionType == ISXACONNECTION) {
            try {
                if (xar == null) {
                    /**
                     * Using the wrapper XAResource.
                     */
                    xar = new com.sun.gjc.spi.XAResourceImpl(((XAConnection) pc).getXAResource(), this);
                }
                return xar;
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage(), sqle);
            }
        } else {
            throw new NotSupportedException("Cannot get an XAResource from a non XA connection");
        }
    }

    /**
     * Removes an already registered connection event listener from the
     * <code>ManagedConnection</code> instance.
     *
     * @param listener <code>ConnectionEventListener</code> to be removed
     * @see <code>addConnectionEventListener</code>
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    /**
     * This method is called from XAResource wrapper object
     * when its XAResource.start() has been called or from
     * LocalTransaction object when its begin() method is called.
     */
    void transactionStarted() {
        transactionInProgress = true;
    }

    /**
     * This method is called from XAResource wrapper object
     * when its XAResource.end() has been called or from
     * LocalTransaction object when its end() method is called.
     */
    void transactionCompleted() {
        try {
            transactionInProgress = false;
            if (connectionType == ISPOOLEDCONNECTION || connectionType == ISXACONNECTION) {
                 if (connectionCount <= 0) {
                    try {
                        actualConnection.close();
                        actualConnection = null;
                    } catch (SQLException sqle) {
                        actualConnection = null;
                    }
                }
            }
        } catch (java.lang.NullPointerException e) {
            _logger.log(Level.FINE, "jdbc.duplicateTxCompleted");
        }

        if (markedForRemoval) {
            connectionErrorOccurred(null, null);
            _logger.log(Level.INFO, "jdbc.markedForRemoval_txCompleted");
            markedForRemoval = false;
        }

        isClean = true;
    }

    /**
     * Checks if a this ManagedConnection is involved in a transaction
     * or not.
     */
    public boolean isTransactionInProgress() {
        return transactionInProgress;
    }

    /**
     * Sets the log writer for this <code>ManagedConnection</code> instance.
     *
     * @param out <code>PrintWriter</code> to be associated with this
     *            <code>ManagedConnection</code> instance
     * @throws ResourceException if the physical connection is not valid
     * @see <code>getLogWriter</code>
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        checkIfValid();
        logWriter = out;
    }

    /**
     * This method determines the type of the connection being held
     * in this <code>ManagedConnection</code>.
     *
     * @param pooledConn <code>PooledConnection</code>
     * @return connection type
     */
    protected int getConnectionType(PooledConnection pooledConn) {
        if (pooledConn == null) {
            return ISNOTAPOOLEDCONNECTION;
        } else if (pooledConn instanceof XAConnection) {
            return ISXACONNECTION;
        } else {
            return ISPOOLEDCONNECTION;
        }
    }

    /**
     * Returns the <code>ManagedConnectionFactory</code> instance that
     * created this <code>ManagedConnection</code> instance.
     *
     * @return <code>ManagedConnectionFactory</code> instance that created this
     *         <code>ManagedConnection</code> instance
     */
    public com.sun.gjc.spi.ManagedConnectionFactory getManagedConnectionFactory() {
        return (com.sun.gjc.spi.ManagedConnectionFactory) mcf;
    }

    /**
     * Returns the actual sql connection for this <code>ManagedConnection</code>.
     *
     * @return the physical <code>java.sql.Connection</code>
     */
    //GJCINT
    java.sql.Connection getActualConnection() throws ResourceException {
        //GJCINT
        if (connectionType == ISXACONNECTION || connectionType == ISPOOLEDCONNECTION) {
            try {
                if (actualConnection == null) {
                    actualConnection = pc.getConnection();

                    //re-initialize lastAutoCommitValue such that resetAutoCommit() wont
                    // affect autoCommit of actualConnection
                    setLastAutoCommitValue(defaultAutoCommitValue);
                }

            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage(), sqle);
            }
        }
        return actualConnection;
    }

    /**
     * Returns the <code>PasswordCredential</code> object associated with this <code>ManagedConnection</code>.
     *
     * @return <code>PasswordCredential</code> associated with this
     *         <code>ManagedConnection</code> instance
     */
    PasswordCredential getPasswordCredential() {
        return passwdCredential;
    }

    /**
     * Checks if this <code>ManagedConnection</code> is valid or not and throws an
     * exception if it is not valid. A <code>ManagedConnection</code> is not valid if
     * destroy has not been called and no physical connection error has
     * occurred rendering the physical connection unusable.
     *
     * @throws ResourceException if <code>destroy</code> has been called on this
     *                           <code>ManagedConnection</code> instance or if a
     *                           physical connection error occurred rendering it unusable
     */
    //GJCINT
    void checkIfValid() throws ResourceException {
        if (isDestroyed || !isUsable) {

            String i18nMsg = localStrings.getString(
                    "jdbc.mc_not_usable");
            throw new ResourceException(i18nMsg);
        }
    }

    /**
     * This method is called by the <code>ConnectionHolder30</code> when its close method is
     * called. This <code>ManagedConnection</code> instance  invalidates the connection handle
     * and sends a CONNECTION_CLOSED event to all the registered event listeners.
     *
     * @param e                  Exception that may have occured while closing the connection handle
     * @param connHolder30Object <code>ConnectionHolder30</code> that has been closed
     * @throws SQLException in case closing the sql connection got out of
     *                      <code>getConnection</code> on the underlying
     *                      <code>PooledConnection</code> throws an exception
     */
    public void connectionClosed(Exception e, ConnectionHolder connHolder30Object) throws SQLException {

        connHolder30Object.invalidate();
        decrementCount();
        ce.setConnectionHandle(connHolder30Object);

        if (markedForRemoval && !transactionInProgress) {
            BadConnectionEventListener bcel = (BadConnectionEventListener)listener;
            bcel.badConnectionClosed(ce);
            _logger.log(Level.INFO, "jdbc.markedForRemoval_conClosed");
            markedForRemoval = false;
        }else{
            listener.connectionClosed(ce);
        }
    }

    /**
     * This method is called by the <code>ConnectionHolder30</code> when it detects a connecion
     * related error.
     *
     * @param e                Exception that has occurred during an operation on the physical connection
     * @param connHolderObject <code>ConnectionHolder</code> that detected the physical
     *                         connection error
     */
    void connectionErrorOccurred(Exception e,
                                 ConnectionHolder connHolderObject) {

        ConnectionEventListener cel = this.listener;
        ConnectionEvent ce = null;
        ce = e == null ? new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED)
                : new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, e);
        if (connHolderObject != null) {
            ce.setConnectionHandle(connHolderObject);
        }

        cel.connectionErrorOccurred(ce);
        isUsable = false;
    }


    /**
     * This method is called by the <code>XAResource</code> object when its start method
     * has been invoked.
     */
    void XAStartOccurred() {
        try {
            actualConnection.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
            connectionErrorOccurred(e, null);
        }
    }

    /**
     * This method is called by the <code>XAResource</code> object when its end method
     * has been invoked.
     */
    void XAEndOccurred() {
        try {
            actualConnection.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
            connectionErrorOccurred(e, null);
        }
    }

    /**
     * This method is called by a Connection Handle to check if it is
     * the active Connection Handle. If it is not the active Connection
     * Handle, this method throws an SQLException. Else, it
     * returns setting the active Connection Handle to the calling
     * Connection Handle object to this object if the active Connection
     * Handle is null.
     *
     * @param ch <code>ConnectionHolder30</code> that requests this
     *           <code>ManagedConnection</code> instance whether
     *           it can be active or not
     * @throws SQLException in case the physical is not valid or
     *                      there is already an active connection handle
     */

    public void checkIfActive(ConnectionHolder ch) throws SQLException {
        if (isDestroyed || !isUsable) {

            String i18nMsg = localStrings.getString(
                    "jdbc.conn_not_usable");
            throw new SQLException(i18nMsg);
        }
    }

    /**
     * sets the connection type of this connection. This method is called
     * by the MCF while creating this ManagedConnection. Saves us a costly
     * instanceof operation in the getConnectionType
     */
    public void initializeConnectionType(int _connectionType) {
        connectionType = _connectionType;
    }

    public void incrementCount() {
        connectionCount++;
    }

    public void decrementCount() {
        connectionCount--;
    }

    public void dissociateConnections() {
        myLogicalConnection.setManagedConnection(null);
    }

    public boolean getLastAutoCommitValue() {
        return lastAutoCommitValue;
    }

    /**
     * To keep track of last auto commit value.
     * Helps to reset the auto-commit-value while giving new connection-handle.
     *
     * @param lastAutoCommitValue
     */
    public void setLastAutoCommitValue(boolean lastAutoCommitValue) {
        this.lastAutoCommitValue = lastAutoCommitValue;
    }


    public void markForRemoval(boolean flag) {
        markedForRemoval = flag;
    }

    public javax.resource.spi.ManagedConnectionFactory getMcf() {
        return mcf;
    }

/*
    public boolean getStatementWrapping(){
        return statemntWrapping;
    }
*/

    public int getStatementTimeout() {
        return statementTimeout;
    }

    public void setLastTransactionIsolationLevel(int isolationLevel) {
        lastTransactionIsolationLevel = isolationLevel;
    }
}
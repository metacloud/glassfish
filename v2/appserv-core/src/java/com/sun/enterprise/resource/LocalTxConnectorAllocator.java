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

package com.sun.enterprise.resource;

import javax.transaction.xa.XAResource;
import javax.resource.spi.*;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import java.util.logging.*;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.PoolManager;

/**
 * @author Tony Ng
 */
public class LocalTxConnectorAllocator extends AbstractConnectorAllocator {


    public LocalTxConnectorAllocator(PoolManager poolMgr,
                                     ManagedConnectionFactory mcf,
                                     ResourceSpec spec,
                                     Subject subject,
                                     ConnectionRequestInfo reqInfo,
                                     ClientSecurityInfo info,
                                     ConnectorDescriptor desc) {
        super(poolMgr, mcf, spec, subject, reqInfo, info, desc);
    }

    
    public ResourceHandle createResource()
         throws PoolingException {
        try {
            ManagedConnection mc =
                mcf.createManagedConnection(subject, reqInfo);
            
            ResourceHandle resource =
                new ResourceHandle(mc, spec, this, info);
            ConnectionEventListener l = 
                new LocalTxConnectionEventListener(resource);
            mc.addConnectionEventListener(l);
            resource.setListener(l);

            XAResource xares = 
                new ConnectorXAResource(resource, spec, this, info);
            resource.fillInResourceObjects(null, xares);

            return resource;
        } catch (ResourceException ex) {
            _logger.log(Level.WARNING,"poolmgr.create_resource_error",ex.getMessage());
            _logger.log(Level.FINE,"Resource Exception while creating resource",ex);

            if (ex.getLinkedException() != null) {
                _logger.log(Level.WARNING,"poolmgr.create_resource_error",ex.getLinkedException().getMessage());
            }
            throw new PoolingException(ex);
        } 
    }

    public void fillInResourceObjects(ResourceHandle resource)
            throws PoolingException {
        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();

            Object con = mc.getConnection(subject, reqInfo);

            ConnectorXAResource xares = (ConnectorXAResource) resource.getXAResource();
            xares.setUserHandle(con);
            resource.fillInResourceObjects(con, xares);
        } catch (ResourceException ex) {
            throw new PoolingException(ex);
        }
    }

    public void destroyResource(ResourceHandle resource)
            throws PoolingException {
        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();
            ConnectorXAResource.freeListener(mc);
            mc.destroy();
            if (_logger.isLoggable( Level.FINEST ) ) {
                _logger.finest( "destroyResource for LocalTxConnectorAllocator done");
            }

        } catch (Exception ex) {
            _logger.log(Level.WARNING, ex.getMessage());
            throw new PoolingException(ex);
        }
    }

   
    public boolean shareableWithinComponent() {
        //For local transactions, a resource is always shareable within components 
	    //within the same transaction
        return true;
    }

}
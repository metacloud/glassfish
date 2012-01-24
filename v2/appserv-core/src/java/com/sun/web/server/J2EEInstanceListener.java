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
package com.sun.web.server;

import java.util.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.transaction.Transaction;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest; // IASRI 4713234
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import org.apache.catalina.Realm;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.jasper.servlet.JspServlet;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.coyote.tomcat5.CoyoteRequestFacade;
import com.sun.enterprise.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.appverification.factory.AppVerification;
import com.sun.enterprise.security.LoginContext;
import com.sun.enterprise.security.SecurityContext; // IASRI 4688449
import com.sun.enterprise.log.Log;
import com.sun.web.security.RealmAdapter;
import com.sun.web.security.WebPrincipal;


//START OF IASRI 4660742
import java.util.logging.*;
import com.sun.logging.*;
//END OF IASRI 4660742

/**
 * This class implements the Tomcat InstanceListener interface and
 * handles the INIT,DESTROY and SERVICE, FILTER events.
 * @author Vivek Nagar
 * @author Tony Ng
 */
public final class J2EEInstanceListener implements InstanceListener {

    // START OF IASRI 4660742
    static Logger _logger=LogDomains.getLogger(LogDomains.WEB_LOGGER);
    // END OF IASRI 4660742

    private static final HashSet beforeEvents = new HashSet(4);
    private static final HashSet afterEvents = new HashSet(4);

    static {
        beforeEvents.add(InstanceEvent.BEFORE_SERVICE_EVENT);
        beforeEvents.add(InstanceEvent.BEFORE_FILTER_EVENT);
        beforeEvents.add(InstanceEvent.BEFORE_INIT_EVENT);
        beforeEvents.add(InstanceEvent.BEFORE_DESTROY_EVENT);

        afterEvents.add(InstanceEvent.AFTER_SERVICE_EVENT);
        afterEvents.add(InstanceEvent.AFTER_FILTER_EVENT);
        afterEvents.add(InstanceEvent.AFTER_INIT_EVENT);
        afterEvents.add(InstanceEvent.AFTER_DESTROY_EVENT);
    }

    private InvocationManager im;
    private J2EETransactionManager tm;
    private InjectionManager injectionMgr;
    //    private LoginContext lc = null;

    public J2EEInstanceListener() {
        im = Switch.getSwitch().getInvocationManager();
        tm = Switch.getSwitch().getTransactionManager();
        injectionMgr = Switch.getSwitch().getInjectionManager();
	//	lc = new LoginContext();
    }

    public void instanceEvent(InstanceEvent event) {
        String eventType = event.getType();
	if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,"*** InstanceEvent: " + eventType);
        }
        if (beforeEvents.contains(eventType)) {
            handleBeforeEvent(event, eventType);
        } else if (afterEvents.contains(eventType)) {
            handleAfterEvent(event, eventType);
        }
    }

    private void handleBeforeEvent(InstanceEvent event, String eventType) {

        Object instance = null;
        if (eventType.equals(InstanceEvent.BEFORE_FILTER_EVENT)) {
            instance = event.getFilter();
        } else {
            instance = event.getServlet();
        }            
        Context context = (Context) event.getWrapper().getParent();

        // set security context
        // BEGIN IASRI 4688449
        //try {
        Realm ra = context.getRealm();
        /** IASRI 4713234
        if (ra != null) {
            HttpServletRequest request = 
                (HttpServletRequest) event.getRequest();
            if (request != null && request.getUserPrincipal() != null) {
                WebPrincipal prin = 
                    (WebPrincipal) request.getUserPrincipal();
                // ra.authenticate(prin);
                
                // It is inefficient to call authenticate just to set
                // sec.ctx.  Instead, WebPrincipal modified to keep the
                // previously created secctx, and set it here directly.

                SecurityContext.setCurrent(prin.getSecurityContext());
            }
        }
        **/
        // START OF IASRI 4713234
        if (ra != null) {

            ServletRequest request = 
		(ServletRequest) event.getRequest();
            if (request != null && request instanceof HttpServletRequest) {

                HttpServletRequest hreq = (HttpServletRequest)request;
		HttpServletRequest base = hreq;
		
		Principal prin = hreq.getUserPrincipal();
		Principal basePrincipal = prin; 
		
		boolean wrapped = false;

		while (prin != null && base != null) {
		    
		    if (base instanceof ServletRequestWrapper) {
			// unwarp any wrappers to find the base object
			ServletRequest sr = 
			    ((ServletRequestWrapper) base).getRequest();

			if (sr instanceof HttpServletRequest) {

			    base = (HttpServletRequest) sr;
			    wrapped = true;
			    continue;
			} 
		    }

		    if (wrapped) {
			basePrincipal = base.getUserPrincipal();
		    } 

		    else if (base instanceof CoyoteRequestFacade) {
			// try to avoid the getUnWrappedCoyoteRequest call
			// when we can identify see we have the texact class.
			if (base.getClass() != CoyoteRequestFacade.class) {
			    basePrincipal = ((CoyoteRequestFacade)base).
				getUnwrappedCoyoteRequest().getUserPrincipal();
			}
		    } else {
			basePrincipal = base.getUserPrincipal();
		    }

		    break;
		}

		if (prin != null && prin == basePrincipal && 
		    prin instanceof WebPrincipal) {

		    SecurityContext.setCurrent
			(getSecurityContextForPrincipal(prin));
		    
		} else if (prin != basePrincipal) {
		    
		    // the wrapper has overridden getUserPrincipal
		    // reject the request if the wrapper does not have
		    // the necessary permission.

		    checkObjectForDoAsPermission(hreq);

		    SecurityContext.setCurrent
			(getSecurityContextForPrincipal(prin));
		}
	    }
        }
        // END OF IASRI 4713234
        //} catch (Exception ex) {
            /** IASRI 4660742
            ex.printStackTrace();
	          **/
	          // START OF IASRI 4660742
                  //_logger.log(Level.SEVERE,"web_server.excep_handle_before_event",ex);
	          // END OF IASRI 4660742
        //}
        // END IASRI 4688449

        ComponentInvocation inv = new ComponentInvocation(instance, context);
        try {
            im.preInvoke(inv);
            if (eventType.equals(InstanceEvent.BEFORE_SERVICE_EVENT)) {
                // enlist resources with TM for service method
                Transaction tran = null;
                if ((tran = tm.getTransaction()) != null) {
                    inv.setTransaction(tran);
                }
                tm.enlistComponentResources();
            } else if (eventType.equals(InstanceEvent.BEFORE_INIT_EVENT)) {
                
                // Perform any required resource injection on the servlet 
                // instance.  This needs to be done after the invocation of
                // preInvoke, but before any of the servlet's application
                // code is executed.
                JndiNameEnvironment desc = (JndiNameEnvironment) 
                    Switch.getSwitch().getDescriptorFor(context);

                // There won't be a corresponding J2EE naming environment
                // descriptor for certain internal servlets so just skip
                // injection for those ones.
                if( desc != null
                        && instance.getClass() != DefaultServlet.class
                        && instance.getClass() != JspServlet.class) {
                    injectionMgr.injectInstance(instance, desc);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                _logger.getResourceBundle().getString(
                    "web_server.excep_handle_before_event"),
                ex);
        }
    }


    private static javax.security.auth.AuthPermission doAsPrivilegedPerm =
 	new javax.security.auth.AuthPermission("doAsPrivileged");

 
    private static void checkObjectForDoAsPermission(final Object o)
            throws AccessControlException{

	if (System.getSecurityManager() != null) {
	    AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
		    ProtectionDomain pD = o.getClass().getProtectionDomain();
		    Policy p = Policy.getPolicy();
		    if (!p.implies(pD,doAsPrivilegedPerm)) {
			throw new AccessControlException
			    ("permission required to override getUserPrincipal",
			     doAsPrivilegedPerm);
		    }
		    return null;
		}
	    });
	}
    }

    private static SecurityContext 
    getSecurityContextForPrincipal(final Principal p) {
	if (p == null) {
	    return null;
	} else if (p instanceof WebPrincipal) {
	    return ((WebPrincipal) p).getSecurityContext();
	} else {
	    return (SecurityContext) 
		AccessController.doPrivileged(new PrivilegedAction() {
		    public Object run() {
			Subject s = new Subject();
			s.getPrincipals().add(p);
			return new SecurityContext(p.getName(),s);
		    }
		});
	}
    }

    private void handleAfterEvent(InstanceEvent event, String eventType) {

        if (AppVerification.doInstrument() 
                && (eventType.equals(InstanceEvent.AFTER_SERVICE_EVENT)
                        || eventType.equals(InstanceEvent.AFTER_INIT_EVENT)
                        || eventType.equals(InstanceEvent.AFTER_DISPATCH_EVENT))) {
            
            AppVerification.getInstrumentLogger().doInstrumentForWeb(event);
        }
        
        Object instance = null;
        if (eventType.equals(InstanceEvent.AFTER_FILTER_EVENT)) {
            instance = event.getFilter();
        } else {
            instance = event.getServlet();
        }
        Context context = (Context) event.getWrapper().getParent();
        ComponentInvocation inv = new ComponentInvocation(instance, context);
        try {
            im.postInvoke(inv);
        } catch (Exception ex) {
            throw new RuntimeException(
                _logger.getResourceBundle().getString(
                    "web_server.excep_handle_after_event"),
                ex);
        } finally {
            if (eventType.equals(InstanceEvent.AFTER_DESTROY_EVENT)) {
                tm.componentDestroyed(instance);                
                JndiNameEnvironment desc = (JndiNameEnvironment) 
                    Switch.getSwitch().getDescriptorFor(context);
                if (desc != null
                        && instance.getClass() != DefaultServlet.class
                        && instance.getClass() != JspServlet.class) {
                    try {
                        injectionMgr.invokeInstancePreDestroy(instance, desc);
                    } catch (InjectionException ie) {
                        _logger.log(Level.SEVERE,
                                    "web_server.excep_handle_after_event",
                                    ie);
                    }
                }
            }
            if (eventType.equals(InstanceEvent.AFTER_FILTER_EVENT) ||
                eventType.equals(InstanceEvent.AFTER_SERVICE_EVENT)) {
                // check it's top level invocation
                // BEGIN IASRI# 4646060
                if (im.getCurrentInvocation() == null) {
                // END IASRI# 4646060
                    try {
                        // clear security context
                        Realm ra = context.getRealm();
                        if (ra != null && (ra instanceof RealmAdapter)) {
                            ((RealmAdapter)ra).logout();
                        }
                    } catch (Exception ex) {
                        /** IASRI 4660742
                        ex.printStackTrace();
                        **/
                        // START OF IASRI 4660742
                            _logger.log(Level.SEVERE,
                                        "web_server.excep_handle_after_event",
                                        ex);
                        // END OF IASRI 4660742
                    }
                    try {
                        if (tm.getTransaction() != null) {
                            tm.rollback();
                        }
                        tm.cleanTxnTimeout();
                    } catch (Exception ex) {}
                }
                tm.componentDestroyed(instance);
            }
        }
    }
}

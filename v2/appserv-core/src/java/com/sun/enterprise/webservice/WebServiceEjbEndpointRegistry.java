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

package com.sun.enterprise.webservice;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

// jaxrpc spi
import com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.webservice.monitoring.WebServiceEngineImpl;
import com.sun.enterprise.webservice.monitoring.WebServiceEngineFactory;
import com.sun.ejb.containers.StatelessSessionContainer;        
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;

/**
 * This class acts as a registry of all the webservice EJB end points
 * enabled in this application server. This a singleton class, use 
 * getRegistry() to obtain the registry instance
 *
 * @author  Jerome Dochez
 */
public class WebServiceEjbEndpointRegistry {
    
    private static StringManager localStrings =
        StringManager.getManager( WebServiceEjbEndpointRegistry.class );
    Logger logger = LogDomains.getLogger(LogDomains.EJB_LOGGER);
    
    private static final WebServiceEjbEndpointRegistry registry = new WebServiceEjbEndpointRegistry();
    
    // Ejb service endpoint info.  
    private Hashtable webServiceEjbEndpoints = new Hashtable();

    // Derived set of all ejb web service related context roots.  Used
    // to optimize the check that determines whether an HTTP request is 
    // for an ejb.  NOTE that ejb endpoints may share the same context
    // root, but that context root must not be used by any web application.  
    // So if the context root portion of the request is in this set, we know
    // the call is for an ejb.
    private Set ejbContextRoots = new HashSet();
    

    // This keeps the list for each service
    private HashMap adapterListMap = new HashMap();
    
    /** Creates a new instance of WebServiceEjbEndpointRegistry */
    private WebServiceEjbEndpointRegistry() {
    }
    
    /**
     * @return the registry instance
     */
    public static WebServiceEjbEndpointRegistry getRegistry() {
        return registry;        
    }
    
    public void registerEjbWebServiceEndpoint(EjbRuntimeEndpointInfo endpoint) throws Exception {
        String ctxtRoot;
        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpoint.getEndpointAddressUri();
            String uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
            if (webServiceEjbEndpoints.containsKey(uri)) {
                logger.log(Level.SEVERE, 
                        localStrings.getString("enterprise.webservice.duplicateService", 
                        new Object[]{uri}));
            }            
            webServiceEjbEndpoints.put(uri, endpoint);
            regenerateEjbContextRoots();
            ctxtRoot = getContextRootForUri(uri);
            if(adapterListMap.get(ctxtRoot) == null) {
                ServletAdapterList list = new ServletAdapterList();
                adapterListMap.put(ctxtRoot, list);
            }
        }
        
        // notify monitoring layers that a new endpoint is being created.
        WebServiceEngineImpl engine = (WebServiceEngineImpl) WebServiceEngineFactory.getInstance().getEngine();
        if (endpoint.getEndpoint().getWebService().getMappingFileUri()!=null) {
            engine.createHandler((com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate)null, endpoint.getEndpoint());
        } else {
            engine.createHandler(endpoint.getEndpoint());
            // Safe to assume that it's a JAXWS endpoint
            endpoint.initRuntimeInfo((ServletAdapterList)adapterListMap.get(ctxtRoot));
        }
    }

    public void unregisterEjbWebServiceEndpoint(String endpointAddressUri) {
        
        EjbRuntimeEndpointInfo endpoint = null;
        
        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpointAddressUri;
            String uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
            String ctxtRoot = getContextRootForUri(uri);
            ServletAdapterList list = (ServletAdapterList)adapterListMap.get(ctxtRoot);
            if(list != null) {
                for(ServletAdapter x : list) {
                    x.getEndpoint().dispose();
                }
            }
            endpoint = (EjbRuntimeEndpointInfo) webServiceEjbEndpoints.remove(uri);
            regenerateEjbContextRoots();
        }
        
        if (endpoint==null) {
            return;
        }
        
        // notify the monitoring layers that an endpoint is destroyed
        WebServiceEngineImpl engine = (WebServiceEngineImpl) WebServiceEngineFactory.getInstance().getEngine();
        engine.removeHandler(endpoint.getEndpoint());
    }
    
    /**
     * Creates a new EjbRuntimeEndpointInfo instance depending on the type
     * and version of the web service implementation.
     * @param   
     */
    public EjbRuntimeEndpointInfo createEjbEndpointInfo(WebServiceEndpoint webServiceEndpoint,
                                  StatelessSessionContainer ejbContainer, 
                                  Object servant, Class tieClass) {
        EjbRuntimeEndpointInfo info;
        if ("1.1".compareTo(webServiceEndpoint.getWebService().getWebServicesDescriptor().getSpecVersion())>=0) {
            info = new Ejb2RuntimeEndpointInfo(webServiceEndpoint, ejbContainer, servant, tieClass);
        } else {
            info = new EjbRuntimeEndpointInfo(webServiceEndpoint, ejbContainer, servant);
        }
        return info;
    }

    public EjbRuntimeEndpointInfo getEjbWebServiceEndpoint
        (String uriRaw, String method, String query) {
        EjbRuntimeEndpointInfo endpoint = null;
        
        if (uriRaw==null || uriRaw.length()==0) {
            return null;
        }
        
        // Strip off any leading slash.
        String uri = (uriRaw.charAt(0) == '/') ? uriRaw.substring(1) : uriRaw;

        synchronized(webServiceEjbEndpoints) {

            if( method.equals("GET") ) {
                // First check for a context root match so we avoid iterating  
                // through all ejb endpoints.  This logic will be used for
                // all HTTP GETs, so it's important to reduce the overhead in
                // the likely most common case that the request is for a web 
                // component.
                String contextRoot = getContextRootForUri(uri);
                if( ejbContextRoots.contains(contextRoot) ) {
                    // Now check for a match with a specific ejb endpoint.
                    Collection values = webServiceEjbEndpoints.values();
                    for(Iterator iter = values.iterator(); iter.hasNext();) {
                        EjbRuntimeEndpointInfo next = (EjbRuntimeEndpointInfo)
                            iter.next();
                        if( next.getEndpoint().matchesEjbPublishRequest
                            (uri, query)) {
                            endpoint = next;
                            break;
                        }
                    }
                }
            } else {
                // In this case the uri must match exactly to be an ejb web
                // service invocation, so do a direct table lookup.
                endpoint = (EjbRuntimeEndpointInfo) 
                    webServiceEjbEndpoints.get(uri);
            }
        }
        return endpoint;
    }

    public Collection getEjbWebServiceEndpoints() {
        return webServiceEjbEndpoints.entrySet();
    }

    private String getContextRootForUri(String uri) {
        StringTokenizer tokenizer = new StringTokenizer(uri, "/");
        if (tokenizer.hasMoreTokens()) {
            return tokenizer.nextToken();
        } else {
            return null;
        }
        
    }

    private void regenerateEjbContextRoots() {
        synchronized(webServiceEjbEndpoints) {
            Set contextRoots = new HashSet();
            for(Iterator iter = webServiceEjbEndpoints.keySet().iterator(); 
                iter.hasNext();) {
                String uri = (String) iter.next();
                String contextRoot = getContextRootForUri(uri);
                if( (contextRoot != null) && !contextRoot.equals("") ) {
                    contextRoots.add(contextRoot);
                }
            }
            ejbContextRoots = contextRoots;
        }
    }
    
}
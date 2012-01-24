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

/*
 * MemoryStrategyBuilder.java
 *
 * Created on September 30, 2002, 12:24 PM
 */

package com.sun.enterprise.web;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import org.apache.catalina.Context;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import com.sun.enterprise.deployment.runtime.web.SessionManager;
import com.sun.enterprise.util.uuid.UuidGenerator;

public class MemoryStrategyBuilder extends BasePersistenceStrategyBuilder implements PersistenceStrategyBuilder{
    
    /** Creates a new instance of MemoryStrategyBuilder */
    /*
    public MemoryStrategyBuilder() {
    }*/
      
    
    public void initializePersistenceStrategy(Context ctx, SessionManager smBean) {
        super.initializePersistenceStrategy(ctx, smBean);
        String persistenceType = "memory";        
        String ctxPath = ctx.getPath();
        if(ctxPath != null && !ctxPath.equals("")) {    
            Object[] params = { ctx.getPath(), persistenceType };
            _logger.log(Level.FINE, "webcontainer.noPersistence", params); 
        }
        StandardManager mgr = new StandardManager();
        //remove this fix because we don't want unload persistence
        //unless sessionFilename has been set explicitly
        //following line replaces commented out code
        /*
        if (sessionFilename != null) {
            mgr.setPathname(sessionFilename);
        }
         */
        if (sessionFilename == null) {
            mgr.setPathname(sessionFilename);
        } else {
            mgr.setPathname(prependContextPathTo(sessionFilename, ctx));
        }
        StandardContext sctx = (StandardContext) ctx;
        sctx.restrictedSetPipeline(new WebPipeline(sctx));         
        
        mgr.setMaxActiveSessions(maxSessions);
        //START OF 6364900
        mgr.setSessionLocker(new PESessionLocker(ctx));
        //END OF 6364900        
        //FIXME: what is the replacement for setCheckInterval
        //mgr.setCheckInterval(reapInterval);
        ctx.setManager(mgr);

        // START CR 6275709
        if (sessionIdGeneratorClassname != null) {
            try {
                UuidGenerator generator = (UuidGenerator)
                    Class.forName(sessionIdGeneratorClassname).newInstance();
                mgr.setUuidGenerator(generator);
            } catch (Exception ex) {
                _logger.log(Level.SEVERE,
                            "Unable to load session uuid generator "
                            + sessionIdGeneratorClassname,
                            ex);
            }
        }
        // END CR 6275709
        
        //this must be after ctx.setManager(mgr);       
        if(!sctx.isSessionTimeoutOveridden()) {
            mgr.setMaxInactiveInterval(sessionMaxInactiveInterval); 
        }        
    }
    
}
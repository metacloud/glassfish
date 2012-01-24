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

package com.sun.enterprise.config.serverbeans.validation;

import java.util.HashMap;
import java.util.Map;


/**
   A class that provides access to a collection of frames
*/


/**
 * FrameHolder.java
 *
 * @author <a href="mailto:toby.h.ferguson@sun.com">Toby H Ferguson</a>
 * @version $Revision: 1.3 $
 */

class FrameHolder {
    public boolean equals(Object o){
        return o == this ||
        (o != null && o instanceof FrameHolder && this.equals((FrameHolder) o));
    }

    private boolean equals(FrameHolder o){
        return this.domain.equals( o.domain) &&
        this.configFrames.equals(o.configFrames) &&
        this.serverFrames.equals(o.serverFrames) &&
        this.clusterFrames.equals(o.clusterFrames);
    }

    public int hashCode(){
        return getDomainFrame().hashCode();
    }
    
    Frame getDomainFrame(){
        return domain;
    }

    Frame getConfigFrame(String n) {
        return memoizedGet(configFrames, n);
    }

    Frame getServerFrame(String n){
        return memoizedGet(serverFrames, n);
    }

    Frame getClusterFrame(String n){
        return memoizedGet(clusterFrames, n);
    }

    private final Frame memoizedGet(Map m, String n){
        Frame result = (Frame) m.get(n);
        if (result == null) {
            result = Frame.newFrame();
            m.put(n, result);
        }
        return result;
    }


    private final Frame domain = Frame.newFrame();
    private final Map configFrames = new HashMap();
    private final Map serverFrames = new HashMap();
    private final Map clusterFrames = new HashMap();
    
    
    
}
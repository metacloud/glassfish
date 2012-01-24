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
 * $Id: StatisticFactory.java,v 1.3 2005/12/25 03:43:45 tcfujii Exp $
 * $Date: 2005/12/25 03:43:45 $
 * $Revision: 1.3 $
 */

package com.sun.enterprise.admin.monitor.stats.spi;
import javax.management.j2ee.statistics.*;
import com.sun.enterprise.admin.monitor.stats.*;

public class StatisticFactory {
    
    /** Creates a new instance of StatisticFactory */
    public StatisticFactory() {
    }
    
    /**
     * returns an instance of the CountStatistic
     * @return  CountStatistic
     */
    public static CountStatistic getCountStatistic(long countVal, String name,
                                                   String unit, String desc,
                                                   long sampleTime, long startTime) {
                                                       
        return new CountStatisticImpl(countVal, name, unit, desc, 
                                      sampleTime, startTime);
    }
    
    /**
     * returns an instance of the RangeStatistic
     * @return  RangeStatistic
     */
    public static RangeStatistic getRangeStatistic(long curVal, long highMark,
                                                   long lowMark, String name,
                                                   String unit, String desc,
                                                   long startTime, long sampleTime) {
        
        return new RangeStatisticImpl(curVal, highMark, lowMark, name, 
                                      unit, desc, startTime, sampleTime);
    }
    
    /**
     * returns an instance of the BoundaryStatistic
     * @return  BoundaryStatistic
     */
    public static BoundaryStatistic getBoundaryStatistic(long lower, long upper,
                                                         String name, String unit,
                                                         String desc, long startTime,
                                                         long sampleTime) {
        
        return new BoundaryStatisticImpl(lower, upper, name, unit, desc, 
                                         startTime, sampleTime);
    }
    
    /**
     * returns an instance of the BoundedRangeStatistic
     * @return  BoundedRangeStatistic
     */
    public static BoundedRangeStatistic getBoundedRangeStatistic(long curVal, long highMark,
                                                                 long lowMark, long upper,
                                                                 long lower, String name,
                                                                 String unit, String desc,
                                                                 long startTime, long sampleTime) {
        
        return new BoundedRangeStatisticImpl(curVal, highMark, lowMark, 
                                             upper, lower, name, unit, 
                                             desc, startTime, sampleTime);
    }
    
    /** 
     * returns an instance of the TimeStatistic
     * @return TimeStatistic
     */
    public static TimeStatistic getTimeStatistic(long counter, long maxTime,
                                                 long minTime, long totalTime, 
                                                 String name, String unit,
                                                 String desc, long startTime,
                                                 long sampleTime) {
                                                     
        return new TimeStatisticImpl(counter, maxTime, minTime, totalTime, 
                                     name, unit, desc, startTime, sampleTime);
                                     
    }
}
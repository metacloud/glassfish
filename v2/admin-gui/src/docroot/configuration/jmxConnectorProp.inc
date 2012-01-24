<!--
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 
 Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 
 The contents of this file are subject to the terms of either the GNU
 General Public License Version 2 only ("GPL") or the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License. You can obtain
 a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 
 When distributing the software, include this License Header Notice in each
 file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 Sun designates this particular file as subject to the "Classpath" exception
 as provided by Sun in the GPL Version 2 section of the License file that
 accompanied this code.  If applicable, add the following below the License
 Header, with the fields enclosed by brackets [] replaced by your own
 identifying information: "Portions Copyrighted [year]
 [name of copyright owner]"
 
 Contributor(s):
 
 If you wish your version of this file to be governed by only the CDDL or
 only the GPL Version 2, indicate your decision by adding "[Contributor]
 elects to include this software in this distribution under the [CDDL or GPL
 Version 2] license."  If you don't indicate a single choice of license, a
 recipient has the option to distribute your version of this file under
 either the CDDL, the GPL Version 2 or to extend the choice of license to
 its licensees as provided above.  However, if you add GPL Version 2 code
 and therefore, elected the GPL Version 2 license, then the option applies
 only if the new code is made subject to such option by the copyright
 holder.
-->
<!-- configuration/jmxConnectorProp.inc -->    
    <!-- PropertySheet .... -->
    <sun:propertySheet id="propertySheet">			 
        <!-- General Settings section -->   
        <sun:propertySheetSection id="generalSettingsSetion" label="$resource{i18n.edJmxConnector.generalSettingsLabel}">
            <sun:property id="JmxNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.jmxNameLabel}" >
                <sun:staticText id="JmxName" text="#{jmxName}" /> 
            </sun:property> 
            <sun:property id="JmxProtocolProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.jmxProtocolLabel}" >
                <sun:staticText id="JmxProtocol" text="#{jmxProtocol}" /> 
            </sun:property> 
            <sun:property id="JmxServUrlProp" rendered="#{! isNodeAgent}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.jmxServUrlLabel}" >
                <sun:staticText id="JmxServUrl" text="#{jmxServUrl}" /> 
            </sun:property> 
            <sun:property id="SecurityProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.securityLabel}" >
                <sun:checkbox id="Security" label="$resource{i18n.desc.Enabled}" selected="#{requestScope.security}" />
            </sun:property>
            <sun:property id="AddressProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.addressLabel}" helpText="$resource{i18n.edJmxConnector.addressHelp}" >
                <sun:textField id="Address" styleClass="required" columns="$int{40}" text="#{requestScope.address}" required="#{true}" />      
            </sun:property>
            <sun:property id="PortProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.portLabel}" helpText="$resource{i18n.edJmxConnector.portHelp}" >
                <sun:textField id="Port" styleClass="required port" columns="$int{40}" text="#{requestScope.port}" required="#{true}" />      
            </sun:property>
            <sun:property id="RealmNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.realmNameLabel}" helpText="$resource{i18n.edJmxConnector.realmNameHelp}" >
                <sun:textField id="RealmName" styleClass="required" columns="$int{40}" text="#{requestScope.realmName}" required="#{true}" />      
            </sun:property>
            <sun:property id="AcceptProp" rendered="#{isNodeAgent}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.nodeAgent.acceptLabel}" helpText="$resource{i18n.nodeAgent.acceptHelp}" >
                <sun:checkbox label="$resource{i18n.desc.Enabled}" selected="#{requestScope.accept}" />   
            </sun:property>
            <sun:property id="clientHostname" rendered="#{supportCluster}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.edJmxConnector.clientHostLabel}" helpText="$resource{i18n.edJmxConnector.clientHostLabelHelp}" >
               <sun:textField id="clientHostname" styleClass="required" columns="$int{40}" text="#{requestScope.clientHostname}" required="#{true}" />        
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>  
	
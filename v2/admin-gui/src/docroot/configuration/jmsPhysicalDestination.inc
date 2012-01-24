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
    
<!-- Page Title -->
<sun:title id="propertyContentPage" title="#{pageTitle}" helpText="#{pageTitleHelp}">
    <!facet pageButtonsTop>
    <sun:panelGroup id="topButtons">
     <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"     
        onClick="return guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}'); " >   
        <!command
            getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
            getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
            convertRowsToProperties(NewList="#{newList}", AddProps=>$attribute{addProps});
            savePhysicalDestinations(
            ConfigName="#{configName}",
            Name="#{name}",
            Edit="#{edit}",                        
            AddProps="${addProps}",
            Type="#{type}",
            );            
            redirect(page="jmsPhysicalDestinations.jsf?configName=#{configName}")
        />
    </sun:button>
    <sun:button id="cancelButton" rendered="#{!edit}" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}">
        <!command
            redirect(page="jmsPhysicalDestinations.jsf?configName=#{configName}");
        />
  </sun:button>

</sun:panelGroup>
</facet>   
</sun:title>
<!-- PropertySheet .... -->
<sun:propertySheet id="propertySheet">			 
        <event>
        <!beforeCreate
        getJmsDestinationSettings(
        Edit="#{edit}",
        ConfigName="#{configName}", 
        Name="#{name}",
        Type=>$attribute{type});            
        getTableList(Properties="#{props}", TableList=>$attribute{tableList});
        />
        </event>

        <sun:propertySheetSection id="propertSectionTextField">
            <sun:property id="NameProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" >
                <sun:staticText id="NameStatic" text="#{name}"/> 
            </sun:property> 

            <sun:property id="NameTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}">
                <sun:textField id="NameText" styleClass="required" text="#{name}" columns="$int{55}" required="#{true}" /> 
            </sun:property> 
            <sun:property id="typeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jmsPhysDestinations.Type}">                           
                <sun:dropDown id="Type" labels={" " "javax.jms.Topic" "javax.jms.Queue"} values={" " "topic" "queue"} selected="#{type}" >
                </sun:dropDown>
            </sun:property> 
        </sun:propertySheetSection>
</sun:propertySheet>    

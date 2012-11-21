/*
 * Copyright 2010 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.tem.document.authorization;

import java.util.Set;

import org.kuali.kfs.module.tem.TemConstants;
import org.kuali.kfs.module.tem.TemConstants.TravelAuthorizationParameters;
import org.kuali.kfs.module.tem.TemConstants.TravelEditMode;
import org.kuali.kfs.module.tem.TemConstants.TravelParameters;
import org.kuali.kfs.module.tem.TemKeyConstants;
import org.kuali.kfs.module.tem.TemParameterConstants;
import org.kuali.kfs.module.tem.TemWorkflowConstants;
import org.kuali.kfs.module.tem.businessobject.TEMProfile;
import org.kuali.kfs.module.tem.document.TravelDocument;
import org.kuali.kfs.module.tem.service.TEMRoleService;
import org.kuali.kfs.module.tem.service.TemProfileService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.document.authorization.FinancialSystemTransactionalDocumentPresentationControllerBase;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.document.authorization.DocumentAuthorizer;
import org.kuali.rice.kns.service.DocumentHelperService;
import org.kuali.rice.kns.service.ParameterService;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;

/**
 * Travel Document Presentation Controller
 * 
 */
public class TravelDocumentPresentationController extends FinancialSystemTransactionalDocumentPresentationControllerBase{

    /**
     * @see org.kuali.kfs.sys.document.authorization.FinancialSystemTransactionalDocumentPresentationControllerBase#getEditModes(org.kuali.rice.kns.document.Document)
     */
    @Override
    public Set<String> getEditModes(Document document) {
        Set<String> editModes = super.getEditModes(document);
        ParameterService paramService = SpringContext.getBean(ParameterService.class);

        if (paramService.getIndicatorParameter(TemParameterConstants.TEM_AUTHORIZATION.class, TravelAuthorizationParameters.ENABLE_CONTACT_INFORMATION_IND)) {
            editModes.add(TemConstants.DISPLAY_EMERGENCY_CONTACT_TAB);
        }
        return editModes;
    }
    
    /**
     * 
     * 
     * @see org.kuali.rice.kns.document.authorization.DocumentPresentationControllerBase#canEdit(org.kuali.rice.kns.document.Document)
     */
    @Override
    protected boolean canEdit(Document document) {
        Person currentUser = GlobalVariables.getUserSession().getPerson();
        TravelDocument travelDocument = (TravelDocument) document;
        KualiWorkflowDocument workflowDocument = document.getDocumentHeader().getWorkflowDocument();
        
        if ((workflowDocument.stateIsInitiated() || workflowDocument.stateIsSaved())) {
            //check if the user can access the travel document as arranger
            boolean arrangerAccess = getTemRoleService().canAccessTravelDocument(travelDocument, currentUser);
            boolean isTraveler = currentUser.getPrincipalId().equals(travelDocument.getTraveler().getPrincipalId());
            
            //if user does not have the access, user is not enabled as document manager throw doc exception with travel document edit error
            // also check if user is NOT the traveler
            
            //NOTE: it doesn't get to here if the edit permission is set correctly
            //NOTE2: cannot add traveler to the edit permission as it creates a circular role lookup 
            if (!arrangerAccess && !isTraveler && !enableForDocumentManager(currentUser, false)){
                throw new DocumentInitiationException(TemKeyConstants.ERROR_TRAVEL_DOCUMENT_EDIT, new String[] {travelDocument.getDocumentTypeName()}, true);
            }
        }
        
        return super.canEdit(travelDocument);
    }
    
    /**
     * @see org.kuali.rice.kns.document.authorization.DocumentPresentationControllerBase#canInitiate(java.lang.String)
     */
    @Override
    public boolean canInitiate(String documentTypeName) {
        Person currentUser = GlobalVariables.getUserSession().getPerson();
        if (!getTemRoleService().isTravelArranger(currentUser)) {
            TEMProfile temProfile = getTemProfileService().findTemProfileByPrincipalId(currentUser.getPrincipalId());
            if (temProfile == null) {
                throw new DocumentInitiationException(TemKeyConstants.ERROR_TRAVEL_DOCUMENT_INITIATION, new String[] { documentTypeName }, true);
            }
        }

        return super.canInitiate(documentTypeName);
    }

    /**
     * Check user's edit permission in order to grant full entry edit
     * 
     * When the document is routed to document manager's approval node, check for permission on document
     * manager to get full edit 
     * 
     * @param document
     * @param editModes
     */
    protected void addFullEntryEditMode(Document document, Set<String> editModes) {
        KualiWorkflowDocument workflowDocument = document.getDocumentHeader().getWorkflowDocument();
        Person currentUser = GlobalVariables.getUserSession().getPerson();
        
        //check edit permission when document is in init or saved
        if ((workflowDocument.stateIsInitiated() || workflowDocument.stateIsSaved())) {
            DocumentAuthorizer authorizer = getDocumentHelperService().getDocumentAuthorizer(document);
            TravelArrangeableAuthorizer travelAuthorizer = (TravelArrangeableAuthorizer)authorizer;
            //check for edit permission on the document
            if (travelAuthorizer.canEditDocument(document, currentUser)){
                editModes.add(TravelEditMode.FULL_ENTRY);
            }
        }
        
        //Document manager will also get full entry edit mode on the approval node
        if(workflowDocument.getCurrentRouteNodeNames().equals(getDocumentManagerApprovalNode()) 
                && enableForDocumentManager(currentUser)){
            editModes.add(TravelEditMode.FULL_ENTRY);
        }        
    }
    
    /**
     * Get the Document Manager approval node 
     * 
     * Default to Travel Manager's - AP_TRAVEL
     * 
     * @return
     */
    public String getDocumentManagerApprovalNode(){
        return TemWorkflowConstants.RouteNodeNames.AP_TRAVEL;
    }

    /**
     * Enable specifically for document manager, default to check for parameter setup
     * 
     * @param workflowDocument
     * @return
     */
    public boolean enableForDocumentManager(Person currentUser){
        return enableForDocumentManager(currentUser, true);
    }
    

    /**
     * Enable specifically for document manager 
     * 
     * 1) user is Travel Manager 
     * 2) parameter allow travel office has full edit
     * 
     * @param workflowDocument
     * @return
     */
    public boolean enableForDocumentManager(Person currentUser, boolean checkParameters){
        boolean isTravelManager = getTemRoleService().isTravelManager(currentUser); 
        boolean allowUpdate = checkParameters? getParamService().getIndicatorParameter(TemParameterConstants.TEM_DOCUMENT.class, TravelParameters.ALLOW_TRAVEL_OFFICE_TO_MODIFY_ALL_IND) : true;
        
        //specifically enabled on AP node on full edit
        boolean isEnabled = isTravelManager && allowUpdate;
        return isEnabled;
    }
    
    /**
     * Check current user is the initiator
     * 
     * @param workflowDocument
     * @return
     */
    public boolean isInitiator(KualiWorkflowDocument workflowDocument){
        String docInitiator = workflowDocument.getInitiatorPrincipalId(); 
        Person currentUser = GlobalVariables.getUserSession().getPerson();
        return docInitiator.equals(currentUser.getPrincipalId());
    }
    
    public ParameterService getParamService() {
        return SpringContext.getBean(ParameterService.class);
    }
    
    protected TEMRoleService getTemRoleService() {
        return SpringContext.getBean(TEMRoleService.class);
    }
    
    protected TemProfileService getTemProfileService() {
        return SpringContext.getBean(TemProfileService.class);
    }
    
    protected DocumentHelperService getDocumentHelperService() {
        return SpringContext.getBean(DocumentHelperService.class);
    }
    
}
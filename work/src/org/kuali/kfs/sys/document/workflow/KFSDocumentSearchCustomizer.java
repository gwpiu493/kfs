/*
 * Copyright 2012 The Kuali Foundation.
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.sys.document.workflow;
//RICE20 Hook to document type is not working right now but needs to be changed to support pre-rice2.0 release
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.core.api.uif.RemotableAttributeError;
import org.kuali.rice.core.api.uif.RemotableAttributeField;
import org.kuali.rice.kew.api.document.DocumentWithContent;
import org.kuali.rice.kew.api.document.attribute.DocumentAttribute;
import org.kuali.rice.kew.api.document.attribute.WorkflowAttributeDefinition;
import org.kuali.rice.kew.api.document.search.DocumentSearchCriteria;
import org.kuali.rice.kew.api.document.search.DocumentSearchResult;
import org.kuali.rice.kew.api.extension.ExtensionDefinition;
import org.kuali.rice.kew.framework.document.attribute.SearchableAttribute;
import org.kuali.rice.kew.framework.document.search.DocumentSearchCustomizer;
import org.kuali.rice.kew.framework.document.search.DocumentSearchResultSetConfiguration;
import org.kuali.rice.kew.framework.document.search.DocumentSearchResultValues;
import org.kuali.rice.kew.framework.document.search.StandardResultField;

public class KFSDocumentSearchCustomizer implements SearchableAttribute, DocumentSearchCustomizer {

    protected SearchableAttribute searchableAttribute;

    public KFSDocumentSearchCustomizer() {
        this(new FinancialSystemSearchableAttribute());
    }

    public KFSDocumentSearchCustomizer(SearchableAttribute searchableAttribute) {
        this.searchableAttribute = searchableAttribute;
    }

    @Override
    public DocumentSearchResultValues customizeResults(DocumentSearchCriteria documentSearchCriteria, List<DocumentSearchResult> defaultResults) {
        return null;
    }

    @Override
    public final String generateSearchContent(ExtensionDefinition extensionDefinition,
            String documentTypeName,
            WorkflowAttributeDefinition attributeDefinition) {
        return getSearchableAttribute().generateSearchContent(extensionDefinition, documentTypeName, attributeDefinition);
    }

    @Override
    public final List<DocumentAttribute> extractDocumentAttributes(ExtensionDefinition extensionDefinition,
            DocumentWithContent documentWithContent) {
        return getSearchableAttribute().extractDocumentAttributes(extensionDefinition, documentWithContent);
    }

    @Override
    public final List<RemotableAttributeField> getSearchFields(ExtensionDefinition extensionDefinition,
            String documentTypeName) {
        return getSearchableAttribute().getSearchFields(extensionDefinition, documentTypeName);
    }

    @Override
    public final List<RemotableAttributeError> validateDocumentAttributeCriteria(ExtensionDefinition extensionDefinition,
            DocumentSearchCriteria documentSearchCriteria) {
        return getSearchableAttribute().validateDocumentAttributeCriteria(extensionDefinition, documentSearchCriteria);
    }

    protected SearchableAttribute getSearchableAttribute() {
        return this.searchableAttribute;
    }

    public void setSearchableAttribute(SearchableAttribute searchableAttribute) {
        this.searchableAttribute = searchableAttribute;
    }

    @Override
    public DocumentSearchCriteria customizeCriteria(DocumentSearchCriteria documentSearchCriteria) {
        if ( documentSearchCriteria.getDocumentAttributeValues().containsKey( FinancialSystemSearchableAttribute.DISPLAY_TYPE_SEARCH_ATTRIBUTE_NAME ) ) {
            DocumentSearchCriteria.Builder newCriteria = DocumentSearchCriteria.Builder.create(documentSearchCriteria);
            newCriteria.getDocumentAttributeValues().remove( FinancialSystemSearchableAttribute.DISPLAY_TYPE_SEARCH_ATTRIBUTE_NAME );
            return newCriteria.build();
        }
        return null;
    }

    @Override
    public DocumentSearchCriteria customizeClearCriteria(DocumentSearchCriteria documentSearchCriteria) {
        DocumentSearchCriteria.Builder newCriteria = DocumentSearchCriteria.Builder.create();
        newCriteria.setDocumentTypeName(documentSearchCriteria.getDocumentTypeName());
        return newCriteria.build();
    }

    protected static final List<StandardResultField> standardResultsToRemove = new ArrayList<StandardResultField>();
    static {
        standardResultsToRemove.add(StandardResultField.DOCUMENT_TYPE);
        standardResultsToRemove.add(StandardResultField.TITLE);
        standardResultsToRemove.add(StandardResultField.DATE_CREATED);
    }

    @Override
    public DocumentSearchResultSetConfiguration customizeResultSetConfiguration(DocumentSearchCriteria documentSearchCriteria) {
        DocumentSearchResultSetConfiguration.Builder config = DocumentSearchResultSetConfiguration.Builder.create();
        config.setOverrideSearchableAttributes(false);
        config.setStandardResultFieldsToRemove(standardResultsToRemove);

        List<String> displayTypeList = documentSearchCriteria.getDocumentAttributeValues().get(FinancialSystemSearchableAttribute.DISPLAY_TYPE_SEARCH_ATTRIBUTE_NAME);
        if ( displayTypeList != null && !displayTypeList.isEmpty() ) {

            String displayType =  displayTypeList.get(0);
            if ( StringUtils.equals(displayType, FinancialSystemSearchableAttribute.WORKFLOW_DISPLAY_TYPE_VALUE)) {
                config.setOverrideSearchableAttributes(true);
                config.setStandardResultFieldsToRemove(null);
            }
        }
        return config.build();
    }

    @Override
    public boolean isCustomizeCriteriaEnabled(String documentTypeName) {
        return true;
    }

    @Override
    public boolean isCustomizeClearCriteriaEnabled(String documentTypeName) {
        return true;
    }

    @Override
    public boolean isCustomizeResultsEnabled(String documentTypeName) {
        return false;
    }

    @Override
    public boolean isCustomizeResultSetFieldsEnabled(String documentTypeName) {
        return true;
    }


}
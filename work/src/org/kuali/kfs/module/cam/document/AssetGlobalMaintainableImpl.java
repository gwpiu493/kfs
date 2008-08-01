/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.kfs.module.cam.document;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.core.document.MaintenanceDocument;
import org.kuali.core.document.MaintenanceLock;
import org.kuali.core.maintenance.KualiGlobalMaintainableImpl;
import org.kuali.core.maintenance.Maintainable;
import org.kuali.core.service.BusinessObjectService;
import org.kuali.core.service.DateTimeService;
import org.kuali.core.util.ObjectUtils;
import org.kuali.core.util.TypedArrayList;
import org.kuali.core.web.ui.Section;
import org.kuali.kfs.coa.businessobject.ObjectCode;
import org.kuali.kfs.coa.service.ObjectCodeService;
import org.kuali.kfs.module.cam.CamsConstants;
import org.kuali.kfs.module.cam.CamsPropertyConstants;
import org.kuali.kfs.module.cam.businessobject.Asset;
import org.kuali.kfs.module.cam.businessobject.AssetDepreciationConvention;
import org.kuali.kfs.module.cam.businessobject.AssetGlobal;
import org.kuali.kfs.module.cam.businessobject.AssetGlobalDetail;
import org.kuali.kfs.module.cam.businessobject.AssetOrganization;
import org.kuali.kfs.module.cam.businessobject.AssetPayment;
import org.kuali.kfs.module.cam.businessobject.AssetPaymentDetail;
import org.kuali.kfs.module.cam.businessobject.defaultvalue.NextAssetNumberFinder;
import org.kuali.kfs.module.cam.document.service.AssetDateService;
import org.kuali.kfs.module.cam.document.service.AssetGlobalService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.context.SpringContext;

/**
 * This class overrides the base {@link KualiGlobalMaintainableImpl} to generate the specific maintenance locks for Global assets
 */
public class AssetGlobalMaintainableImpl extends KualiGlobalMaintainableImpl {

    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AssetGlobalMaintainableImpl.class);
    private static AssetGlobalService assetGlobalService = SpringContext.getBean(AssetGlobalService.class);

    /**
     * Get Asset from AssetGlobal
     * 
     * @see org.kuali.core.maintenance.KualiMaintainableImpl#processAfterNew(org.kuali.core.document.MaintenanceDocument, java.util.Map)
     */
    @Override
    public void processAfterNew(MaintenanceDocument document, Map<String, String[]> parameters) {
        LOG.info("processAfterNew CALLED....");
        
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        document.getNewMaintainableObject().setGenerateDefaultValues(false);
        
        // set asset and type code
        setSeparateSourceCapitalAssetNumber(assetGlobal, parameters);
        setFinancialDocumentTypeCode(assetGlobal, parameters);
        
        if (assetGlobalService.isAssetSeparateDocument(assetGlobal)) {
            
            /* XXX need asset reference in OJB
            assetGlobal.refreshReferenceObject("asset");
            if (ObjectUtils.isNotNull(assetGlobal.getSeparateSourceCapitalAsset())) {
                Asset asset = (Asset) businessObject;
                populateAssetInformation(assetGlobal, asset); 
                populateAssetSeparateAssetDetails(assetGlobal, asset, assetOrganization); 
                populateAssetSeparatePaymentDetails(assetGlobal, asset);
            }
            */
            
            populateAssetSeparateAssetDetails(assetGlobal, getAsset(assetGlobal), getAssetOrganization(assetGlobal));
            populateAssetSeparatePaymentDetails(assetGlobal, getAsset(assetGlobal));
            populateAssetInformation(assetGlobal, getAsset(assetGlobal));
            LOG.info("populateAssetInformation total: '" + assetGlobalService.totalPaymentByAsset(assetGlobal) + "'");
        }
        
        LOG.info("processAfterNew RETURN....");
        super.processAfterNew(document, parameters);
    }   
    
    /**
     * Get Asset from AssetGlobal
     * 
     * @param assetGlobal
     * @return Asset
     */
    private Asset getAsset(AssetGlobal assetGlobal){
        HashMap map = new HashMap();
        map.put(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, assetGlobal.getSeparateSourceCapitalAssetNumber());
        Asset asset = (Asset) SpringContext.getBean(BusinessObjectService.class).findByPrimaryKey(Asset.class, map);
        return asset;
    }

    /**
     * IN PROGRESS
     * Get AssetOrganization from AssetGlobal
     * 
     * @param assetGlobal
     * @return AssetOrganization
     */
    private AssetOrganization getAssetOrganization(AssetGlobal assetGlobal){
        HashMap map = new HashMap();
        map.put(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER, assetGlobal.getSeparateSourceCapitalAssetNumber());
        AssetOrganization assetOrganization = (AssetOrganization) SpringContext.getBean(BusinessObjectService.class).findByPrimaryKey(AssetOrganization.class, map);
        return assetOrganization;
    }
    
    /**
     * IN PROGRESS
     * Populate Asset Information for Asset Separate doc
     * 
     * @param assetGlobal
     * @param asset
     */
    private void populateAssetInformation(AssetGlobal assetGlobal, Asset asset) {
        LOG.info("populateAssetInformation CALLED....");
        
        // TODO working but not showing up
        assetGlobal.setSeparateSourceCapitalAssetNumber(asset.getCapitalAssetNumber());
        //assetGlobal.setSeparateSourceCapitalAssetNumber(Long.parseLong("1930")); // test
        LOG.info("populateAssetInformation asset: '" + asset.getCapitalAssetNumber() + "'");
        LOG.info("populateAssetInformation sep asset: '" + assetGlobal.getSeparateSourceCapitalAssetNumber() + "'");
        
        LOG.info("populateAssetInformation RETURN....");
    }  
    
    /**
     * IN PROGRESS
     * Populate Asset Details for Asset Separate doc
     * 
     * @param assetGlobal
     */
    private void populateAssetSeparateAssetDetails(AssetGlobal assetGlobal, Asset asset, AssetOrganization assetOrganization) {
        assetGlobal.setOrganizationOwnerAccountNumber(asset.getOrganizationOwnerAccountNumber());
        assetGlobal.setOrganizationOwnerChartOfAccountsCode(asset.getOrganizationOwnerChartOfAccountsCode());
        assetGlobal.setAgencyNumber(asset.getAgencyNumber());
        assetGlobal.setAcquisitionTypeCode(asset.getAcquisitionTypeCode());
        assetGlobal.setInventoryStatusCode(asset.getInventoryStatusCode());
        assetGlobal.setConditionCode(asset.getConditionCode());
        assetGlobal.setCapitalAssetDescription(asset.getCapitalAssetDescription());
        assetGlobal.setCapitalAssetTypeCode(asset.getCapitalAssetTypeCode());
        assetGlobal.setVendorName(asset.getVendorName());
        assetGlobal.setManufacturerName(asset.getManufacturerName());
        assetGlobal.setManufacturerModelNumber(asset.getManufacturerModelNumber());
        assetGlobal.setOrganizationText(assetOrganization.getOrganizationText());
        assetGlobal.setLastInventoryDate(new java.sql.Date(asset.getLastInventoryDate().getTime()));
        assetGlobal.setCreateDate(asset.getCreateDate());
        assetGlobal.setCapitalAssetInServiceDate(asset.getCapitalAssetInServiceDate());
        assetGlobal.setLandCountyName(asset.getLandCountyName());
        assetGlobal.setLandAcreageSize(asset.getLandAcreageSize());
        assetGlobal.setLandParcelNumber(asset.getLandParcelNumber());   
    }
    
    
    /**
     * IN PROGRESS
     * Populate Asset Payment Details for Asset Separate doc
     * 
     * @param assetGlobal
     */
    private void populateAssetSeparatePaymentDetails(AssetGlobal assetGlobal, Asset asset) {        
        // clear and create temp AssetPaymentDetail list
        assetGlobal.getAssetPaymentDetails().clear();
        List<AssetPaymentDetail> newAssetPaymentDetailList = assetGlobal.getAssetPaymentDetails();

        for (AssetPayment assetPayment : asset.getAssetPayments()) {
            // create new AssetPaymentDetail
            AssetPaymentDetail assetPaymentDetail = new AssetPaymentDetail();
            
            // populate AssetPaymentDetail with AssetPayment data
            assetPaymentDetail.setSequenceNumber(assetGlobal.incrementFinancialDocumentLineNumber());
            assetPaymentDetail.setChartOfAccountsCode(assetPayment.getChartOfAccountsCode());
            assetPaymentDetail.setAccountNumber(assetPayment.getAccountNumber());
            assetPaymentDetail.setSubAccountNumber(assetPayment.getSubAccountNumber());
            assetPaymentDetail.setFinancialObjectCode(assetPayment.getFinancialObjectCode());
            assetPaymentDetail.setFinancialSubObjectCode(assetPayment.getFinancialSubObjectCode());
            assetPaymentDetail.setProjectCode(assetPayment.getProjectCode());
            assetPaymentDetail.setOrganizationReferenceId(assetPayment.getOrganizationReferenceId());
            assetPaymentDetail.setExpenditureFinancialDocumentNumber(assetPayment.getDocumentNumber());
            assetPaymentDetail.setRequisitionNumber(assetPayment.getRequisitionNumber());
            assetPaymentDetail.setExpenditureFinancialDocumentPostedDate(assetPayment.getFinancialDocumentPostingDate());
            assetPaymentDetail.setFinancialDocumentPostingYear(assetPayment.getFinancialDocumentPostingYear());
            assetPaymentDetail.setFinancialDocumentPostingPeriodCode(assetPayment.getFinancialDocumentPostingPeriodCode());
            assetPaymentDetail.setAmount(assetPayment.getAccountChargeAmount());
            
            // add assetPaymentDetail to AssetPaymentDetail list
            newAssetPaymentDetailList.add(assetPaymentDetail);
        }

        // set AssetGlobal payment details with new payment details
        assetGlobal.setAssetPaymentDetails(newAssetPaymentDetailList);
    }
    
    
    /**
     * Get capital asset number from URL.
     * @see org.kuali.module.cams.lookup.AssetLookupableHelperServiceImpl#getSeparateUrl(BusinessObject)
     * 
     * @param assetGlobal
     * @param parameters
     */
    private void setSeparateSourceCapitalAssetNumber(AssetGlobal assetGlobal, Map<String, String[]> parameters) {
        String[] separateSourceCapitalAssetNumber = parameters.get(CamsPropertyConstants.AssetGlobal.SEPARATE_SOURCE_CAPITAL_ASSET_NUMBER);
        if (separateSourceCapitalAssetNumber != null) {
            assetGlobal.setSeparateSourceCapitalAssetNumber(Long.parseLong(separateSourceCapitalAssetNumber[0].toString()));
        }
    }
    
    /**
     * Get document type code from URL.
     * @see org.kuali.module.cams.lookup.AssetLookupableHelperServiceImpl#getSeparateUrl(BusinessObject)
     * 
     * @param assetGlobal
     * @param parameters
     */
    private void setFinancialDocumentTypeCode(AssetGlobal assetGlobal, Map<String, String[]> parameters) {
        String[] financialDocumentTypeCode = parameters.get(KFSPropertyConstants.FINANCIAL_DOCUMENT_TYPE_CODE);
        if (financialDocumentTypeCode != null) {
            assetGlobal.setFinancialDocumentTypeCode(financialDocumentTypeCode[0].toString());
        }
    }   
    
    /**
     * Hide specific document sections
     * 
     * @see org.kuali.core.maintenance.KualiMaintainableImpl#getCoreSections(org.kuali.core.maintenance.Maintainable)
     */
    @Override
    public List<Section> getCoreSections(Maintainable oldMaintainable) {
        List<Section> sections = super.getCoreSections(oldMaintainable);
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        
        // hide "Asset Information" section if doc is not "Asset Separate"
        if (!assetGlobalService.isAssetSeparateDocument(assetGlobal)) {
            for (Section section : sections) {
                if (CamsConstants.AssetGlobal.SECTION_ID_ASSET_INFORMATION.equals(section.getSectionId())) {
                    section.setHidden(true);
                }
            }
        }

        return sections;
    }  
    
    /**
     * Hook for quantity and setting asset numbers.
     * 
     * @see org.kuali.core.maintenance.KualiMaintainableImpl#addNewLineToCollection(java.lang.String)
     */
    @Override
    public void addNewLineToCollection(String collectionName) {
        LOG.info("addNewLineToCollection CALLED....");
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        
        if (CamsPropertyConstants.AssetGlobal.ASSET_PAYMENT_DETAILS.equalsIgnoreCase(collectionName)) {
            LOG.info("addNewLineToCollection assetPaymentDetails START");
            handAssetPaymentsCollection(collectionName, assetGlobal);
            LOG.info("addNewLineToCollection assetPaymentDetails END");
        }
        
        // this is where the location magic is happening
        if (CamsPropertyConstants.AssetGlobal.ASSET_SHARED_DETAILS.equalsIgnoreCase(collectionName)) {
            LOG.info("addNewLineToCollection assetSharedDetails START");
            handleAssetSharedDetailsCollection(collectionName);
            LOG.info("addNewLineToCollection assetSharedDetails END");
        }

        int sharedDetailsIndex = assetGlobal.getAssetSharedDetails().size() - 1;
        if (sharedDetailsIndex > -1 && (CamsPropertyConstants.AssetGlobal.ASSET_SHARED_DETAILS + "[" + sharedDetailsIndex + "]." + CamsPropertyConstants.AssetGlobalDetail.ASSET_UNIQUE_DETAILS).equalsIgnoreCase(collectionName)) {
            handleAssetUniqueCollection(collectionName);
        }
        LOG.info("addNewLineToCollection RETURN....");
        super.addNewLineToCollection(collectionName);
    }

    private void handleAssetUniqueCollection(String collectionName) {
        AssetGlobalDetail assetGlobalDetail = (AssetGlobalDetail) newCollectionLines.get(collectionName);
        if (assetGlobalDetail != null) {
            assetGlobalDetail.setCapitalAssetNumber(NextAssetNumberFinder.getLongValue());
        }
    }

    private void handleAssetSharedDetailsCollection(String collectionName) {
        LOG.info("handleAssetSharedDetailsCollection CALLED....");
        AssetGlobalDetail assetGlobalDetail = (AssetGlobalDetail) newCollectionLines.get(collectionName);
        Integer locationQuantity = assetGlobalDetail.getLocationQuantity();
        LOG.info("handleAssetSharedDetailsCollection locationQuantity: " + locationQuantity + "'");
        while (locationQuantity != null && locationQuantity > 0) {
            AssetGlobalDetail newAssetUnique = new AssetGlobalDetail();
            LOG.info("handleAssetSharedDetailsCollection newAssetUnique : " + NextAssetNumberFinder.getLongValue() + "'");
            newAssetUnique.setCapitalAssetNumber(NextAssetNumberFinder.getLongValue());
            assetGlobalDetail.getAssetGlobalUniqueDetails().add(newAssetUnique);
            newAssetUnique.setNewCollectionRecord(true);
            locationQuantity--;
        }
        LOG.info("handleAssetSharedDetailsCollection RETURN....");
    }

    private void handAssetPaymentsCollection(String collectionName, AssetGlobal assetGlobal) {
        AssetPaymentDetail assetPaymentDetail = (AssetPaymentDetail) newCollectionLines.get(collectionName);
        if (assetPaymentDetail != null) {
            assetPaymentDetail.setSequenceNumber(assetGlobal.incrementFinancialDocumentLineNumber());
            // Set for document number and  document type code
            if (assetGlobalService.existsInGroup(CamsConstants.AssetGlobal.NON_NEW_ACQUISITION_CODE_GROUP, assetGlobal.getAcquisitionTypeCode())) {
                assetPaymentDetail.setExpenditureFinancialDocumentNumber(documentNumber);
                assetPaymentDetail.setExpenditureFinancialDocumentTypeCode(CamsConstants.AssetGlobal.ADD_ASSET_DOCUMENT_TYPE_CODE);
            }
        }
    }

    /**
     * This creates the particular locking representation for this global document.
     * 
     * @see org.kuali.core.maintenance.Maintainable#generateMaintenanceLocks()
     */
    @Override
    public List<MaintenanceLock> generateMaintenanceLocks() {
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        List<MaintenanceLock> maintenanceLocks = new ArrayList<MaintenanceLock>();

        for (AssetGlobalDetail detail : assetGlobal.getAssetGlobalDetails()) {
            MaintenanceLock maintenanceLock = new MaintenanceLock();
            StringBuffer lockRep = new StringBuffer();

            lockRep.append(Asset.class.getName() + KFSConstants.Maintenance.AFTER_CLASS_DELIM);
            lockRep.append(CamsPropertyConstants.Asset.CAPITAL_ASSET_NUMBER + KFSConstants.Maintenance.AFTER_FIELDNAME_DELIM);
            lockRep.append(detail.getCapitalAssetNumber());

            maintenanceLock.setDocumentNumber(assetGlobal.getDocumentNumber());
            maintenanceLock.setLockingRepresentation(lockRep.toString());
            maintenanceLocks.add(maintenanceLock);
        }
        return maintenanceLocks;
    }


    @Override
    public void prepareForSave() {
        super.prepareForSave();
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        List<AssetGlobalDetail> assetSharedDetails = assetGlobal.getAssetSharedDetails();
        List<AssetGlobalDetail> newDetails = new TypedArrayList(AssetGlobalDetail.class);
        AssetGlobalDetail newAssetGlobalDetail = null;
        // clear existing entries
        deleteExistingAssetGlobalDetailRecords(assetGlobal);
        if (!assetSharedDetails.isEmpty() && !assetSharedDetails.get(0).getAssetGlobalUniqueDetails().isEmpty()) {

            for (AssetGlobalDetail locationDetail : assetSharedDetails) {
                List<AssetGlobalDetail> assetGlobalUniqueDetails = locationDetail.getAssetGlobalUniqueDetails();

                for (AssetGlobalDetail detail : assetGlobalUniqueDetails) {
                    // read from location and set it to detail
                    detail.setCampusCode(locationDetail.getCampusCode());
                    detail.setBuildingCode(locationDetail.getBuildingCode());
                    detail.setBuildingRoomNumber(locationDetail.getBuildingRoomNumber());
                    detail.setBuildingSubRoomNumber(locationDetail.getBuildingSubRoomNumber());
                    detail.setOffCampusName(locationDetail.getOffCampusName());
                    detail.setOffCampusAddress(locationDetail.getOffCampusAddress());
                    detail.setOffCampusCityName(locationDetail.getOffCampusCityName());
                    detail.setOffCampusStateCode(locationDetail.getOffCampusStateCode());
                    detail.setOffCampusCountryCode(locationDetail.getOffCampusCountryCode());
                    detail.setOffCampusZipCode(locationDetail.getOffCampusZipCode());
                    newDetails.add(detail);
                }
            }
        }
        computeDepreciationDate(assetGlobal);
        assetGlobal.getAssetGlobalDetails().clear();
        assetGlobal.setPrimaryDepreciationMethodCode(CamsConstants.DEPRECIATION_METHOD_STRAIGHT_LINE_CODE);
        assetGlobal.setAssetGlobalDetails(newDetails);
        assetGlobal.setPrimaryDepreciationBaseAmount(assetGlobalService.totalNonFederalPaymentByAsset(assetGlobal));
    }

        

    private void computeDepreciationDate(AssetGlobal assetGlobal) {
        Date inServiceDate = SpringContext.getBean(DateTimeService.class).getCurrentSqlDate();
        assetGlobal.setCapitalAssetInServiceDate(inServiceDate);

        List<AssetPaymentDetail> assetPaymentDetails = assetGlobal.getAssetPaymentDetails();
        if (assetPaymentDetails != null && !assetPaymentDetails.isEmpty()) {
            LOG.debug("Compute depreciation date based on asset type, depreciation convention and in-service date");
            AssetPaymentDetail firstAssetPaymentDetail = assetPaymentDetails.get(0);
            ObjectCode objectCode = SpringContext.getBean(ObjectCodeService.class).getByPrimaryId(firstAssetPaymentDetail.getFinancialDocumentPostingYear(), firstAssetPaymentDetail.getChartOfAccountsCode(), firstAssetPaymentDetail.getFinancialObjectCode());
            if (ObjectUtils.isNotNull(objectCode)) {
                assetGlobal.refreshReferenceObject(CamsPropertyConstants.AssetGlobal.CAPITAL_ASSET_TYPE);
                Map<String, String> primaryKeys = new HashMap<String, String>();
                primaryKeys.put(CamsPropertyConstants.AssetDepreciationConvention.FINANCIAL_OBJECT_SUB_TYPE_CODE, objectCode.getFinancialObjectSubTypeCode());
                AssetDepreciationConvention depreciationConvention = (AssetDepreciationConvention) SpringContext.getBean(BusinessObjectService.class).findByPrimaryKey(AssetDepreciationConvention.class, primaryKeys);
                Date depreciationDate = SpringContext.getBean(AssetDateService.class).computeDepreciationDate(assetGlobal.getCapitalAssetType(), depreciationConvention, inServiceDate);
                assetGlobal.setCapitalAssetDepreciationDate(depreciationDate);
            }
        }
    }

    private void deleteExistingAssetGlobalDetailRecords(AssetGlobal assetGlobal) {
        LOG.debug("Clearing Global Details, before saving new state");
        BusinessObjectService boService = SpringContext.getBean(BusinessObjectService.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put(CamsPropertyConstants.AssetGlobalDetail.DOCUMENT_NUMBER, assetGlobal.getDocumentNumber());
        boService.deleteMatching(AssetGlobalDetail.class, params);
    }


    @Override
    public void processAfterRetrieve() {
        super.processAfterRetrieve();
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        assetGlobal.refresh();
        List<AssetGlobalDetail> assetGlobalDetails = assetGlobal.getAssetGlobalDetails();
        AssetGlobalDetail currLocationDetail = null;
        HashMap<String, AssetGlobalDetail> locationMap = new HashMap<String, AssetGlobalDetail>();
        AssetGlobalDetail copyValue = null;
        for (AssetGlobalDetail detail : assetGlobalDetails) {
            copyValue = (AssetGlobalDetail) ObjectUtils.deepCopy(detail);
            String key = generateLocationKey(copyValue);
            if ((currLocationDetail = locationMap.get(key)) == null) {
                currLocationDetail = copyValue;
                locationMap.put(key, currLocationDetail);
            }
            currLocationDetail.getAssetGlobalUniqueDetails().add(copyValue);
            currLocationDetail.setLocationQuantity(currLocationDetail.getAssetGlobalUniqueDetails().size());
        }
        assetGlobal.getAssetSharedDetails().clear();
        assetGlobal.getAssetSharedDetails().addAll(locationMap.values());
    }

    private String generateLocationKey(AssetGlobalDetail location) {
        StringBuilder builder = new StringBuilder();
        builder.append(location.getCampusCode() == null ? "" : location.getCampusCode().trim().toLowerCase());
        builder.append(location.getBuildingCode() == null ? "" : location.getBuildingCode().trim().toLowerCase());
        builder.append(location.getBuildingRoomNumber() == null ? "" : location.getBuildingRoomNumber().trim().toLowerCase());
        builder.append(location.getBuildingSubRoomNumber() == null ? "" : location.getBuildingSubRoomNumber().trim().toLowerCase());
        builder.append(location.getOffCampusName() == null ? "" : location.getOffCampusName().trim().toLowerCase());
        builder.append(location.getOffCampusAddress() == null ? "" : location.getOffCampusAddress().trim().toLowerCase());
        builder.append(location.getOffCampusCityName() == null ? "" : location.getOffCampusCityName().trim().toLowerCase());
        builder.append(location.getOffCampusStateCode() == null ? "" : location.getOffCampusStateCode().trim().toLowerCase());
        builder.append(location.getOffCampusZipCode() == null ? "" : location.getOffCampusZipCode().trim().toLowerCase());
        builder.append(location.getOffCampusCountryCode() == null ? "" : location.getOffCampusCountryCode().trim().toLowerCase());
        return builder.toString();
    }

    @Override
    public void processAfterPost(MaintenanceDocument document, Map<String, String[]> parameters) {
        super.processAfterPost(document, parameters);
        // adjust the quantity
        AssetGlobal assetGlobal = (AssetGlobal) getBusinessObject();
        List<AssetGlobalDetail> assetSharedDetails = assetGlobal.getAssetSharedDetails();
        if (!assetSharedDetails.isEmpty()) {
            for (AssetGlobalDetail assetGlobalDetail : assetSharedDetails) {
                assetGlobalDetail.setLocationQuantity(assetGlobalDetail.getAssetGlobalUniqueDetails().size());
            }

        }
    }
}

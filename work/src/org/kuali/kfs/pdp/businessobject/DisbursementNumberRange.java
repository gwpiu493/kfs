/*
 * Copyright 2007 The Kuali Foundation
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
/*
 * Created on Jul 12, 2004
 *
 */
package org.kuali.kfs.pdp.businessobject;

import java.sql.Date;
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.pdp.PdpPropertyConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.Bank;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.core.api.mo.common.active.Inactivatable;
import org.kuali.rice.core.api.util.type.KualiInteger;
import org.kuali.rice.krad.bo.PersistableBusinessObjectBase;
import org.kuali.rice.location.api.campus.Campus;
import org.kuali.rice.location.api.campus.CampusService;

public class DisbursementNumberRange extends PersistableBusinessObjectBase implements Inactivatable {

    private String physCampusProcCode;
    private KualiInteger beginDisbursementNbr;
    private KualiInteger lastAssignedDisbNbr;
    private KualiInteger endDisbursementNbr;
    private Date disbNbrRangeStartDt;
    private String bankCode;
    private String disbursementTypeCode;
    private boolean active;

    private Campus campus;
    private Bank bank;
    private DisbursementType disbursementType;

    public DisbursementNumberRange() {
        super();
    }

    /**
     * @return
     */
    public Bank getBank() {
        return bank;
    }

    /**
     * Gets the bankCode attribute.
     * 
     * @return Returns the bankCode.
     */
    public String getBankCode() {
        return bankCode;
    }

    /**
     * Sets the bankCode attribute value.
     * 
     * @param bankCode The bankCode to set.
     */
    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    /**
     * @return
     * @hibernate.property column="BEG_DISB_NBR"
     */
    public KualiInteger getBeginDisbursementNbr() {
        return beginDisbursementNbr;
    }

    /**
     * @return
     * @hibernate.property column="END_DISB_NBR"
     */
    public KualiInteger getEndDisbursementNbr() {
        return endDisbursementNbr;
    }

    /**
     * @return
     * @hibernate.property column="LST_ASND_DISB_NBR"
     */
    public KualiInteger getLastAssignedDisbNbr() {
        return lastAssignedDisbNbr;
    }

    /**
     * @return
     * @hibernate.property column="PHYS_CMP_PROC_CD" length="2"
     */
    public String getPhysCampusProcCode() {
        return physCampusProcCode;
    }

    /**
     * @param Bank
     */
    @Deprecated
    public void setBank(Bank bank) {
        this.bank = bank;
    }

    /**
     * @param integer
     */
    public void setBeginDisbursementNbr(KualiInteger integer) {
        beginDisbursementNbr = integer;
    }

    /**
     * @param integer
     */
    public void setEndDisbursementNbr(KualiInteger integer) {
        endDisbursementNbr = integer;
    }

    /**
     * @param integer
     */
    public void setLastAssignedDisbNbr(KualiInteger integer) {
        lastAssignedDisbNbr = integer;
    }

    /**
     * @param string
     */
    public void setPhysCampusProcCode(String string) {
        physCampusProcCode = string;
    }

    /**
     * Gets the disbursementTypeCode attribute.
     * 
     * @return Returns the disbursementTypeCode.
     */
    public String getDisbursementTypeCode() {
        return disbursementTypeCode;
    }

    /**
     * Sets the disbursementTypeCode attribute value.
     * 
     * @param disbursementTypeCode The disbursementTypeCode to set.
     */
    public void setDisbursementTypeCode(String disbursementTypeCode) {
        this.disbursementTypeCode = disbursementTypeCode;
    }

    /**
     * Gets the disbursementType attribute.
     * 
     * @return Returns the disbursementType.
     */
    public DisbursementType getDisbursementType() {
        return disbursementType;
    }

    /**
     * Sets the disbursementType attribute value.
     * 
     * @param disbursementType The disbursementType to set.
     */
    public void setDisbursementType(DisbursementType disbursementType) {
        this.disbursementType = disbursementType;
    }

    /**
     * Gets the campus attribute.
     * 
     * @return Returns the campus.
     */
    public Campus getCampus() {
        return campus = StringUtils.isBlank( campusCode)?null:((campus!=null && campus.getCode().equals( campusCode))?campus:SpringContext.getBean(CampusService.class).getCampus( campusCode));
    }

    /**
     * Sets the campus attribute value.
     * 
     * @param campus The campus to set.
     */
    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    /**
     * @see org.kuali.rice.core.api.mo.common.active.Inactivatable#isActive()
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @see org.kuali.rice.core.api.mo.common.active.Inactivatable#setActive(boolean)
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets the disbNbrRangeStartDt attribute.
     * 
     * @return Returns the disbNbrRangeStartDt.
     */
    public Date getDisbNbrRangeStartDt() {
        return disbNbrRangeStartDt;
    }

    /**
     * Sets the disbNbrRangeStartDt attribute value.
     * 
     * @param disbNbrRangeStartDt The disbNbrRangeStartDt to set.
     */
    public void setDisbNbrRangeStartDt(Date disbNbrRangeStartDt) {
        this.disbNbrRangeStartDt = disbNbrRangeStartDt;
    }

    /**
     * @see org.kuali.rice.krad.bo.BusinessObjectBase#toStringMapper()
     */
    
    protected LinkedHashMap toStringMapper_RICE20_REFACTORME() {
        LinkedHashMap m = new LinkedHashMap();
        m.put(PdpPropertyConstants.PHYS_CAMPUS_PROC_CODE, this.physCampusProcCode);
        m.put(PdpPropertyConstants.DISBURSEMENT_TYPE_CODE, this.disbursementTypeCode);
        m.put(KFSPropertyConstants.BANK_CODE, this.bankCode);
        m.put(PdpPropertyConstants.DISBURSEMENT_NUMBER_RANGE_START_DATE, this.disbNbrRangeStartDt);

        return m;
    }
}

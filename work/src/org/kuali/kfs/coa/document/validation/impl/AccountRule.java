/*
 * Copyright (c) 2004, 2005 The National Association of College and University Business Officers,
 * Cornell University, Trustees of Indiana University, Michigan State University Board of Trustees,
 * Trustees of San Joaquin Delta College, University of Hawai'i, The Arizona Board of Regents on
 * behalf of the University of Arizona, and the r*smart group.
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License"); By obtaining,
 * using and/or copying this Original Work, you agree that you have read, understand, and will
 * comply with the terms and conditions of the Educational Community License.
 * 
 * You may obtain a copy of the License at:
 * 
 * http://kualiproject.org/license.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.kuali.module.chart.rules;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.kuali.Constants;
import org.kuali.KeyConstants;
import org.kuali.core.bo.Building;
import org.kuali.core.bo.user.KualiUser;
import org.kuali.core.bo.user.UniversalUser;
import org.kuali.core.document.MaintenanceDocument;
import org.kuali.core.maintenance.rules.MaintenanceDocumentRuleBase;
import org.kuali.core.service.DictionaryValidationService;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.ObjectUtils;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.module.chart.bo.Account;
import org.kuali.module.chart.bo.SubFundGroup;
import org.kuali.module.gl.service.BalanceService;
import org.kuali.module.gl.service.GeneralLedgerPendingEntryService;

/**
 * Business rule(s) applicable to AccountMaintenance documents.
 * 
 * @author Kuali Nervous System Team (kualidev@oncourse.iu.edu)
 */
public class AccountRule extends MaintenanceDocumentRuleBase {
    
    protected static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AccountRule.class);
    
    private static final String ACCT_PREFIX_RESTRICTION = "Account.PrefixRestriction";
    private static final String ACCT_CAPITAL_SUBFUNDGROUP = "Account.CapitalSubFundGroup";

    private static final String CONTRACTS_GRANTS_CD = "CG";
    private static final String GENERAL_FUND_CD = "GF";
    private static final String RESTRICTED_FUND_CD = "RF";
    private static final String ENDOWMENT_FUND_CD = "EN";
    private static final String PLANT_FUND_CD = "PF";
    
    private static final String RESTRICTED_CD_RESTRICTED = "R";
    private static final String RESTRICTED_CD_UNRESTRICTED = "U";
    private static final String RESTRICTED_CD_TEMPORARILY_RESTRICTED = "T";
    
    
    private static final String SUB_FUND_GROUP_MEDICAL_PRACTICE_FUNDS = "MPRACT";
    
    private static final String BUDGET_RECORDING_LEVEL_MIXED = "M";
    
    private GeneralLedgerPendingEntryService generalLedgerPendingEntryService;
    private BalanceService balanceService;
    
    private Account oldAccount;
    private Account newAccount;
    
    public AccountRule() {
        
        // Pseudo-inject some services.
        //
        // This approach is being used to make it simpler to convert the Rule classes 
        // to spring-managed with these services injected by Spring at some later date.  
        // When this happens, just remove these calls to the setters with 
        // SpringServiceLocator, and configure the bean defs for spring.
        this.setGeneralLedgerPendingEntryService(SpringServiceLocator.getGeneralLedgerPendingEntryService());
        this.setBalanceService(SpringServiceLocator.getGeneralLedgerBalanceService());
    }
    
    /**
     * 
     * This method sets the convenience objects like newAccount and oldAccount, so you
     * have short and easy handles to the new and old objects contained in the 
     * maintenance document.
     * 
     * It also calls the BusinessObjectBase.refresh(), which will attempt to load 
     * all sub-objects from the DB by their primary keys, if available.
     * 
     * @param document - the maintenanceDocument being evaluated
     * 
     */
    protected void setupConvenienceObjects(MaintenanceDocument document) {
        
        //	setup oldAccount convenience objects, make sure all possible sub-objects are populated
        oldAccount = (Account) super.getOldBo();

        //	setup newAccount convenience objects, make sure all possible sub-objects are populated
        newAccount = (Account) super.getNewBo();
    }
    
    /**
     * 
     * @see org.kuali.core.maintenance.rules.MaintenanceDocumentRuleBase#processCustomSaveDocumentBusinessRules(org.kuali.core.document.MaintenanceDocument)
     */
    protected boolean processCustomSaveDocumentBusinessRules(MaintenanceDocument document) {
        
        LOG.info("processCustomSaveDocumentBusinessRules called");
        setupConvenienceObjects(document);
        
        checkEmptyValues(document);
        checkGeneralRules(document);
        checkCloseAccount(document);
        checkContractsAndGrants(document);
        checkExpirationDate(document);
        checkFundGroup(document);
        checkSubFundGroup(document);
        
        //	Save always succeeds, even if there are business rule failures
        return true;
    }
    
    protected boolean processCustomRouteDocumentBusinessRules(MaintenanceDocument document) {

        LOG.info("processCustomRouteDocumentBusinessRules called");
        setupConvenienceObjects(document);
        
        //	default to success
        boolean success = true;
        
        success &= checkEmptyValues(document);
        success &= checkGeneralRules(document);
        success &= checkCloseAccount(document);
        success &= checkContractsAndGrants(document);
        success &= checkExpirationDate(document);
        success &= checkFundGroup(document);
        success &= checkSubFundGroup(document);
        
        return success;
    }

    /**
     * 
     * This method checks the basic rules for empty values in an account and associated
     * objects with this account
     * @param maintenanceDocument
     * @return
     */
    protected boolean checkEmptyValues(MaintenanceDocument maintenanceDocument) {
        
        LOG.info("checkEmptyValues called");

        boolean success = true;
        
        //	guidelines are always required, except when the expirationDate is set, and its 
        // earlier than today
        boolean guidelinesRequired = areGuidelinesRequired((Account) maintenanceDocument.getNewMaintainableObject().getBusinessObject());

        //  confirm that required guidelines are entered, if required
        if (guidelinesRequired) {
            success &= checkEmptyBOField("accountGuideline.accountExpenseGuidelineText", newAccount.getAccountGuideline().getAccountExpenseGuidelineText(), "Expense Guideline");
            success &= checkEmptyBOField("accountGuideline.accountIncomeGuidelineText", newAccount.getAccountGuideline().getAccountIncomeGuidelineText(), "Income Guideline");
            success &= checkEmptyBOField("accountGuideline.accountPurposeText", newAccount.getAccountGuideline().getAccountPurposeText(), "Account Purpose");
        }

        //  this set confirms that all fields which are grouped (ie, foreign keys of a referenc 
        // object), must either be none filled out, or all filled out.
        success &= checkForPartiallyFilledOutReferenceForeignKeys("continuationAccount");
        success &= checkForPartiallyFilledOutReferenceForeignKeys("incomeStreamAccount");
        success &= checkForPartiallyFilledOutReferenceForeignKeys("endowmentIncomeAccount");
        success &= checkForPartiallyFilledOutReferenceForeignKeys("reportsToAccount");
        success &= checkForPartiallyFilledOutReferenceForeignKeys("contractControlAccount");
        success &= checkForPartiallyFilledOutReferenceForeignKeys("indirectCostRecoveryAcct");
        
        return success;
    }
    
    /**
     * 
     * This method determines whether the guidelines are required, based on 
     * business rules.
     * 
     * @param account - the populated Account bo to be evaluated
     * @return - true if guidelines are required, false otherwise
     * 
     */
    protected boolean areGuidelinesRequired(Account account) {
        
        boolean result = true;
        
        if (account.getAccountExpirationDate() != null) {
            Timestamp today = getDateTimeService().getCurrentTimestamp();
            today.setTime(DateUtils.truncate(today, Calendar.DAY_OF_MONTH).getTime());
            if (account.getAccountExpirationDate().before(today)) {
                result = false;
            }
        }
        return result;
    }
    
    /**
     * 
     * This method tests whether the accountNumber passed in is prefixed with an 
     * allowed prefix, or an illegal one. 
     * 
     * The illegal prefixes are passed in as an array of strings.
     * 
     * @param accountNumber - The Account Number to be tested.
     * @param illegalValues - An Array of Strings of the unallowable prefixes.
     * @return - false if the accountNumber starts with any of the illegalPrefixes, true otherwise
     * 
     */
    protected boolean accountNumberStartsWithAllowedPrefix(String accountNumber, String[] illegalValues) {
        
        boolean result = true;
        
        //  for each disallowed value, make sure the account doesnt start with it
        for (int i = 0; i < illegalValues.length; i++) {
            if (accountNumber.startsWith(illegalValues[i])) {
                result = false;
                putFieldError("accountNumber", 
                        KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_NMBR_NOT_ALLOWED, 
                        new String[] {accountNumber, illegalValues[i]});
            }
        }
        
        return result;
    }
    
    /**
     * 
     * This method tests whether an account is being ReOpened by anyone except a system 
     * supervisor.  Only system supervisors may reopen closed accounts.
     * 
     * @param document - populated document containing the old and new accounts
     * @param user - the user who is trying to possibly reopen the account 
     * @return - true if:  document is an edit document, old was closed and new is open, and the 
     *           user is not one of the System Supervisors
     *           
     */
    protected boolean isNonSystemSupervisorReopeningAClosedAccount(MaintenanceDocument document, KualiUser user) {
        
        boolean result = false;
        
        if (document.isEdit()) {
            
            //  get local references
            Account oldAccount = (Account) document.getOldMaintainableObject().getBusinessObject();
            Account newAccount = (Account) document.getNewMaintainableObject().getBusinessObject();
            
            //  do the test
            if (oldAccount.isAccountClosedIndicator()) {
                if (!newAccount.isAccountClosedIndicator()) {
                    if (!user.isSupervisorUser()) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * 
     * This method tests whether a given account has the T - Temporary value for 
     * Restricted Status Code, but does not have a Restricted Status Date, which is 
     * required when the code is T.
     * 
     * @param account
     * @return
     */
    protected boolean hasTemporaryRestrictedStatusCodeButNoRestrictedStatusDate(Account account) {
        
        boolean result = false;
        
        if (StringUtils.isNotBlank(account.getAccountRestrictedStatusCode())) {
            if (RESTRICTED_CD_TEMPORARILY_RESTRICTED.equalsIgnoreCase(account.getAccountRestrictedStatusCode().trim())) {
                if (account.getAccountRestrictedStatusDate() == null) {
                    result = true;
                }
            }
        }
        return result;
    }
    
    /**
     * 
     * This method checks some of the general business rules associated with this document
     * @param maintenanceDocument
     * @return false on rules violation
     */
    protected boolean checkGeneralRules(MaintenanceDocument maintenanceDocument) {

        LOG.info("checkGeneralRules called");
        UniversalUser fiscalOfficer = newAccount.getAccountFiscalOfficerUser();
        UniversalUser accountManager = newAccount.getAccountManagerUser();
        UniversalUser accountSupervisor = newAccount.getAccountSupervisoryUser();
        
        boolean success = true;

        // Enforce institutionally specified restrictions on account number prefixes
        // (e.g. the account number cannot begin with a 3 or with 00.)
        // Only bother trying if there is an account string to test
        if (!StringUtils.isBlank(newAccount.getAccountNumber())) {
            String[] illegalValues = getConfigService().getApplicationParameterValues(
                Constants.ChartApcParms.GROUP_CHART_MAINT_EDOCS, ACCT_PREFIX_RESTRICTION);
            //  test the number
            success &= accountNumberStartsWithAllowedPrefix(newAccount.getAccountNumber(), illegalValues);
        }
        
        //only a FIS supervisor can reopen a closed account. (This is the central super user, not an account supervisor).
        //we need to get the old maintanable doc here
        if (isNonSystemSupervisorReopeningAClosedAccount(maintenanceDocument, GlobalVariables.getUserSession().getKualiUser())) {
            success &= false;
            putFieldError("accountClosedIndicator", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ONLY_SUPERVISORS_CAN_REOPEN);
        }
        
        //  when a restricted status code of 'T' (temporarily restricted) is selected, a restricted status 
        // date must be supplied.
        if (hasTemporaryRestrictedStatusCodeButNoRestrictedStatusDate(newAccount)) {
            success &= false;
            putFieldError("accountRestrictedStatusDate", KeyConstants.ERROR_DOCUMENT_ACCMAINT_RESTRICTED_STATUS_DT_REQ, newAccount.getAccountNumber());
        }
        
        //  check FringeBenefit account rules
        success &= checkFringeBenefitAccountRule(newAccount);
        
        //the employee type for fiscal officer, account manager, and account supervisor must be 'P' � professional.
        success &= checkUserStatusAndType("accountFiscalOfficerUser.personUserIdentifier", fiscalOfficer);
        success &= checkUserStatusAndType("accountSupervisoryUser.personUserIdentifier", accountSupervisor);
        success &= checkUserStatusAndType("accountManagerUser.personUserIdentifier", accountManager);
        
        //the supervisor cannot be the same as the fiscal officer or account manager.
        if (isSupervisorSameAsFiscalOfficer(newAccount)) {
            success &= false;
            putFieldError("accountsSupervisorySystemsIdentifier", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_SUPER_CANNOT_BE_FISCAL_OFFICER);
        }
        if (isSupervisorSameAsManager(newAccount)) {
            success &= false;
            putFieldError("accountManagerSystemIdentifier", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_SUPER_CANNOT_BE_ACCT_MGR);
        }
        
        //  disallow continuation account being expired
        if (isContinuationAccountExpired(newAccount)) {
            success &= false;
            putFieldError("continuationAccountNumber", 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCOUNT_EXPIRED_CONTINUATION);
        }
        
        return success;
    }

    /**
     * 
     * This method tests whether the continuation account entered (if any) 
     * has expired or not.
     * 
     * @param newAccount
     * @return
     * 
     */
    protected boolean isContinuationAccountExpired(Account newAccount) {
        
        boolean result = false;
        
        String chartCode = newAccount.getContinuationFinChrtOfAcctCd();
        String accountNumber = newAccount.getContinuationAccountNumber();
        
        //  if either chartCode or accountNumber is not entered, then we 
        // cant continue, so exit
        if (StringUtils.isBlank(chartCode) || StringUtils.isBlank(accountNumber)) {
            return result;
        }
        
        //  attempt to retrieve the continuation account from the DB
        Account continuation = null;
        Map pkMap = new HashMap();
        pkMap.put("chartOfAccountsCode", chartCode);
        pkMap.put("accountNumber", accountNumber);
        continuation = (Account) super.getBoService().findByPrimaryKey(Account.class, pkMap);
        
        //  if the object doesnt exist, then we cant continue, so exit
        if (ObjectUtils.isNull(continuation)) {
            return result;
        }
        
        //  at this point, we have a valid continuation account, so we just need to 
        // know whether its expired or not
        result = continuation.isExpired();
        
        return result;
    }
    
    // the fringe benefit account (otherwise known as the reportsToAccount) is required if 
    // the fringe benefit code is set to N. 
    // The fringe benefit code of the account designated to accept the fringes must be Y.
    protected boolean checkFringeBenefitAccountRule(Account newAccount) {
        
        boolean result = true;
        
        //  if this account is selected as a Fringe Benefit Account, then we have nothing
        // to test, so exit
        if (newAccount.isAccountsFringesBnftIndicator()) {
            return true;
        }
        
        //  if fringe benefit is not selected ... continue processing
        
        //  fringe benefit account number is required
        if (StringUtils.isBlank(newAccount.getReportsToAccountNumber())) {
            putFieldError("reportsToAccountNumber", KeyConstants.ERROR_DOCUMENT_ACCMAINT_RPTS_TO_ACCT_REQUIRED_IF_FRINGEBENEFIT_FALSE);
            return false;
        }
        
        //  fringe benefit chart of accounts code is required
        if (StringUtils.isBlank(newAccount.getReportsToChartOfAccountsCode())) {
            putFieldError("reportsToChartOfAccountsCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_RPTS_TO_ACCT_REQUIRED_IF_FRINGEBENEFIT_FALSE);
            return false;
        }
        
        //  if the reportsToAccount doesnt exist by the accountNumber and chartOfAccountsCode
        DictionaryValidationService dvService = super.getDictionaryValidationService();
        boolean referenceExists = dvService.validateReferenceExists(newAccount, "reportsToAccount");
        if (!referenceExists) {
            putFieldError("reportsToAccountNumber", KeyConstants.ERROR_EXISTENCE, 
                            "Fringe Benefit Account: " + newAccount.getReportsToChartOfAccountsCode() + "-" + 
                            newAccount.getReportsToAccountNumber());
            return false;
        }

        //  check active
        boolean active = dvService.validateReferenceIsActive(newAccount, "reportsToAccount", "accountClosedIndicator", true);
        if (!active) {
            putFieldError("reportsToAccountNumber", 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_RPTS_TO_ACCT_MUST_BE_FLAGGED_FRINGEBENEFIT, 
                    newAccount.getReportsToAccountNumber());
            return false;
        }
        return result;
    }
    
    protected boolean isSupervisorSameAsFiscalOfficer(Account account) {
        return areTwoUsersTheSame(account.getAccountSupervisoryUser(), account.getAccountFiscalOfficerUser());
    }
    
    protected boolean isSupervisorSameAsManager(Account account) {
        return areTwoUsersTheSame(account.getAccountSupervisoryUser(), account.getAccountManagerUser());
    }
    
    protected boolean areTwoUsersTheSame(UniversalUser user1, UniversalUser user2) {
        if (ObjectUtils.isNull(user1)) {
            return false;
        }
        if (ObjectUtils.isNull(user2)) {
            return false;
        }
        if (ObjectUtils.equalByKeys(user1, user2)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * 
     * This method checks to see if the user passed in is of the type requested.  
     * 
     * If so, it returns true.  If not, it returns false, and adds an error to 
     * the GlobalErrors.
     * @param user - UniversalUser to be tested
     * @param employeeType - String value expected for Employee Type 
     * @param userRoleDescription - User Role being tested, to be passed into an error message
     * 
     * @return - true if user is of the requested employee type, false if not, true if the 
     *           user object is null
     * 
     */
    protected boolean checkUserStatusAndType(String propertyName, UniversalUser user) {
        
        boolean success = true;
        
        //	if the user isnt populated, exit with success
        // the actual existence check is performed in the general rules so not testing here
        if (ObjectUtils.isNull(user)) {
            return success;
        }
        
        //  user must be of the allowable statuses (A - Active)
        if (apcRuleFails(Constants.ChartApcParms.GROUP_CHART_MAINT_EDOCS, 
                Constants.ChartApcParms.ACCOUNT_USER_EMP_STATUSES, 
                user.getEmployeeStatusCode())) {
            success &= false;
            putFieldError(propertyName, 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACTIVE_REQD_FOR_EMPLOYEE, 
                    getDdService().getAttributeLabel(Account.class, propertyName));
        }
        
        //  user must be of the allowable types (P - Professional)
        if (apcRuleFails(Constants.ChartApcParms.GROUP_CHART_MAINT_EDOCS, 
                Constants.ChartApcParms.ACCOUNT_USER_EMP_TYPES, 
                user.getEmployeeTypeCode())) {
            success &= false;
            putFieldError(propertyName, 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_PRO_TYPE_REQD_FOR_EMPLOYEE, 
                    getDdService().getAttributeLabel(Account.class, propertyName));
        }
        
        return success;
    }
    
    /**
     * 
     * This method checks to see if the user is trying to close the account and if so if any 
     * rules are being violated
     * @param maintenanceDocument
     * @return false on rules violation
     */
    protected boolean checkCloseAccount(MaintenanceDocument maintenanceDocument) {

        LOG.info("checkCloseAccount called");

        boolean success = true;
        boolean isBeingClosed = false;

        //	if the account isnt being closed, then dont bother processing the rest of 
        // the method
        if(!oldAccount.isAccountClosedIndicator() && newAccount.isAccountClosedIndicator()) {
            isBeingClosed = true;
        } 

        if (!isBeingClosed) {
            return true;
        }
        
        //	get the two dates, and remove any time-components from the dates
        Timestamp expirationDate = newAccount.getAccountExpirationDate();
        Timestamp todaysDate = getDateTimeService().getCurrentTimestamp();
        //TODO: convert this to using Wes' kuali DateUtils once we're using Date's instead of Timestamp
        todaysDate.setTime(DateUtils.truncate(todaysDate, Calendar.DAY_OF_MONTH).getTime());
        
        if (ObjectUtils.isNotNull(expirationDate)) {

            //when closing an account, the account expiration date must be the current date or earlier
            expirationDate.setTime(DateUtils.truncate(expirationDate, Calendar.DAY_OF_MONTH).getTime());
            if (expirationDate.before(todaysDate) || expirationDate.equals(todaysDate)) {
                putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_CANNOT_BE_CLOSED_EXP_DATE_INVALID);
                success &= false;
            }
        }
        
        // when closing an account, a continuation account is required 
        if (StringUtils.isBlank(newAccount.getContinuationAccountNumber())) {
            putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_CLOSE_CONTINUATION_ACCT_REQD);
            success &= false;
        }
        
        // must have no pending ledger entries
        if (generalLedgerPendingEntryService.hasPendingGeneralLedgerEntry(newAccount)) {
            putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCOUNT_CLOSED_PENDING_LEDGER_ENTRIES);
            success &= false;
        }

        // beginning balance must be loaded in order to close account
        if (!balanceService.beginningBalanceLoaded(newAccount)) {
            putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCOUNT_CLOSED_NO_LOADED_BEGINNING_BALANCE);
            success &= false;
        }
        
        // must have no base budget,  must have no open encumbrances, must have no asset, liability or fund balance balances other than object code 9899 
        //      (9899 is fund balance for us), and the process of closing income and expense into 9899 must take the 9899 balance to zero.
        if (balanceService.hasAssetLiabilityFundBalanceBalances(newAccount)) {
            putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCOUNT_CLOSED_NO_FUND_BALANCES);
            success &= false;
        }

        // TODO:  must have no pending labor ledger entries (depends on labor: KULLAB-1) 

        return success;
    }
    
    protected boolean checkAccountExpirationDateTodayOrEarlier(Account newAccount) {
        
        //  get today's date, with no time component
        Timestamp todaysDate = getDateTimeService().getCurrentTimestamp();
        todaysDate.setTime(DateUtils.truncate(todaysDate, Calendar.DAY_OF_MONTH).getTime());
        //TODO: convert this to using Wes' kuali DateUtils once we're using Date's instead of Timestamp
        
        //  get the expiration date, if any
        Timestamp expirationDate = newAccount.getAccountExpirationDate();
        if (ObjectUtils.isNull(expirationDate)) {
            putFieldError("accountExpirationDate", 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_CANNOT_BE_CLOSED_EXP_DATE_INVALID);
            return false;
        }

        //when closing an account, the account expiration date must be the current date or earlier
        expirationDate.setTime(DateUtils.truncate(expirationDate, Calendar.DAY_OF_MONTH).getTime());
        if (expirationDate.after(todaysDate)) {
            putFieldError("accountExpirationDate", 
                    KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_CANNOT_BE_CLOSED_EXP_DATE_INVALID);
            return false;
        }
        
        return true;
    }
    
    /**
     * 
     * This method checks to see if any Contracts and Grants business rules were violated
     * @param maintenanceDocument
     * @return false on rules violation
     */
    protected boolean checkContractsAndGrants(MaintenanceDocument maintenanceDocument) {
        
        //TODO: Must add validation for C&G field. 
        
        LOG.info("checkContractsAndGrants called");

        boolean success = true;
        
        //Certain C&G fields are required if the Account belongs to the CG Fund Group 
        if (ObjectUtils.isNotNull(newAccount.getSubFundGroup())) {
	        if (newAccount.getSubFundGroup().getFundGroupCode().equalsIgnoreCase(CONTRACTS_GRANTS_CD)) {
	            success &= checkEmptyBOField("contractControlFinCoaCode", newAccount.getContractControlFinCoaCode(), 
	                    "When Fund Group is CG, Contract Control Chart of Accounts Code");
	            success &= checkEmptyBOField("contractControlAccountNumber", newAccount.getContractControlAccountNumber(), 
	                    "When Fund Group is CG, Contract Control Account Number");
	            success &= checkEmptyBOField("acctIndirectCostRcvyTypeCd", newAccount.getAcctIndirectCostRcvyTypeCd(), 
	                    "When Fund Group is CG, ICR Type Code");
		        success &= checkEmptyBOField("financialIcrSeriesIdentifier", newAccount.getFinancialIcrSeriesIdentifier(), 
		                "When Fund Group is CG, ICR Series Identifier");
		        success &= checkEmptyBOField("indirectCostRcvyFinCoaCode", newAccount.getIndirectCostRcvyFinCoaCode(), 
		                "When Fund Group is CG, ICR Cost Recovery Chart of Accounts Code");
		        success &= checkEmptyBOField("indirectCostRecoveryAcctNbr", newAccount.getIndirectCostRecoveryAcctNbr(), 
		                "When Fund Group is CG, ICR Cost Recovery Account");
		        success &= checkEmptyBOField("cgCatlfFedDomestcAssistNbr", newAccount.getCgCatlfFedDomestcAssistNbr(), 
		                "When Fund Group is CG, C&G Domestic Assistance Number");
	        }
        }
        
        //	an income stream account is required for accounts in the C&G (CG) and General Fund (GF) fund groups 
        // (except for the MPRACT sub-fund group in the general fund fund group).
        if (ObjectUtils.isNotNull(newAccount.getSubFundGroup())) {
            String fundGroupCode = newAccount.getSubFundGroup().getFundGroupCode();
            if (fundGroupCode.equalsIgnoreCase(CONTRACTS_GRANTS_CD) || 
               (fundGroupCode.equalsIgnoreCase(GENERAL_FUND_CD) && 
               !newAccount.getSubFundGroupCode().equalsIgnoreCase(SUB_FUND_GROUP_MEDICAL_PRACTICE_FUNDS))) {
                
                success &= checkEmptyBOField("incomeStreamAccountNumber", newAccount.getIncomeStreamAccountNumber(), 
                        "When Fund Group is CG or GF, Income Stream Account Number");
                success &= checkEmptyBOField("incomeStreamFinancialCoaCode", newAccount.getIncomeStreamFinancialCoaCode(), 
                        "When Fund Group is CG or GF, Income Stream Chart Of Accounts Code");
                
                /*if(StringUtils.isBlank(newAccount.getIncomeStreamAccountNumber())) {
                    putFieldError("incomeStreamAccountNumber", KeyConstants.ERROR_DOCUMENT_ACCMAINT_INCOME_STREAM_ACCT_NBR_CANNOT_BE_NULL);
                    success &= false;
                }
                if(StringUtils.isBlank(newAccount.getIncomeStreamFinancialCoaCode())) {
                    putFieldError("incomeStreamFinancialCoaCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_INCOME_STREAM_ACCT_COA_CANNOT_BE_NULL);
                    success &= false;
                }*/
            }
        }
        return success;
    }
    
    /**
     * 
     * This method checks to see if any expiration date field rules were violated
     * @param maintenanceDocument
     * @return false on rules violation
     */
    protected boolean checkExpirationDate(MaintenanceDocument maintenanceDocument) {

        LOG.info("checkExpirationDate called");

        boolean success = true;

        Timestamp oldExpDate = oldAccount.getAccountExpirationDate();
        Timestamp newExpDate = newAccount.getAccountExpirationDate();
        Timestamp today = getDateTimeService().getCurrentTimestamp();
        today.setTime(DateUtils.truncate(today, Calendar.DAY_OF_MONTH).getTime()); // remove any time components
        
        //	When updating an account expiration date, the date must be today or later 
        // (except for C&G accounts).  Only run this test if this maint doc 
        // is an edit doc
        if (maintenanceDocument.isEdit()) {
            
            boolean expDateHasChanged = false;
            
            //	if the old version of the account had no expiration date, and the new 
            // one has a date
            if (ObjectUtils.isNull(oldExpDate) && ObjectUtils.isNotNull(newExpDate)) {
                expDateHasChanged = true;
            }
            
            //	 if there was an old and a new expDate, but they're different
            else if (ObjectUtils.isNotNull(oldExpDate) && ObjectUtils.isNotNull(newExpDate)) {
                if (!oldExpDate.equals(newExpDate)) {
                    expDateHasChanged = true;
                }
            }

            //	if the dates are different
            if (expDateHasChanged) {
                
                //	If we have a subFundGroup value.  Normally, this would never be allowed 
                // to be null, but it could be in this case, which will trigger a different 
                // validation error.  But if it is null, we want to silently not bother to 
                // make the test, as it'll run the test for real once the user gets the 
                // subFundGroupCode entered and correct.
                if (ObjectUtils.isNotNull(newAccount.getSubFundGroup())) {
                    String fundGroupCode = newAccount.getSubFundGroup().getFundGroupCode();
                    
                    //	If this is NOT a CG Fund Group account, then Expiration Date 
                    // must be later than today.  If its not, add a business rule error.
                    if (!fundGroupCode.equalsIgnoreCase(CONTRACTS_GRANTS_CD)) {
                        if (!newExpDate.after(today) && !newExpDate.equals(today)) {
                            putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_EXP_DATE_TODAY_LATER_EXCEPT_CANDG_ACCT);
                            success &= false;
                        }
                    }
                }
            }
        }
        
        //	a continuation account is required if the expiration date is completed.
        if (ObjectUtils.isNotNull(newExpDate)) {
            if (StringUtils.isBlank(newAccount.getContinuationAccountNumber())){
                putFieldError("continuationAccountNumber", KeyConstants.ERROR_DOCUMENT_ACCMAINT_CONTINUATION_ACCT_REQD_IF_EXP_DATE_COMPLETED); 
            }
            if (StringUtils.isBlank(newAccount.getContinuationFinChrtOfAcctCd())){
                putFieldError("continuationFinChrtOfAcctCd", KeyConstants.ERROR_DOCUMENT_ACCMAINT_CONTINUATION_FINCODE_REQD_IF_EXP_DATE_COMPLETED); 
                //putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_CONTINUATION_ACCT_REQD_IF_EXP_DATE_COMPLETED);
                success &= false;
            }
        }
        
        //	If creating a new account if acct_expiration_dt is set and the fund_group is not "CG" then 
        // the acct_expiration_dt must be changed to a date that is today or later
        if(maintenanceDocument.isNew() && ObjectUtils.isNotNull(newExpDate) ) {
            if(ObjectUtils.isNotNull(newAccount.getSubFundGroup())) {
                if(!newAccount.getSubFundGroup().getFundGroupCode().equalsIgnoreCase(CONTRACTS_GRANTS_CD)) {
                    if(!newExpDate.after(today) && !newExpDate.equals(today) ) {
                        putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_EXP_DATE_TODAY_LATER_EXCEPT_CANDG_ACCT);
                        success &= false;
                    }
                }
            }
        }
        
        //	acct_expiration_dt can not be before acct_effect_dt
        Timestamp effectiveDate = newAccount.getAccountEffectiveDate();
        if (ObjectUtils.isNotNull(effectiveDate) && ObjectUtils.isNotNull(newExpDate)) {
            if (newExpDate.before(effectiveDate)) {
                putGlobalError(KeyConstants.ERROR_DOCUMENT_ACCMAINT_EXP_DATE_CANNOT_BE_BEFORE_EFFECTIVE_DATE);
                success &= false;
            }
        }

        return success;
    }
    
    /**
     * 
     * This method checks to see if any Fund Group rules were violated
     * @param maintenanceDocument
     * @return false on rules violation
     * 
     */
    protected boolean checkFundGroup(MaintenanceDocument maintenanceDocument) {
        
        LOG.info("checkFundGroup called");

        boolean success = true;
        SubFundGroup subFundGroup = newAccount.getSubFundGroup();
        
        if (ObjectUtils.isNotNull(subFundGroup)) {
            
            //  get values for fundGroupCode and restrictedStatusCode
            String fundGroupCode = "";
            String restrictedStatusCode = "";
            if(StringUtils.isNotBlank(subFundGroup.getFundGroupCode())) {
                fundGroupCode = subFundGroup.getFundGroupCode().trim();
            }
            if(StringUtils.isNotBlank(newAccount.getAccountRestrictedStatusCode())) {
                restrictedStatusCode = newAccount.getAccountRestrictedStatusCode().trim();
            }

            if (ObjectUtils.isNotNull(restrictedStatusCode)) {
                //	on the account screen, if the fund group of the account is CG (contracts & grants) or 
                // RF (restricted funds), the restricted status code must be 'R'.
                if (fundGroupCode.equalsIgnoreCase(CONTRACTS_GRANTS_CD) || fundGroupCode.equalsIgnoreCase(RESTRICTED_FUND_CD)) {
                    if (!restrictedStatusCode.equalsIgnoreCase(RESTRICTED_CD_RESTRICTED)) {
                        putFieldError("accountRestrictedStatusCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_RESTRICTED_STATUS_CD_MUST_BE_R);
                        success &= false;
                    }
                }

                //	If the fund group is EN (endowment) or PF (plant fund) the value is not set by the system and 
                // must be set by the user 
                else if (fundGroupCode.equalsIgnoreCase(ENDOWMENT_FUND_CD) || fundGroupCode.equalsIgnoreCase(PLANT_FUND_CD)) {
                    if (StringUtils.isBlank(restrictedStatusCode) || 
                       (!restrictedStatusCode.equalsIgnoreCase(RESTRICTED_CD_RESTRICTED) && !restrictedStatusCode.equalsIgnoreCase(RESTRICTED_CD_UNRESTRICTED))) {
                       
                        putFieldError("accountRestrictedStatusCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_RESTRICTED_STATUS_CD_MUST_BE_U_OR_R);
                        success &= false;
                    }
                }
                
                //	for all other fund groups the value is set to 'U'. R being restricted,U being unrestricted.
                else {
                    if (!restrictedStatusCode.equalsIgnoreCase(RESTRICTED_CD_UNRESTRICTED)) {
                        putFieldError("accountRestrictedStatusCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_RESTRICTED_STATUS_CD_MUST_BE_U);
        				success &= false;
                    }
                }
            }
            
            //	an account in the general fund fund group cannot have a budget recording level of mixed.
            if (fundGroupCode.equalsIgnoreCase(GENERAL_FUND_CD)) {
                String budgetRecordingLevelCode = newAccount.getBudgetRecordingLevelCode();
                if (StringUtils.isNotEmpty(budgetRecordingLevelCode)) {
                    if (budgetRecordingLevelCode.equalsIgnoreCase(BUDGET_RECORDING_LEVEL_MIXED)) {
                        putFieldError("budgetRecordingLevelCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_ACCT_GF_BUDGET_RECORD_LVL_MIXED);
                        success &= false;
                    }
                }
            }
        }

        return success;
    }
    
    /**
     * 
     * This method checks to see if any SubFund Group rules were violated
     * @param maintenanceDocument
     * @return false on rules violation
     * 
     */
    protected boolean checkSubFundGroup(MaintenanceDocument maintenanceDocument) {
        
        LOG.info("checkSubFundGroup called");

        boolean success = true;
        
        //  if we dont have a valid subFundGroupCode and subFundGroup object, we cannot proceed
        if (StringUtils.isBlank(newAccount.getSubFundGroupCode()) || ObjectUtils.isNull(newAccount.getSubFundGroup())) {
            return success;
        }
        
        //	PFCMD (Plant Fund, Construction and Major Remodeling) SubFundCode checks

        //	Attempt to get the right SubFundGroup code to check the following logic with.  If the value isn't available, go ahead 
        // and die, as this indicates a misconfigured app, and important business rules wont be implemented without it.
        String capitalSubFundGroup = "";
        capitalSubFundGroup = getConfigService().getApplicationParameterValue(Constants.ChartApcParms.GROUP_CHART_MAINT_EDOCS, ACCT_CAPITAL_SUBFUNDGROUP);

        if (capitalSubFundGroup.equalsIgnoreCase(newAccount.getSubFundGroupCode().trim())) {
            
            String campusCode = newAccount.getAccountDescription().getCampusCode();
            String buildingCode = newAccount.getAccountDescription().getBuildingCode();
            
            //	if sub_fund_grp_cd is 'PFCMR' then campus_cd must be entered
            if (StringUtils.isBlank(campusCode)) {
    	        putFieldError("accountDescription.campusCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_CAMS_SUBFUNDGROUP_WITH_MISSING_CAMPUS_CD_FOR_BLDG);
                success &= false;
            }
        
        	//	if sub_fund_grp_cd is 'PFCMR' then bldg_cd must be entered
        	if (StringUtils.isBlank(buildingCode)) {
    	        putFieldError("accountDescription.campusCode", KeyConstants.ERROR_DOCUMENT_ACCMAINT_CAMS_SUBFUNDGROUP_WITH_MISSING_BUILDING_CD);
                success &= false;
        	} 
        	
        	//	the building object (campusCode & buildingCode) must exist in the DB
        	if (!StringUtils.isBlank(campusCode) && !StringUtils.isBlank(buildingCode)) {
        	    Map pkMap = new HashMap();
        	    pkMap.put("campusCode", campusCode);
        	    pkMap.put("buildingCode", buildingCode);

        	    Building building = (Building) getBoService().findByPrimaryKey(Building.class, pkMap);
        	    if (building == null) {
        	        putFieldError("accountDescription.campusCode", KeyConstants.ERROR_EXISTENCE, campusCode);
        	        putFieldError("accountDescription.buildingCode", KeyConstants.ERROR_EXISTENCE, buildingCode);
        	        success &= false;
        	    }
        	}
        }

        return success;
    }

    public void setGeneralLedgerPendingEntryService(GeneralLedgerPendingEntryService generalLedgerPendingEntryService) {
        this.generalLedgerPendingEntryService = generalLedgerPendingEntryService;
    }

    public void setBalanceService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }
    
}
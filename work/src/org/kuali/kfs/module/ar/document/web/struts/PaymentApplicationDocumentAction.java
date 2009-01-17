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
package org.kuali.kfs.module.ar.document.web.struts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.kuali.kfs.module.ar.ArKeyConstants;
import org.kuali.kfs.module.ar.businessobject.AccountsReceivableDocumentHeader;
import org.kuali.kfs.module.ar.businessobject.Customer;
import org.kuali.kfs.module.ar.businessobject.CustomerInvoiceDetail;
import org.kuali.kfs.module.ar.businessobject.InvoicePaidApplied;
import org.kuali.kfs.module.ar.businessobject.NonAppliedHolding;
import org.kuali.kfs.module.ar.businessobject.NonInvoiced;
import org.kuali.kfs.module.ar.businessobject.NonInvoicedDistribution;
import org.kuali.kfs.module.ar.document.CustomerInvoiceDocument;
import org.kuali.kfs.module.ar.document.PaymentApplicationDocument;
import org.kuali.kfs.module.ar.document.service.AccountsReceivableDocumentHeaderService;
import org.kuali.kfs.module.ar.document.service.CustomerInvoiceDetailService;
import org.kuali.kfs.module.ar.document.service.CustomerInvoiceDocumentService;
import org.kuali.kfs.module.ar.document.service.NonAppliedHoldingService;
import org.kuali.kfs.module.ar.document.service.PaymentApplicationDocumentService;
import org.kuali.kfs.module.ar.document.validation.impl.PaymentApplicationDocumentRuleUtil;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.document.web.struts.FinancialSystemTransactionalDocumentActionBase;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.DocumentService;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.KNSConstants;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase;
import org.kuali.rice.kns.workflow.service.WorkflowDocumentService;

public class PaymentApplicationDocumentAction extends FinancialSystemTransactionalDocumentActionBase {

    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PaymentApplicationDocumentAction.class);

    private BusinessObjectService businessObjectService;
    private DocumentService documentService;
    private WorkflowDocumentService workflowDocumentService;
    private PaymentApplicationDocumentService paymentApplicationDocumentService;
    private CustomerInvoiceDocumentService customerInvoiceDocumentService;
    private CustomerInvoiceDetailService customerInvoiceDetailService;
    private NonAppliedHoldingService nonAppliedHoldingService;

    /**
     * Constructs a PaymentApplicationDocumentAction.java.
     */
    public PaymentApplicationDocumentAction() {
        super();
        businessObjectService = SpringContext.getBean(BusinessObjectService.class);
        documentService = SpringContext.getBean(DocumentService.class);
        workflowDocumentService = SpringContext.getBean(WorkflowDocumentService.class);
        paymentApplicationDocumentService = SpringContext.getBean(PaymentApplicationDocumentService.class);
        customerInvoiceDocumentService = SpringContext.getBean(CustomerInvoiceDocumentService.class);
        customerInvoiceDetailService = SpringContext.getBean(CustomerInvoiceDetailService.class);
        nonAppliedHoldingService = SpringContext.getBean(NonAppliedHoldingService.class);
    }

    /**
     * This is overridden in order to recalculate the invoice totals before doing the submit.
     * 
     * @see org.kuali.rice.kns.web.struts.action.KualiDocumentActionBase#route(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward route(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return super.route(mapping, form, request, response);
    }

    /**
     * Create an InvoicePaidApplied for a CustomerInvoiceDetail and validate it.
     * If the validation succeeds it's added to the PaymentApplicationDocument.
     * If the validation does succeed it's not added to the PaymentApplicationDocument and null is returned.
     * 
     * @param customerInvoiceDetail
     * @param paymentApplicationDocument
     * @param amount
     * @param fieldName
     * @return
     * @throws WorkflowException
     */
    private InvoicePaidApplied applyToCustomerInvoiceDetail(CustomerInvoiceDetail customerInvoiceDetail, PaymentApplicationDocument paymentApplicationDocument, KualiDecimal amount, String fieldName) throws WorkflowException {
        InvoicePaidApplied invoicePaidApplied = 
            paymentApplicationDocumentService.createInvoicePaidAppliedForInvoiceDetail(
                customerInvoiceDetail, paymentApplicationDocument, amount);
        // If the new invoice paid applied is valid, add it to the document
        if (PaymentApplicationDocumentRuleUtil.validateInvoicePaidApplied(invoicePaidApplied, fieldName)) {
            paymentApplicationDocument.getInvoicePaidApplieds().add(invoicePaidApplied);
            return invoicePaidApplied;
        }
        return null;
    }
    
    public ActionForward applyAllAmounts(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm paymentApplicationDocumentForm = (PaymentApplicationDocumentForm) form;
        PaymentApplicationDocument paymentApplicationDocument = paymentApplicationDocumentForm.getPaymentApplicationDocument();
        
        // Remove all invoice paid applieds from the document because we'll be adding them straight from the form.
        if(ObjectUtils.isNotNull(paymentApplicationDocument)) {
            paymentApplicationDocument.getInvoicePaidApplieds().clear();
        }
        
        applyToIndividualCustomerInvoiceDetails(paymentApplicationDocumentForm);
        applyToInvoices(paymentApplicationDocumentForm);
        applyNonInvoiced(paymentApplicationDocumentForm);
        applyUnapplied(paymentApplicationDocumentForm);
        
        // Check that we haven't applied more than the cash control total amount
        KualiDecimal openAmount = paymentApplicationDocument.getCashControlDocument().getCashControlTotalAmount();
        openAmount = openAmount.subtract(paymentApplicationDocument.getSumOfInvoicePaidApplieds());
        openAmount = openAmount.subtract(paymentApplicationDocument.getSumOfNonAppliedDistributions());
        openAmount = openAmount.subtract(paymentApplicationDocument.getSumOfNonInvoicedDistributions());
        openAmount = openAmount.subtract(paymentApplicationDocument.getSumOfNonInvoiceds());
        openAmount = openAmount.subtract(paymentApplicationDocument.getNonAppliedHoldingAmount());
        
        if(KualiDecimal.ZERO.isGreaterThan(openAmount)) {
            addGlobalError(ArKeyConstants.PaymentApplicationDocumentErrors.CANNOT_APPLY_MORE_THAN_CASH_CONTROL_TOTAL_AMOUNT);
        }
        
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }
    
    private void applyToIndividualCustomerInvoiceDetails(PaymentApplicationDocumentForm paymentApplicationDocumentForm) throws WorkflowException{
        PaymentApplicationDocument paymentApplicationDocument = paymentApplicationDocumentForm.getPaymentApplicationDocument();
        String applicationDocNbr = paymentApplicationDocument.getDocumentNumber();

        // Handle amounts applied at the invoice detail level
        int customerInvoiceDetailCounter = 1;
        int simpleCustomerInvoiceDetailCounter = 0;
        for (CustomerInvoiceDetail customerInvoiceDetail : paymentApplicationDocumentForm.getCustomerInvoiceDetails()) {
            
            KualiDecimal amountToApply = null;
            
            String fieldName = "customerInvoiceDetail[" + (simpleCustomerInvoiceDetailCounter) + "]";
            // Increment now because we don't want the continue below to skip the increment.
            simpleCustomerInvoiceDetailCounter += 1;

            if(customerInvoiceDetail.isFullApply()) {
                // Apply the full detail amount
                fieldName += ".fullApply";
                amountToApply = customerInvoiceDetail.getAmountOpenExcludingAnyAmountFrom(paymentApplicationDocument);
            } else if (KualiDecimal.ZERO.equals(customerInvoiceDetail.getSpecialAppliedAmount())) {
                // Don't add lines where the amount to apply is zero. Wouldn't make any sense to do that.
                continue;
            } else {
                // Apply the partial amount
                fieldName += ".amountApplied";
                amountToApply = customerInvoiceDetail.getSpecialAppliedAmount();
            }
            
            // If the new invoice paid applied is valid, add it to the document
            if (null != applyToCustomerInvoiceDetail(customerInvoiceDetail, paymentApplicationDocument, amountToApply, fieldName)) {
                customerInvoiceDetailCounter++;
            }
        }
    }
    
    private void applyToInvoices(PaymentApplicationDocumentForm paymentApplicationDocumentForm) throws WorkflowException {
        PaymentApplicationDocument applicationDocument = (PaymentApplicationDocument) paymentApplicationDocumentForm.getDocument();
        String applicationDocumentNumber = applicationDocument.getDocumentNumber();

        // get the university fiscal year and fiscal period code
        Integer universityFiscalYear = applicationDocument.getAccountingPeriod().getUniversityFiscalYear();
        String universityFiscalPeriodCode = applicationDocument.getAccountingPeriod().getUniversityFiscalPeriodCode();

        List<String> invoiceNumbers = new ArrayList<String>();
        Collection<CustomerInvoiceDocument> invoices = paymentApplicationDocumentForm.getInvoices();

        for (CustomerInvoiceDocument customerInvoiceDocument : invoices) {
            if (customerInvoiceDocument.isQuickApply()) {
                invoiceNumbers.add(customerInvoiceDocument.getDocumentNumber());
            }
        }

        // make sure none of the invoices selected have zero open amounts, complain if so
        CustomerInvoiceDocument invoice = null;
        for (String invoiceNumber : invoiceNumbers) {
            invoice = customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(invoiceNumber);
            if (invoice.getOpenAmount().isZero()) {
                addGlobalError(ArKeyConstants.PaymentApplicationDocumentErrors.CANNOT_QUICK_APPLY_ON_INVOICE_WITH_ZERO_OPEN_AMOUNT);
                return;// mapping.findForward(KFSConstants.MAPPING_BASIC);
            }
        }

        KualiDecimal cashControlTotalAmount = applicationDocument.getCashControlDetail().getFinancialDocumentLineAmount();
        PaymentApplicationDocumentService applicationDocumentService = SpringContext.getBean(PaymentApplicationDocumentService.class);

        // go over the selected invoices and apply full amount to each of their details
        for (String customerInvoiceDocumentNumber : invoiceNumbers) {
            // get the customer invoice details for the current invoice number
            Collection<CustomerInvoiceDetail> customerInvoiceDetails = 
                customerInvoiceDocumentService.getCustomerInvoiceDetailsForCustomerInvoiceDocument(customerInvoiceDocumentNumber);
            
            for (CustomerInvoiceDetail customerInvoiceDetail : customerInvoiceDetails) {

                applyToCustomerInvoiceDetail(customerInvoiceDetail, applicationDocument, customerInvoiceDetail.getAmount(), "invoice[" + (customerInvoiceDocumentNumber) + "].quickApply");
                
            }

            if (customerInvoiceDocumentNumber.equals(paymentApplicationDocumentForm.getEnteredInvoiceDocumentNumber())) {
                paymentApplicationDocumentForm.setSelectedInvoiceDocument(customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(customerInvoiceDocumentNumber));
            }
        }
    }
    
    private void applyNonInvoiced(PaymentApplicationDocumentForm paymentApplicationDocumentForm) throws WorkflowException {
        PaymentApplicationDocument applicationDocument = (PaymentApplicationDocument) paymentApplicationDocumentForm.getDocument();
        
        NonInvoiced nonInvoiced = paymentApplicationDocumentForm.getNonInvoicedAddLine();
        
        // Only apply if the user entered an amount
        if(ObjectUtils.isNotNull(nonInvoiced.getFinancialDocumentLineAmount())) {
            nonInvoiced.setFinancialDocumentPostingYear(applicationDocument.getPostingYear());
            nonInvoiced.setDocumentNumber(applicationDocument.getDocumentNumber());
            nonInvoiced.setFinancialDocumentLineNumber(paymentApplicationDocumentForm.getNextNonInvoicedLineNumber());

            if (PaymentApplicationDocumentRuleUtil.validateNonInvoiced(nonInvoiced, applicationDocument)) {
                // add advanceDeposit
                applicationDocument.getNonInvoiceds().add(nonInvoiced);

                // clear the used advanceDeposit
                paymentApplicationDocumentForm.setNonInvoicedAddLine(new NonInvoiced());
            }
        }
    }
    
    private void applyUnapplied(PaymentApplicationDocumentForm paymentApplicationDocumentForm) throws WorkflowException {
        PaymentApplicationDocument applicationDocument = paymentApplicationDocumentForm.getPaymentApplicationDocument();
        NonAppliedHolding nonAppliedHolding = applicationDocument.getNonAppliedHolding();
        if (PaymentApplicationDocumentRuleUtil.validateNonAppliedHolding(applicationDocument)) {
            if (ObjectUtils.isNotNull(nonAppliedHolding)) {
                // Associate the non applied holding with the payment application document.
                if (ObjectUtils.isNull(nonAppliedHolding.getReferenceFinancialDocumentNumber())) {
                    nonAppliedHolding.setReferenceFinancialDocumentNumber(applicationDocument.getDocumentNumber());
                }
                // Force the customer number to upper case to the foreign key constraint passes.
                if (ObjectUtils.isNotNull(nonAppliedHolding.getCustomerNumber())) {
                    nonAppliedHolding.setCustomerNumber(nonAppliedHolding.getCustomerNumber().toUpperCase());
                }
                // businessObjectService.save(nonAppliedHolding);
            }
        }
    }

    /**
     * @see org.kuali.kfs.sys.web.struts.KualiAccountingDocumentActionBase#execute(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {

        PaymentApplicationDocumentForm applicationDocumentForm = (PaymentApplicationDocumentForm) form;
        initializeForm(applicationDocumentForm);
        ActionForward actionForward = super.execute(mapping, form, request, response);

        return actionForward;
    }

    /**
     * This method initializes the form
     * 
     * @param applicationDocumentForm
     */
    private void initializeForm(PaymentApplicationDocumentForm form) {
        if (null != form) {
            if (null != form.getDocument()) {
                PaymentApplicationDocument paymentApplicationDocument = form.getPaymentApplicationDocument();
                if (null != paymentApplicationDocument.getNonInvoicedDistributions()) {
                    for (NonInvoicedDistribution u : paymentApplicationDocument.getNonInvoicedDistributions()) {
                        if (null == form.getNextNonInvoicedLineNumber()) {
                            form.setNextNonInvoicedLineNumber(u.getFinancialDocumentLineNumber());
                        } else if (u.getFinancialDocumentLineNumber() > form.getNextNonInvoicedLineNumber()) {
                            form.setNextNonInvoicedLineNumber(u.getFinancialDocumentLineNumber());
                        }
                    }
                }
                // This step doesn't affect anything persisted to the database. It allows proper calculation
                // of amounts for the display.
                for(CustomerInvoiceDetail customerInvoiceDetail : form.getSelectedInvoiceDocument().getCustomerInvoiceDetailsWithoutDiscounts()) {
                    customerInvoiceDetail.setCurrentPaymentApplicationDocument(paymentApplicationDocument);
                }
                // Struts/Spring likes to set the nonAppliedHolding to a new instance
                // when it should be null (i.e. has not yet been set) 
                NonAppliedHolding holding = paymentApplicationDocument.getNonAppliedHolding();
                if(ObjectUtils.isNotNull(holding)) {
                    if(ObjectUtils.isNull(holding.getObjectId())) {
                        paymentApplicationDocument.setNonAppliedHolding(null);
                    }
                }
            }
            if (null == form.getNextNonInvoicedLineNumber()) {
                form.setNextNonInvoicedLineNumber(1);
            }
        }
    }

    /**
     * This method loads the invoices for currently selected customer
     * 
     * @param applicationDocumentForm
     */
    private void loadInvoices(PaymentApplicationDocumentForm applicationDocumentForm) throws WorkflowException {
        PaymentApplicationDocument applicationDocument = applicationDocumentForm.getPaymentApplicationDocument();
        String customerNumber = applicationDocument.getAccountsReceivableDocumentHeader() == null ? null : applicationDocument.getAccountsReceivableDocumentHeader().getCustomerNumber();
        String currentInvoiceNumber = applicationDocumentForm.getEnteredInvoiceDocumentNumber();

        // if customer number is null but invoice number is not null then get the customer number based on the invoice number
        if ((customerNumber == null || customerNumber.equals("")) && (currentInvoiceNumber != null && !currentInvoiceNumber.equals(""))) {
            Customer customer = customerInvoiceDocumentService.getCustomerByInvoiceDocumentNumber(currentInvoiceNumber);
            customerNumber = customer.getCustomerNumber();
            applicationDocument.getAccountsReceivableDocumentHeader().setCustomerNumber(customerNumber);
        }

        // get open invoices for the current customer
        Collection<CustomerInvoiceDocument> openInvoicesForCustomer = customerInvoiceDocumentService.getOpenInvoiceDocumentsByCustomerNumber(customerNumber);
        applicationDocumentForm.setInvoices(new ArrayList<CustomerInvoiceDocument>(openInvoicesForCustomer));

        // if no invoice number entered than get the first invoice
        if ((customerNumber != null && !customerNumber.equals("")) && (currentInvoiceNumber == null || "".equalsIgnoreCase(currentInvoiceNumber))) {
            if (applicationDocumentForm.getInvoices() != null && applicationDocumentForm.getInvoices().size() > 0) {
                currentInvoiceNumber = applicationDocumentForm.getInvoices().iterator().next().getDocumentNumber();
                applicationDocumentForm.setEnteredInvoiceDocumentNumber(currentInvoiceNumber);
            }
        }
        // set the selected invoice to be the first one in the list
        applicationDocumentForm.setSelectedInvoiceDocumentNumber(currentInvoiceNumber);

        if (currentInvoiceNumber != null && !currentInvoiceNumber.equals("")) {
            // load information for the current selected invoice
            applicationDocumentForm.setSelectedInvoiceDocument(customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(currentInvoiceNumber));
        }
    }

    /**
     * This method updates the customer invoice details when a new invoice is selected
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward goToInvoice(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm paymentApplicationDocumentForm = (PaymentApplicationDocumentForm) form;
        String currentInvoiceNumber = paymentApplicationDocumentForm.getSelectedInvoiceDocumentNumber();
        if (currentInvoiceNumber != null && !currentInvoiceNumber.equals("")) {
            // set entered invoice number to be the current selected invoice number
            paymentApplicationDocumentForm.setEnteredInvoiceDocumentNumber(currentInvoiceNumber);
            // load information for the current selected invoice
            paymentApplicationDocumentForm.setSelectedInvoiceDocument(customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(currentInvoiceNumber));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * This method updates customer invoice details when next invoice is selected
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward goToNextInvoice(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm paymentApplicationDocumentForm = (PaymentApplicationDocumentForm) form;
        String currentInvoiceNumber = paymentApplicationDocumentForm.getNextInvoiceDocumentNumber();
        if (currentInvoiceNumber != null && !currentInvoiceNumber.equals("")) {

            // set entered invoice number to be the current selected invoice number
            paymentApplicationDocumentForm.setEnteredInvoiceDocumentNumber(currentInvoiceNumber);
            paymentApplicationDocumentForm.setSelectedInvoiceDocumentNumber(currentInvoiceNumber);
            // load information for the current selected invoice
            paymentApplicationDocumentForm.setSelectedInvoiceDocument(customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(currentInvoiceNumber));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * This method updates customer invoice details when previous invoice is selected
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward goToPreviousInvoice(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm paymentApplicationDocumentForm = (PaymentApplicationDocumentForm) form;
        String currentInvoiceNumber = paymentApplicationDocumentForm.getPreviousInvoiceDocumentNumber();
        if (currentInvoiceNumber != null && !currentInvoiceNumber.equals("")) {

            // set entered invoice number to be the current selected invoice number
            paymentApplicationDocumentForm.setEnteredInvoiceDocumentNumber(currentInvoiceNumber);
            paymentApplicationDocumentForm.setSelectedInvoiceDocumentNumber(currentInvoiceNumber);
            // load information for the current selected invoice
            paymentApplicationDocumentForm.setSelectedInvoiceDocument(customerInvoiceDocumentService.getInvoiceByInvoiceDocumentNumber(currentInvoiceNumber));
        }

        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Set the customer so we can pull up invoices for that customer.
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward setCustomer(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm pform = (PaymentApplicationDocumentForm) form;
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Retrieve all invoices for the selected customer.
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward loadInvoices(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm pform = (PaymentApplicationDocumentForm) form;
        loadInvoices(pform);
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Cancel the document.
     * 
     * @see org.kuali.rice.kns.web.struts.action.KualiDocumentActionBase#cancel(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward cancel(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PaymentApplicationDocumentForm _form = (PaymentApplicationDocumentForm) form;
        if (null == _form.getCashControlDocument()) {
            return super.cancel(mapping, form, request, response);
        }
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * @see org.kuali.rice.kns.web.struts.action.KualiDocumentActionBase#createDocument(org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase)
     */
    @Override
    protected void createDocument(KualiDocumentFormBase kualiDocumentFormBase) throws WorkflowException {
        super.createDocument(kualiDocumentFormBase);
        PaymentApplicationDocumentForm form = (PaymentApplicationDocumentForm) kualiDocumentFormBase;
        PaymentApplicationDocument document = form.getPaymentApplicationDocument();

        // create new accounts receivable header and set it to the payment application document
        AccountsReceivableDocumentHeaderService accountsReceivableDocumentHeaderService = SpringContext.getBean(AccountsReceivableDocumentHeaderService.class);
        AccountsReceivableDocumentHeader accountsReceivableDocumentHeader = accountsReceivableDocumentHeaderService.getNewAccountsReceivableDocumentHeaderForCurrentUser();
        accountsReceivableDocumentHeader.setDocumentNumber(document.getDocumentNumber());
        document.setAccountsReceivableDocumentHeader(accountsReceivableDocumentHeader);
    }

    /**
     * @see org.kuali.rice.kns.web.struts.action.KualiDocumentActionBase#loadDocument(org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase)
     */
    @Override
    protected void loadDocument(KualiDocumentFormBase kualiDocumentFormBase) throws WorkflowException {
        super.loadDocument(kualiDocumentFormBase);
        PaymentApplicationDocumentForm pform = (PaymentApplicationDocumentForm) kualiDocumentFormBase;
        loadInvoices(pform);
    }

    /**
     * Get an error to display in the UI for a certain field.
     * 
     * @param propertyName
     * @param errorKey
     */
    private void addFieldError(String propertyName, String errorKey) {
        GlobalVariables.getErrorMap().putError(propertyName, errorKey);
    }

    /**
     * Get an error to display at the global level, for the whole document.
     * 
     * @param errorKey
     */
    private void addGlobalError(String errorKey) {
        GlobalVariables.getErrorMap().putErrorWithoutFullErrorPath(KNSConstants.DOCUMENT_ERRORS, errorKey, "document.hiddenFieldForErrors");
    }

}

/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kuali.kfs.fp.document.service;

import java.util.List;

import org.kuali.kfs.fp.document.CashReceiptDocument;
import org.kuali.rice.kim.api.identity.Person;

/**
 *
 * This service interface defines the methods that a CashReceiptService implementation must provide.
 */
public interface CashReceiptService {
    /**
     * This method retrieves the cash receipt verification unit for the given user.
     *
     * TODO: change this to do something other than return null (which will require updating CashReceiptDocumentAuthorizer, since
     * that's the one place I'm sure that returning a null is interpreted to mean that a user is a member of no verificationUnit)
     *
     * @param user The user to retrieve the cash receipt verification unit for.
     * @return Cash receipt verificationUnit campusCode associated with the given user; null if the user is not a member of any verification campus.
     */
    public String getCashReceiptVerificationUnitForUser(Person user);

    /**
     * Returns a List of CashReceiptDocuments for the given verification unit whose status matches the given status code.
     *
     * @param verificationUnit A verification unit for a cash receipt.
     * @param statusCode A cash receipt status code.
     * @return List of CashReceiptDocument instances.
     * @throws IllegalArgumentException Thrown if verificationUnit is blank
     * @throws IllegalArgumentException Thrown if statusCode is blank
     */
    public List getCashReceipts(String verificationUnit, String statusCode);

    /**
     * Returns a List of CashReceiptDocuments for the given verificationUnit whose status matches any of the status codes in the
     * given String[].
     *
     * @param verificationUnit A verification unit for a cash receipt.
     * @param statii A collection of potential cash receipt document statuses.
     * @return List of CashReceiptDocument instances.
     * @throws IllegalArgumentException Thrown if verificationUnit is blank
     * @throws IllegalArgumentException Thrown if statii is null or empty or contains any blank statusCodes
     */
    public List getCashReceipts(String verificationUnit, String[] statii);

    /**
     * This adds the currency and coin details associated with this Cash Receipt document to the proper cash drawer and to the
     * cumulative Cash Receipt details for the document which opened the cash drawer.
     *
     * @param crDoc The cash receipt document with cash details to add to the cash drawer.
     */
    public void addCashDetailsToCashDrawer(CashReceiptDocument crDoc);

    /**
     * Deprecated: Please use areCashAmountsInvalid.
     * Checks whether the CashReceiptDocument's cash totals are invalid, generating global errors if so.
     *
     * @param cashReceiptDocument submitted cash receipt document
     * @return true if CashReceiptDocument's cash totals are valid
     */
    @Deprecated
    public abstract boolean areCashTotalsInvalid(CashReceiptDocument cashReceiptDocument);

    /**
     * Checks whether the CashReceiptDocument's detail and total amounts in each category is invalid, generating global errors if so.
     * A Cash Receipt document is considered containing invalid amount if any of the following is true:
     *  1. The total check amount is negative;
     *  2. The total currency amount or coin amount is negative;
     *  3. The total change currency or coin amount is negative.
     * We are not checking the net total here, because that will be checked when the document routes.
     * Note:
     * This method now is optional, since we've added DD validation for each currency/coin roll/count field, to only allow
     * positive integer. This means that each individual denomination amount will be non-negative, resulting in only non-negative
     * total amount in each category. Further more, check amount is validated to be non-negative each time a new check is added,
     * which ensures that total check amount will be non-negative. Non the less, this method serves as a safe guard in case
     * amounts are set elsewhere other than from the CR document UI.
     *
     * @param cashReceiptDocument submitted cash receipt document
     * @return true if any of CashReceiptDocument's amount is invalid
     */
    public abstract boolean areCashAmountsInvalid(CashReceiptDocument cashReceiptDocument);
}


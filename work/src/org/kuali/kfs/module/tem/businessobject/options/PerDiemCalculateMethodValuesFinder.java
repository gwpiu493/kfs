/*
 * Copyright 2011 The Kuali Foundation.
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
package org.kuali.kfs.module.tem.businessobject.options;

import java.util.ArrayList;
import java.util.List;

import org.kuali.kfs.module.tem.TemConstants;
import org.kuali.rice.core.util.KeyLabelPair;
import org.kuali.rice.kns.lookup.keyvalues.KeyValuesBase;

public class PerDiemCalculateMethodValuesFinder extends KeyValuesBase {
    @Override
    @SuppressWarnings("rawtypes")
    public List getKeyValues() {
        List keyValues = new ArrayList();

        keyValues.add(new KeyLabelPair("", ""));
        keyValues.add(new KeyLabelPair(TemConstants.PERCENTAGE, "Percentage"));
        keyValues.add(new KeyLabelPair(TemConstants.QUARTER, "Quarter"));
        
        return keyValues;
    }
}

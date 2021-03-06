/*
 * Copyright (C) 2012 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.sds;

import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.util.ValidationUtils;
import au.org.ala.sds.validation.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import java.util.Map;

/**
 * This class provides the generic entry into the SDS. Users don't need to know how to use piece the components together.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SensitiveDataService {

    protected static final Logger logger = Logger.getLogger(SensitiveDataService.class);

    /** A flag passed in the property map used to determine if sampled values have been provided */
    public static final String SAMPLED_VALUES_PROVIDED = "samplesProvided";

    private ReportFactory reportFactory = new SdsReportFactory();

    public boolean isTaxonSensitive(SensitiveSpeciesFinder finder, String scientificName, String taxonId){
        return  getSensitiveTaxon(finder, scientificName, taxonId) != null;
    }

    private SensitiveTaxon getSensitiveTaxon(SensitiveSpeciesFinder finder, String scientificName, String taxonId) {
        SensitiveTaxon st = null;
        if(taxonId != null) {
            st = finder.findSensitiveSpeciesByLsid(taxonId);
        }

        if(st == null && scientificName != null) {
            st = finder.findSensitiveSpecies(scientificName);
        }
        return st;
    }

    public ValidationOutcome testMapDetails(SensitiveSpeciesFinder finder,
                                            Map<String, String> properties,
                                            String scientificName){
        return testMapDetails(finder, properties, scientificName, null);
    }

    /**
     * Tests the map details for sensitivity concerns.
     *
     * @param finder The SDS finder to use for the taxon searches. This will be initialised with an appropriate CBIndexSearcher
     * @param properties A map of the raw record details. This will allow the SDS to remove any properties that need to be hidden.
     * @param scientificName  The raw scientific name to match on - will only be used if taxon id is null
     * @param taxonId  The guid for the matched scientific name - this prevent superfluous name lookup from occurring.
     * @return A validation outcome to be used by the client processing. It is up to each client program to perform actions
     * resulting from the outcome.
     */
    public ValidationOutcome testMapDetails(SensitiveSpeciesFinder finder,
                                            Map<String, String> properties,
                                            String scientificName,
                                            String taxonId){
        //Step 1 apply rules for flags
        Configuration config = null;
        try {
            //Step 1 apply rules for flags
            config = Configuration.getInstance();
        } catch (Exception e){
            throw new RuntimeException("Problem initialising SDS.");
        }

        for(String rule : config.getFlagRules().split(",")){
            if(StringUtils.isNotBlank(properties.get(rule))){
                return restrictRecord(properties, rule, scientificName);
            }
        }

        //Only continue if no flags were discovered
        //Step 2 extract the sensitive species and validate the service
        //search for a sensitive taxon
        SensitiveTaxon st = getSensitiveTaxon(finder, scientificName, taxonId);

        //if taxon recognised as a sensitive species, run the validation
        if(st != null){
            ValidationService service = ServiceFactory.createValidationService(st);
            return service.validate(properties);
        }

        //species is not sensitive and can be loaded "as is"
        ValidationOutcome vo = new ValidationOutcome();
        vo.setLoadable(true);
        return vo;
    }

    /**
     * Performs the restriction for the "flag" rules.
     *
     * @param properties
     * @param rule
     * @param scientificName
     * @return
     */
    private ValidationOutcome restrictRecord(Map<String, String> properties, String rule, String scientificName){

        ValidationOutcome vo = new ValidationOutcome(reportFactory.createValidationReport(null));
        vo.setLoadable(true);
        vo.getReport().setCategory(rule);
        vo.getReport().setAssertion(MessageFactory.getMessageText(rule, scientificName));
        vo.getReport().addMessage(MessageFactory.createMessage(Message.Type.INFO, rule, scientificName));

        //now remove all the non-classification/attribution properties
        Map<String, Object> results = ValidationUtils.restrictForPests(properties);
        vo.setResult(results);
        return vo;
    }
}

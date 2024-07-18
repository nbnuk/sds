/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
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
 ***************************************************************************/
package au.org.ala.sds;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SearchTest {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
        TestUtils.initConfig();
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

    @Test
    public void lookupRufus() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Macropus rufus");
        assertNull(ss);
    }

    @Test
    public void lookupCrex() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Crex crex");
        assertNotNull(ss);
    }

    @Test
    public void lookupMitchellsByLsid() {
        SensitiveTaxon ss = finder.findSensitiveSpeciesByLsid("https://biodiversity.org.au/afd/taxa/5815e99d-01cd-4a92-99ba-36f480c4834d");
        assertNotNull(ss);
        assertEquals("Lophochroa leadbeateri", ss.getTaxonName());
    }

    @Test
    public void lookupMitchells() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Cacatua leadbeateri");
        assertNotNull(ss);
        assertEquals("Lophochroa leadbeateri", ss.getTaxonName());
    }
}

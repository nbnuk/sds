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
package au.org.ala.sds.model;

import au.org.ala.names.model.*;
import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.names.search.SearchResultException;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveTaxonStore implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Logger logger = Logger.getLogger(SensitiveTaxonStore.class);

    protected static final Set<MatchType> INACCURATE_MATCH = new HashSet<>(Arrays.asList(MatchType.RECURSIVE, MatchType.SOUNDEX, MatchType.VERNACULAR));

    private final List<SensitiveTaxon> taxonList;

    private final Map<String, Integer> lsidMap;
    private final Map<String, Integer> nameMap;

    private transient final ALANameSearcher namesSearcher;

    public SensitiveTaxonStore(SensitiveSpeciesDao dao, ALANameSearcher nameSearcher) throws Exception {
        this.namesSearcher = nameSearcher;
        this.lsidMap = new HashMap<String, Integer>();
        this.nameMap = new HashMap<String, Integer>();
        this.taxonList = dao.getAll();
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() {
        List<SensitiveTaxon> additionalAcceptedTaxons = new ArrayList<SensitiveTaxon>();

        for (SensitiveTaxon st : taxonList) {
            NameSearchResult match = lookupName(st.getTaxonName(), st.getFamily(), st.getRank());
            if (match != null) {
                st.setLsid(match.getLsid());
                if (match.isSynonym()) {
                    NameSearchResult accepted = getAcceptedNameFromSynonym(match);
                    if (accepted != null) {
                        String acceptedName = accepted.getRankClassification().getScientificName();
                        //logger.info("Sensitive species '" + st.getName() + "' is not accepted name - using '" + acceptedName + "'");
                        //SensitiveTaxon acceptedTaxon = findByExactMatch(acceptedName);
                        //NBN: the problem with the above logic, is that the accepted scientificName can be the same as the synonym (if the synonym is e.g. the naked name)
                        String acceptedLsid = accepted.getLsid();
                        st.setLsid(acceptedLsid);
                        logger.info("Sensitive species '" + st.getName() + "' is not accepted name - using name for lsid '" + acceptedLsid + "'");
                        SensitiveTaxon acceptedTaxon = findByLsid(acceptedLsid);
                        if (acceptedTaxon == null) {
                            acceptedTaxon = findByExactMatch(acceptedName); //revert to this
                        }
                        if (acceptedTaxon == null) {
                            acceptedTaxon = new SensitiveTaxon(acceptedName, StringUtils.contains(acceptedName, ' ') ? RankType.SPECIES : RankType.GENUS);
                            acceptedTaxon.setLsid(accepted.getLsid());
                            if (!additionalAcceptedTaxons.contains(acceptedTaxon)) {
                                additionalAcceptedTaxons.add(acceptedTaxon);
                                logger.info("Accepted name '" + acceptedName + "' (" + acceptedTaxon.getLsid() + ") added to sensitive taxon list");
                            }
                        }
                        st.setAcceptedName(acceptedName);
                    }
                }
                logger.debug(st.getName() + (st.getAcceptedName() == null ? "" : " (" + st.getAcceptedName() + ")") + "\t" + st.getLsid());
            } else {
                logger.warn("Sensitive species '" + st.getName() + "' not found in NameMatching index");
            }
        }

        // Add additional accepted sensitive taxa
        taxonList.addAll(additionalAcceptedTaxons);
        Collections.sort(taxonList);

        // Construct lookup maps and deal with synonym sensitivity instances
        for (int i = 0; i < this.taxonList.size(); i++) {
            SensitiveTaxon st = taxonList.get(i);
            String lsid = st.getLsid();
            if (StringUtils.isNotBlank(lsid)) {
                lsidMap.put(st.getLsid(), i);
            }
            if (st.getAcceptedName() != null) {
                SensitiveTaxon acceptedTaxon = findByExactMatch(st.getAcceptedName());
                if (acceptedTaxon != null) {
                    for (SensitivityInstance si : st.getInstances()) {
                        if (!acceptedTaxon.getInstances().contains(si)) {
                            acceptedTaxon.getInstances().add(si);
                        }
                    }
                    st.setAcceptedTaxon(acceptedTaxon);
                } else {
                    logger.error("Accepted taxon '" + st.getAcceptedName() + "' not found in taxon list");
                }
            } else {
                if (StringUtils.isNotBlank(lsid)) {
                    nameMap.put(st.getName(), i);
                    logger.debug("Added '" + st.getName() + "' to nameMap");
                }
            }
        }
    }

    public SensitiveTaxon findByName(String name) {
        String acceptedName = name;
        NameSearchResult result = getAcceptedName(name);
        if (result != null) {
            acceptedName = result.getRankClassification().getScientificName();
        }

        Integer nameIndex = nameMap.get(acceptedName);
        if (nameIndex != null) {
            return taxonList.get(nameIndex);
        } else {
            // Try binary search
            return findByExactMatch(name);
        }
    }

    public SensitiveTaxon findByAcceptedName(String acceptedName) {
        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return taxonList.get(index);
        } else {
            return null;
        }
    }

    public SensitiveTaxon findByLsid(String lsid) {
        Integer index = lsidMap.get(lsid);
        if (index != null) {
            return taxonList.get(index);
        } else {
            return null;
        }
    }

    public SensitiveTaxon findByExactMatch(String name) {
        // Do binary search
        int idx = Collections.binarySearch(taxonList, new SensitiveTaxon(name, StringUtils.contains(name, ' ') ? RankType.SPECIES : RankType.GENUS));
        if (idx >= 0 && taxonList.get(idx).getName().equalsIgnoreCase(name)) {
            return taxonList.get(idx);
        } else {
            return null;
        }
    }

    private NameSearchResult getAcceptedName(String name) {
        NameSearchResult match = null;
        if (namesSearcher != null) {
            try {
                match = this.lookupName(name, null, null);
                if (match != null && match.isSynonym()) {
                    match = getAcceptedNameFromSynonym(match);
                }
            } catch (RuntimeException e) {
                logger.error("'" + name + "'", e);
            }
        }

        return match;
    }

    private NameSearchResult lookupName(String name, String family, RankType rank) {
        NameSearchResult match = null;
        if (namesSearcher != null) {
            try {
                LinnaeanRankClassification lrc = new LinnaeanRankClassification(null, null, null, null, family, null, name);
                lrc.setRank(rank == null ? null : rank.getRank());
                MetricsResultDTO metrics = namesSearcher.searchForRecordMetrics(lrc, false, false);
                if (metrics != null) {
                    match = metrics.getResult();
                    if (match != null && INACCURATE_MATCH.contains(match.getMatchType())) {
                        logger.error("Inaccurate match type " + match.getMatchType() + " for " + name);
                        match = null;
                    }
                    if (!metrics.getErrors().contains(ErrorType.NONE)){
                        logger.warn("Name search for " + name + " contains flags " + metrics.getErrors());
                    }
                }
            } catch (SearchResultException ex) {
                logger.error("Error searching for " + name, ex);
            }
        }
        return match;
    }

    private NameSearchResult getAcceptedNameFromSynonym(NameSearchResult match) {
        NameSearchResult accepted;
        if (match.isSynonym()) {
            accepted = namesSearcher.searchForRecordByLsid(match.getAcceptedLsid());
            if (accepted == null) {
                logger.error("Could not find accepted name for synonym '" + match.getRankClassification().getScientificName() + "' - " + match.getLsid() + " - " + match.getAcceptedLsid());
            }
            return accepted;
        } else {
            return match;
        }
    }

    public int getTaxonCount(){
        if(taxonList != null)
            return taxonList.size();
        return 0;
    }
}

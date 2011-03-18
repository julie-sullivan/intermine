package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.GOTerm;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Builds datastructure from go parent id to go term id.  Includes evidence codes.
 * @author julie
 */
public class GeneOntologyDisplayer extends CustomDisplayer
{

    private static final Set<String> ONTOLOGIES = new HashSet<String>();
    Map<String, Map<GOTerm, Set<String>>> goTermsByOntology = new HashMap<String, Map<GOTerm,
        Set<String>>>();

    /**
     * @param config config
     * @param im API
     */
    public GeneOntologyDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    static {
        ONTOLOGIES.add("GO:0008150");
        ONTOLOGIES.add("GO:0003674");
        ONTOLOGIES.add("GO:0005575");
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {
        Model model = im.getModel();
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        InterMineObject object = (InterMineObject) request.getAttribute("object");
        String primaryIdentifier = null;
        try {
            primaryIdentifier = (String) object.getFieldValue("primaryIdentifier");
        } catch (IllegalAccessException e) {
            return;
        }
        if (StringUtils.isEmpty(primaryIdentifier)) {
            return;
        }

        PathQuery query = buildQuery(model, displayObject.getId());
        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();
            String parentTerm = (String) row.get(0).getField();
            GOTerm term = (GOTerm) row.get(1).getObject();
            String code = (String) row.get(2).getField();
            addToOntologyMap(parentTerm, term, code);
        }

        request.setAttribute("goTerms", goTermsByOntology);
    }

    private void addToOntologyMap(String namespace, GOTerm term, String evidenceCode) {
        Map<GOTerm, Set<String>> termToEvidence = goTermsByOntology.get(namespace);
        if (termToEvidence == null) {
            termToEvidence = new HashMap<GOTerm, Set<String>>();
            goTermsByOntology.put(namespace, termToEvidence);
        }
        Set<String> codes = termToEvidence.get(term);
        if (codes == null) {
            codes = new HashSet<String>();
            termToEvidence.put(term, codes);
        }
        codes.add(evidenceCode);
    }

    private PathQuery buildQuery(Model model, Integer geneId) {
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.goAnnotation.ontologyTerm.parents.name",
                "Gene.goAnnotation.ontologyTerm.name",
                "Gene.goAnnotation.evidence.code.code");
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.parents.name", OrderDirection.ASC);
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.name", OrderDirection.ASC);

        // parents have to be main ontology
        q.addConstraint(Constraints.oneOfValues("Gene.goAnnotation.ontologyTerm.parents.identifier",
                ONTOLOGIES));

        // not a NOT relationship
        q.addConstraint(Constraints.isNull("Gene.goAnnotation.qualifier"));

        // gene from report page
        q.addConstraint(Constraints.eq("Gene.id", "" + geneId));

        return q;
    }
}

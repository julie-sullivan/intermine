package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.OrderBy;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.widget.GraphCategoryURLGenerator;

import org.jfree.data.category.CategoryDataset;


/**
 *
 * @author Julie Sullivan
 */
public class ChromosomeDistributionGraphURLGenerator implements GraphCategoryURLGenerator
{
    String bagName;

    /**
     * Creates a ChromosomeDistributionGraphURLGenerator for the chart
     * @param model
     * @param bagName name of bag for which to render this widget
     */
    public ChromosomeDistributionGraphURLGenerator(String bagName) {
        super();
        this.bagName = bagName;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.urls.CategoryURLGenerator#generateURL(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateURL(CategoryDataset dataset,
                              @SuppressWarnings("unused") int series,
                              int category) {
        StringBuffer sb = new StringBuffer("queryForGraphAction.do?bagName=" + bagName);
        sb.append("&category=" + dataset.getColumnKey(category));
        sb.append("&series=");
        sb.append("&urlGen=org.intermine.bio.web.widget.ChromosomeDistributionGraphURLGenerator");

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery(ObjectStore os,
                                       InterMineBag imBag,
                                       @SuppressWarnings("unused") String series,
                                       @SuppressWarnings("unused") String category) {

        Model model = os.getModel();
        InterMineBag bag = imBag;
        PathQuery q = new PathQuery(model);

        Path identifier = MainHelper.makePath(model, q, "Gene.identifier");
        Path organismDbId = MainHelper.makePath(model, q, "Gene.organismDbId");
        Path name = MainHelper.makePath(model, q, "Gene.organism.name");
        Path chromoIdentifier = MainHelper.makePath(model, q, "Gene.chromosome.identifier");
        Path start = MainHelper.makePath(model, q, "Gene.chromosomeLocation.start");
        Path end = MainHelper.makePath(model, q, "Gene.chromosomeLocation.end");
        Path strand = MainHelper.makePath(model, q, "Gene.chromosomeLocation.strand");

        List<Path> view = new ArrayList<Path>();

        view.add(identifier);
        view.add(organismDbId);
        view.add(name);
        view.add(chromoIdentifier);
        view.add(start);
        view.add(end);
        view.add(strand);

        q.setView(view);

        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();

        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        //  constrain to be specific chromosome
        constraintOp = ConstraintOp.EQUALS;
        code = q.getUnusedConstraintCode();
        PathNode chromosomeNode = q.addNode("Gene.chromosome.identifier");
        Constraint chromosomeConstraint
                        = new Constraint(constraintOp, series, false, label, code, id, null);
        chromosomeNode.getConstraints().add(chromosomeConstraint);

        q.setConstraintLogic("A and B");

        List<OrderBy>  sortOrder = new ArrayList<OrderBy>();

        sortOrder.add(new OrderBy(start, "asc"));
        sortOrder.add(new OrderBy(identifier, "asc"));
        sortOrder.add(new OrderBy(organismDbId, "asc"));
        sortOrder.add(new OrderBy(name, "asc"));
        sortOrder.add(new OrderBy(chromoIdentifier, "asc"));
        sortOrder.add(new OrderBy(end, "asc"));
        sortOrder.add(new OrderBy(strand, "asc"));

        q.setSortOrder(sortOrder);

        q.syncLogicExpression("and");

        return q;
    }
}

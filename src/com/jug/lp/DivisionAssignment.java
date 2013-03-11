/**
 *
 */
package com.jug.lp;

import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class DivisionAssignment extends AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > {

	@SuppressWarnings( "unused" )
	private final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >, Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes;
	@SuppressWarnings( "unused" )
	private final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> from;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> toUpper;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> toLower;

	/**
	 * Creates an DivisionAssignment.
	 *
	 * @param nodes
	 * @param edges
	 * @param from
	 * @param to1
	 * @param to2
	 * @throws GRBException
	 */
	public DivisionAssignment( final int t, final GRBVar ilpVariable, final GRBModel model,
			final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >,
			Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes,
			final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
			AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges,
		    final Hypothesis< ComponentTreeNode< DoubleType, ? >> from,
		    final Hypothesis< ComponentTreeNode< DoubleType, ? >> toUpper,
		    final Hypothesis< ComponentTreeNode< DoubleType, ? >> toLower ) throws GRBException {
		super( GrowthLineTrackingILP.ASSIGNMENT_DIVISION, ilpVariable, model );
		this.from = from;
		this.toUpper = toUpper;
		this.toLower = toLower;
		this.edges = edges;
		this.nodes = nodes;
	}

	/**
	 * This method is void. DIVISION assignments do not come with assignment
	 * specific constrains...
	 *
	 * @throws GRBException
	 * @see com.jug.lp.AbstractAssignment#addConstraintsToLP(gurobi.GRBModel,
	 *      com.jug.lp.AssignmentsAndHypotheses,
	 *      com.jug.lp.HypothesisNeighborhoods)
	 */
	@Override
	public void addConstraintsToLP() throws GRBException {
		// DIVISION assignments do not come with assignment specific
		// constrains...
	}

}

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
public class MappingAssignment extends AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > {

	@SuppressWarnings( "unused" )
	private final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >, Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes;
	@SuppressWarnings( "unused" )
	private final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> from;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> to;

	/**
	 * Creates an MappingAssignment.
	 *
	 * @param nodes
	 * @param edges
	 * @param from
	 * @param to
	 * @throws GRBException
	 */
	public MappingAssignment( final int t, final GRBVar ilpVariable, final GRBModel model,
			final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >,
						Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes,
			final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
						AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges,
			final Hypothesis<ComponentTreeNode<DoubleType,?>> from,
			final Hypothesis< ComponentTreeNode< DoubleType, ? >> to ) throws GRBException {
		super( GrowthLineTrackingILP.ASSIGNMENT_MAPPING, ilpVariable, model );
		this.from = from;
		this.to = to;
		this.edges = edges;
		this.nodes = nodes;
	}

	/**
	 * This method is void. MAPPING assignments do not come with assignment
	 * specific constrains...
	 *
	 * @throws GRBException
	 * @see com.jug.lp.AbstractAssignment#addConstraintsToLP(gurobi.GRBModel,
	 *      com.jug.lp.AssignmentsAndHypotheses,
	 *      com.jug.lp.HypothesisNeighborhoods)
	 */
	@Override
	public void addConstraintsToLP() throws GRBException {
	}

	/**
	 * Returns the segmentation hypothesis this mapping-assignment comes from
	 * (the one at the earlier time-point t).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< ComponentTreeNode< DoubleType, ? >> getSourceHypothesis() {
		return from;
	}

	/**
	 * Returns the segmentation hypothesis this mapping-assignment links to
	 * (the one at the later time-point t+1).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< ComponentTreeNode< DoubleType, ? >> getDestinationHypothesis() {
		return to;
	}

}

/**
 *
 */
package com.jug.lp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.List;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class ExitAssignment extends AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > {

	private final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> Hup;
	@SuppressWarnings( "unused" )
	private final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >, Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes;
	private final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges;
	@SuppressWarnings( "unused" )
	private final Hypothesis< ComponentTreeNode< DoubleType, ? >> who;

	private static int dcId = 0;

	/**
	 * Creates an ExitAssignment.
	 *
	 * @param nodes
	 * @param edges
	 * @param who
	 * @throws GRBException
	 */
	public ExitAssignment(final int t, final GRBVar ilpVariable, final GRBModel model,
			final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >, Hypothesis< ComponentTreeNode< DoubleType, ? > > > nodes,
			final HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edges,
			final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> Hup,
			final Hypothesis< ComponentTreeNode< DoubleType, ? >> who ) throws GRBException {
		super( GrowthLineTrackingILP.ASSIGNMENT_EXIT, ilpVariable, model );
		this.Hup = Hup;
		this.edges = edges;
		this.nodes = nodes;
		this.who = who;
	}

	/**
	 * @throws GRBException
	 * @see com.jug.lp.AbstractAssignment#addConstraintsToLP(gurobi.GRBModel,
	 *      com.jug.lp.AssignmentsAndHypotheses,
	 *      com.jug.lp.HypothesisNeighborhoods)
	 */
	@Override
	public void addConstraintsToLP() throws GRBException {
		final GRBLinExpr expr = new GRBLinExpr();

		expr.addTerm( Hup.size(), this.getGRBVar() );

		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> rightHyp : Hup ) {
			if ( edges.getLeftNeighborhood( rightHyp ) != null ) {
				for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a_j : edges.getLeftNeighborhood( rightHyp ) ) {
					expr.addTerm( 1.0, a_j.getGRBVar() );
				}
			}
		}

		model.addConstr( expr, GRB.LESS_EQUAL, Hup.size(), "dc_" + dcId );
		dcId++;
	}

	/**
	 * Returns the segmentation hypothesis this term-assignment is associated
	 * with.
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< ComponentTreeNode< DoubleType, ? >> getAssociatedHypothesis() {
		return who;
	}
}

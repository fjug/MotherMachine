/**
 *
 */
package com.jug.lp;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

import com.jug.GrowthLine;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;


/**
 * @author jug
 */
public class GrowthLineTrackingILP< H extends Hypothesis< ? >, A extends AbstractAssignment< H > > {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	public static int OPTIMIZATION_NEVER_PERFORMED = 0;
	public static int OPTIMAL = 1;
	public static int INFEASIBLE = 2;
	public static int UNBOUNDED = 3;
	public static int SUBOPTIMAL = 4;
	public static int NUMERIC = 5;
	public static int LIMIT_REACHED = 6;

	public static int ASSIGNMENT_EXIT = 0;
	public static int ASSIGNMENT_MAPPING = 1;
	public static int ASSIGNMENT_DIVISION = 2;

	public static GRBEnv env;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final GrowthLine gl;

	public GRBModel model;
	private int status;

	public final AssignmentsAndHypotheses<
		AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >,
		Hypothesis< ComponentTreeNode< DoubleType, ? > > >              nodes =
				new AssignmentsAndHypotheses<
							AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > >,
							Hypothesis< ComponentTreeNode< DoubleType, ? > > >();
	public final HypothesisNeighborhoods<
		Hypothesis< ComponentTreeNode< DoubleType, ? > >,
		AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > edgeSets =
				new HypothesisNeighborhoods< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
					AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > >();


	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public GrowthLineTrackingILP(final GrowthLine gl) {
		this.gl = gl;

		// Setting static stuff (this IS ugly!)
		if ( env == null ) {
			try {
				env = new GRBEnv( "MotherMachineILPs.log" );
			}
			catch ( final GRBException e ) {
				System.out.println( "GrowthLineTrackingILP::env could not be initialized!" );
				e.printStackTrace();
			}
		}

		try {
			model = new GRBModel( env );
		}
		catch ( final GRBException e ) {
			System.out.println( "GrowthLineTrackingILP::model could not be initialized!" );
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------------------
	// getters & setters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the status. This status returns one of the following values:
	 *         OPTIMIZATION_NEVER_PERFORMED, OPTIMAL, INFEASABLE, UNBOUNDED,
	 *         SUBOPTIMAL, NUMERIC, or LIMIT_REACHED. Values 2-6 correspond
	 *         directly to the ones from gurobi, the last one is set when none
	 *         of the others was actually returned by gurobi.
	 *         OPTIMIZATION_NEVER_PERFORMED shows, that the optimizer was never
	 *         started on this ILP setup.
	 */
	public int getStatus() {
		return status;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Adds all hypothesis given by the nodes in the component tree to
	 * <code>nodes</code>. Note: order is chosen such that the hypothesis are
	 * ordered ascending by right interval end.
	 *
	 * @param ctRoot
	 * @param t
	 */
	public void recursivelyAddCTNsAsHypotheses( final ComponentTreeNode< DoubleType, ? > ctNode, final int t ) {
		// do the same for all children
		for ( final ComponentTreeNode< DoubleType, ? > ctChild : ctNode.getChildren() ) {
			recursivelyAddCTNsAsHypotheses( ctChild, t );
		}
		// add the current ctNode as Hypothesis (including corresponding costs)
		final Pair< Integer, Integer > segInterval = ComponentTreeUtils.getTreeNodeInterval( ctNode );
		final int a = segInterval.a.intValue();
		final int b = segInterval.b.intValue();
		// TODO complete redesign is really necessary... giving null does only
		// work, because the gapsepvals are lazyly evaluated!
		final double[] gapSepFkt = gl.getFrames().get( t ).getGapSeparationValues( null );
		final double max = SimpleFunctionAnalysis.getMax( gapSepFkt, a, b ).b.doubleValue();
		final double sum = SimpleFunctionAnalysis.getSum( gapSepFkt, a, b );

		final double cost = -1 * 0.5 * ( ( b - a ) * max - sum );
		nodes.addHypothesis( t, new Hypothesis< ComponentTreeNode< DoubleType, ? > >( ctNode, cost ) );
	}

	/**
	 * @param t
	 * @param hyps
	 * @throws GRBException
	 */
	public void addExitAssignments( final int t, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps ) throws GRBException {
		final double cost = 0.0;

		int i = 0;
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : hyps ) {
			final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, String.format( "a_%d^EXIT--%d", t, i ) );
			final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> Hup = LpUtils.getHup( hyp, hyps );
			final ExitAssignment ea = new ExitAssignment( t, newLPVar, model, nodes, edgeSets, Hup, hyp );
			nodes.addAssignment( t, ea );
			edgeSets.addToRightNeighborhood( hyp, ea );
			i++;
		}
	}

	/**
	 * @param t
	 * @param curHyps
	 * @param nxtHyps
	 * @throws GRBException
	 */
	public void addMappingAssignments( final int t, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> curHyps, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> nxtHyps ) throws GRBException {
		double cost;

		int i = 0;
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> from : curHyps ) {
			int j = 0;
			for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> to : nxtHyps ) {
				if ( !( ComponentTreeUtils.isBelow( to.getWrappedHypothesis(), from.getWrappedHypothesis() ) ) ) {
					cost = from.getCosts() + to.getCosts() + compatibilityCostOfMapping( from, to );
//					System.out.println( String.format( "a_%d^MAPPING--(%d,%d)  ==> %f", t, i, j, cost ) );
					final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, String.format( "a_%d^MAPPING--(%d,%d)", t, i, j ) );
					final MappingAssignment ma = new MappingAssignment( t, newLPVar, model, nodes, edgeSets, from, to );
					nodes.addAssignment( t, ma );
					edgeSets.addToRightNeighborhood( from, ma );
					edgeSets.addToLeftNeighborhood( to, ma );
					j++;
				}
			}
			i++;
		}
	}

	/**
	 * @param from
	 * @param to
	 * @return
	 */
	private double compatibilityCostOfMapping( final Hypothesis< ComponentTreeNode< DoubleType, ? >> from, final Hypothesis< ComponentTreeNode< DoubleType, ? >> to ) {
		final long sizeFrom = from.getWrappedHypothesis().getSize();
		final long sizeTo = to.getWrappedHypothesis().getSize();
		final double valueFrom = from.getWrappedHypothesis().getValue().get();
		final double valueTo = to.getWrappedHypothesis().getValue().get();
		final Pair< Integer, Integer > intervalFrom = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalTo = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );

		final double costDeltaL = Math.max( sizeFrom - sizeTo, 0 );
		final double costDeltaV = Math.abs( valueFrom - valueTo );
		final double costDeltaH = 0.5 * Math.max( intervalFrom.b.intValue() - intervalFrom.a.intValue(), 0 ) + 0.5 * Math.max( intervalTo.b.intValue() - intervalTo.a.intValue(), 0 );

		return costDeltaL + costDeltaV + costDeltaH;
	}

	/**
	 * @param t
	 * @param curHyps
	 * @param nxtHyps
	 * @throws GRBException
	 */
	public void addDivisionAssignments( final int t, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> curHyps, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> nxtHyps ) throws GRBException {
		double cost;

		int i = 0;
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> from : curHyps ) {
			int j = 0;
			for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> to : nxtHyps ) {
				if ( !( ComponentTreeUtils.isBelow( to.getWrappedHypothesis(), from.getWrappedHypothesis() ) ) ) {
					for ( final ComponentTreeNode< DoubleType, ? > neighborCTN : ComponentTreeUtils.getRightNeighbors( to.getWrappedHypothesis() ) ) {
						@SuppressWarnings( "unchecked" )
						final Hypothesis< ComponentTreeNode< DoubleType, ? > > lowerNeighbor = ( Hypothesis< ComponentTreeNode< DoubleType, ? >> ) nodes.findHypothesisContaining( neighborCTN );
						if ( lowerNeighbor == null ) {
							System.out.println( "CRITICAL BUG!!!! Check GrowthLineTimeSeris::adDivisionAssignment(...)" );
						} else {
							cost = from.getCosts() + to.getCosts() + lowerNeighbor.getCosts() + compatibilityCostOfDivision( from, to, lowerNeighbor );
							final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, String.format( "a_%d^DIVISION--(%d,%d)", t, i, j ) );
							final DivisionAssignment da = new DivisionAssignment( t, newLPVar, model, nodes, edgeSets, from, to, lowerNeighbor );
							nodes.addAssignment( t, da );
							edgeSets.addToRightNeighborhood( from, da );
							edgeSets.addToLeftNeighborhood( to, da );
							edgeSets.addToLeftNeighborhood( lowerNeighbor, da );
							j++;
						}
					}
				}
			}
			i++;
		}
	}

	/**
	 * @param from
	 * @param to
	 * @param lowerNeighbor
	 * @return
	 */
	private double compatibilityCostOfDivision( final Hypothesis< ComponentTreeNode< DoubleType, ? >> from, final Hypothesis< ComponentTreeNode< DoubleType, ? >> toUpper, final Hypothesis< ComponentTreeNode< DoubleType, ? >> toLower ) {
		final long sizeFrom = from.getWrappedHypothesis().getSize();
		final long sizeToU = toUpper.getWrappedHypothesis().getSize();
		final long sizeToL = toLower.getWrappedHypothesis().getSize();
		final long sizeTo = sizeToU + sizeToL;
		final double valueFrom = from.getWrappedHypothesis().getValue().get();
		final double valueTo = 0.5 * ( toUpper.getWrappedHypothesis().getValue().get() + toLower.getWrappedHypothesis().getValue().get() );
		final Pair< Integer, Integer > intervalFrom = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalToU = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalToL = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );

		final double costDeltaL = Math.max( sizeFrom - sizeTo, 0 );
		final double costDeltaV = Math.abs( valueFrom - valueTo );
		final double costDeltaH = 0.5 * Math.max( intervalFrom.b.intValue() - intervalFrom.a.intValue(), 0 ) + 0.5 * Math.max( intervalToL.b.intValue() - intervalToU.a.intValue(), 0 );
		final double costDeltaS = Math.abs( sizeToU - sizeToL );

		return costDeltaL + costDeltaV + costDeltaH + costDeltaS;
	}

	/**
	 * @throws GRBException
	 *
	 */
	public void addPathBlockingConstraint() throws GRBException {
		// For each time-point
		for ( int t = 0; t < gl.size(); t++ ) {
			final ComponentTree< DoubleType, ? > ct = gl.get( t ).getComponentTree();
			for ( final ComponentTreeNode< DoubleType, ? > ctRoot : ct.roots() ) {
				recursivelyAddPathBlockingConstraints( ctRoot );
			}
		}
	}

	/**
	 *
	 * @param ctRoot
	 * @throws GRBException
	 */
	private void recursivelyAddPathBlockingConstraints( final ComponentTreeNode< DoubleType, ? > ctNode ) throws GRBException {
		int pbcId = 0;

		// if ctNode is a leave node -> add constraint (by going up the list of
		// parents and building up the constraint)
		if ( ctNode.getChildren().size() == 0 ) {
			ComponentTreeNode< DoubleType, ? > runnerNode = ctNode;

			final GRBLinExpr expr = new GRBLinExpr();
			while ( runnerNode != null ) {
				@SuppressWarnings( "unchecked" )
				final Hypothesis< ComponentTreeNode< DoubleType, ? > > hypothesis = ( Hypothesis< ComponentTreeNode< DoubleType, ? >> ) nodes.findHypothesisContaining( runnerNode );
				if ( edgeSets.getRightNeighborhood( hypothesis ) != null ) {
					for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a : edgeSets.getRightNeighborhood( hypothesis ) ) {
						expr.addTerm( 1.0, a.getGRBVar() );
					}
				}
				runnerNode = runnerNode.getParent();
			}
			model.addConstr( expr, GRB.LESS_EQUAL, 1.0, "pbc_" + pbcId );
			pbcId++;
		} else {
			// if ctNode is a inner node -> recursion
			for ( final ComponentTreeNode< DoubleType, ? > ctChild : ctNode.getChildren() ) {
				recursivelyAddPathBlockingConstraints( ctChild );
			}
		}
	}

	/**
	 *
	 */
	public void addExplainationContinuityConstraints() throws GRBException {
		int eccId = 0;

		// For each time-point
		for ( int t = 1; t < gl.size() - 1; t++ ) { // !!! starting from 1 !!!
			final GRBLinExpr expr = new GRBLinExpr();

			for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> leftHyp : nodes.getHypothesesAt( t ) ) {
				if ( edgeSets.getRightNeighborhood( leftHyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a_i : edgeSets.getRightNeighborhood( leftHyp ) ) {
						expr.addTerm( 1.0, a_i.getGRBVar() );
					}
				}
			}
			for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> rightHyp : nodes.getHypothesesAt( t + 1 ) ) {
				if ( edgeSets.getLeftNeighborhood( rightHyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a_j : edgeSets.getLeftNeighborhood( rightHyp ) ) {
						expr.addTerm( -1.0, a_j.getGRBVar() );
					}
				}
			}

			model.addConstr( expr, GRB.EQUAL, 0.0, "ecc_" + eccId );
			eccId++;
		}
	}

	/**
	 *
	 */
	public void run() {
		try {
			// RUN + return true if solution is feasible
			// - - - - - - - - - - - - - - - - - - - - -
			model.optimize();

			// Read solution and extract interpretation
			// - - - - - - - - - - - - - - - - - - - - -
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.OPTIMAL ) {
				status = OPTIMAL;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.INFEASIBLE ) {
				status = INFEASIBLE;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.UNBOUNDED ) {
				status = UNBOUNDED;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.SUBOPTIMAL ) {
				status = SUBOPTIMAL;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.NUMERIC ) {
				status = NUMERIC;
			} else {
				status = LIMIT_REACHED;
			}
		}
		catch ( final GRBException e ) {
			System.out.println( "Could not run the generated ILP!" );
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	public List< ComponentTreeNode< DoubleType, ? >> getOptimalSegmentation( final int t ) {
		//TODO implement!
		return null;
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	public List< H > getOptimalHypotheses( final int t ) {
		final ArrayList< H > ret = new ArrayList< H >();

		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : hyps ) {

		}

		return ret;
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	public HashMap< H, Set< A > > getOptimalLeftAssignments( final int t ) {
		final HashMap< H, Set< A > > ret = new HashMap< H, Set< A > >();
		// TODO implement
		return ret;
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	private A getOptimalLeftAssignment( final int t, final H hypothesis ) {
		return findActiveAssignment( edgeSets.getLeftNeighborhood( hypothesis ) ); // FIXME
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	public HashMap< H, Set< A > > getOptimalRightAssignments( final int t ) {
		final HashMap< H, Set< A > > ret = new HashMap< H, Set< A > >();
		// TODO implement
		return ret;
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	private A getOptimalRightAssignment( final int t, final H hypothesis ) {
		return findActiveAssignment( edgeSets.getRightNeighborhood( hypothesis ) ); // FIXME
	}

	/**
	 *
	 * @param t
	 * @return
	 * @throws GRBException
	 */
	private A findActiveAssignment( final Set< A > assignments ) throws GRBException {
		for ( final A a : assignments ) {
			if ( a.isChoosen() ) { return a; }
		}
		return null;
	}

}
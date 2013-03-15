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
public class GrowthLineTrackingILP {

	// < H extends Hypothesis< ComponentTreeNode< DoubleType, ? > >, A extends AbstractAssignment< H > >

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
	 * ordered ascending by right (upper) interval end (JF: check if still the
	 * case!).
	 *
	 * @param ctNode
	 *            a node in a <code>ComponentTree</code>.
	 * @param t
	 *            the time-index the ctNode comes from.
	 */
	public void recursivelyAddCTNsAsHypotheses( final int t, final ComponentTreeNode< DoubleType, ? > ctNode ) {
		// do the same for all children
		for ( final ComponentTreeNode< DoubleType, ? > ctChild : ctNode.getChildren() ) {
			recursivelyAddCTNsAsHypotheses( t, ctChild );
		}
		// add the current ctNode as Hypothesis (including corresponding costs)
		final Pair< Integer, Integer > segInterval = ComponentTreeUtils.getTreeNodeInterval( ctNode );
		final int a = segInterval.a.intValue();
		final int b = segInterval.b.intValue();
		// TODO Here I have to still do some redesign... it is unacceptable the way it is right now...
		final double[] gapSepFkt = gl.getFrames().get( t ).getGapSeparationValues( null );
		final double max = SimpleFunctionAnalysis.getMax( gapSepFkt, a, b ).b.doubleValue();
		final double sum = SimpleFunctionAnalysis.getSum( gapSepFkt, a, b );

		final double cost = -1 * 0.5 * ( ( b - a ) * max - sum );
		nodes.addHypothesis( t, new Hypothesis< ComponentTreeNode< DoubleType, ? > >( ctNode, cost ) );
	}

	/**
	 * Add an exit-assignment at time t to a bunch of segmentation hypotheses.
	 *
	 * @param t
	 *            the time-point.
	 * @param hyps
	 *            a list of hypothesis for which an <code>ExitAssignment</code>
	 *            should be added.
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
	 * Add a mapping-assignment to a bunch of segmentation hypotheses.
	 *
	 * @param t
	 *            the time-point from which the <code>curHyps</code> originate.
	 * @param curHyps
	 *            a list of hypothesis for which a
	 *            <code>MappingAssignment</code> should be added.
	 * @param nxtHyps
	 *            a list of hypothesis at the next time-point at which the newly
	 *            added <code>MappingAssignments</code> should end at.
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
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
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
	 * Add a division-assignment to a bunch of segmentation hypotheses. Note
	 * that this function also looks for suitable pairs of hypothesis in
	 * nxtHyps, since division-assignments naturally need two right-neighbors.
	 *
	 * @param t
	 *            the time-point from which the <code>curHyps</code> originate.
	 * @param curHyps
	 *            a list of hypothesis for which a
	 *            <code>DivisionAssignment</code> should be added.
	 * @param nxtHyps
	 *            a list of hypothesis at the next time-point at which the newly
	 *            added <code>DivisionAssignments</code> should end at.
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
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the upper (left) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @param lowerNeighbor
	 *            the lower (right) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
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
	 * This function traverses all time points of the growth-line
	 * <code>gl</code>, retrieves the full component tree that has to be built
	 * beforehand, and calls the private method
	 * <code>recursivelyAddPathBlockingConstraints</code> on all those root
	 * nodes. This function adds one constraint for each path starting at a leaf
	 * node in the tree up to the root node itself.
	 * Those path-blocking constraints ensure, that only 0 or 1 of the
	 * segmentation hypothesis along such a path can be chosen during the convex
	 * optimization.
	 *
	 * @throws GRBException
	 *
	 */
	public void addPathBlockingConstraint() throws GRBException {
		// For each time-point
		for ( int t = 0; t < gl.size(); t++ ) {
			// Get the full component tree
			final ComponentTree< DoubleType, ? > ct = gl.get( t ).getComponentTree();
			for ( final ComponentTreeNode< DoubleType, ? > ctRoot : ct.roots() ) {
				// And call the function adding all the path-blocking-constraints...
				recursivelyAddPathBlockingConstraints( ctRoot );
			}
		}
	}

	/**
	 * Generates path-blocking constraints for each path from the given
	 * <code>ctNode</code> to a leaf in the tree.
	 * Those path-blocking constraints ensure, that only 0 or 1 of the
	 * segmentation hypothesis along such a path can be chosen during the convex
	 * optimization.
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
	 * This function generated and adds the explanation-continuity-constraints
	 * to the ILP model.
	 * Those constraints ensure that for each segmentation hypotheses at all
	 * time-points t we have the same number of active incoming and active
	 * outgoing edges from/to assignments.
	 * Intuitively speaking this means that each hypothesis that is chosen by an
	 * assignment coming from t-1 we need to continue its interpretation by
	 * finding an active assignment towards t+1.
	 */
	public void addExplainationContinuityConstraints() throws GRBException {
		int eccId = 0;

		// For each time-point
		for ( int t = 1; t < gl.size() - 1; t++ ) { // !!! sparing out the border !!!
			final GRBLinExpr expr = new GRBLinExpr();

			for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : nodes.getHypothesesAt( t ) ) {
				if ( edgeSets.getLeftNeighborhood( hyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a_j : edgeSets.getLeftNeighborhood( hyp ) ) {
						expr.addTerm( 1.0, a_j.getGRBVar() );
					}
				}
				if ( edgeSets.getRightNeighborhood( hyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> a_j : edgeSets.getRightNeighborhood( hyp ) ) {
						expr.addTerm( -1.0, a_j.getGRBVar() );
					}
				}
			}

			model.addConstr( expr, GRB.EQUAL, 0.0, "ecc_" + eccId );
			eccId++;
		}
	}

	/**
	 * This function takes the ILP (hopefully) built up in <code>model</code>
	 * and starts the convex optimization procedure. This is actually the step
	 * that will find the MAP in the given model and hence the solution to our
	 * segmentation and tracking problem.
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
	 * Returns the optimal segmentation at time t, given by a list of non
	 * conflicting component-tree-nodes.
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time-point at which to look for the optimal segmentation.
	 * @return a list of <code>ComponentTreeNodes</code> that correspond to the
	 *         active segmentation hypothesis (chosen by the optimization
	 *         procedure).
	 */
	public List< ComponentTreeNode< DoubleType, ? >> getOptimalSegmentation( final int t ) {
		final ArrayList< ComponentTreeNode< DoubleType, ? >> ret = new ArrayList< ComponentTreeNode< DoubleType, ? >>();

		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps = getOptimalHypotheses( t );
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> h : hyps ) {
			ret.add( h.getWrappedHypothesis() );
		}

		return ret;
	}

	/**
	 * Returns the optimal segmentation at time t, given by a list of non
	 * conflicting segmentation hypothesis.
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time-point at which to look for the optimal segmentation.
	 * @return a list of
	 *         <code>Hypothesis< ComponentTreeNode< DoubleType, ? > ></code>
	 *         that correspond to the active segmentation hypothesis (chosen by
	 *         the optimization procedure).
	 */
	public List< Hypothesis< ComponentTreeNode< DoubleType, ? > > > getOptimalHypotheses( final int t ) {
		final ArrayList< Hypothesis< ComponentTreeNode< DoubleType, ? > > > ret =
				new ArrayList< Hypothesis< ComponentTreeNode< DoubleType, ? > > >();

		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : hyps ) {
			Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > nh;
			if ( t > 0 ) {
				nh = edgeSets.getLeftNeighborhood( hyp );
			} else {
				nh = edgeSets.getRightNeighborhood( hyp );
			}

			try {
				final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> aa = findActiveAssignment( nh );
				if ( aa != null ) {
					ret.add( hyp );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "It could not be determined of a certain assignment was choosen during the convex optimization!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal left (to t-1) assignments at time-point t.
	 * For each segmentation hypothesis at t we collect all active assignments
	 * coming in from the left (from t-1).
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time at which to look for active left-assignments.
	 *            Values for t make only sense if <code>>=1</code> and
	 *            <code>< nodes.getNumberOfTimeSteps().</code>
	 * @return a hash-map that maps from segmentation hypothesis to assignments
	 *         that (i) are active, and (i) come in from the left (from t-1).
	 *         Note that segmentation hypothesis that are not active will NOT be
	 *         included in the hash-map.
	 */
	public HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > getOptimalLeftAssignments( final int t ) {
		assert ( t >= 1 );
		assert ( t < nodes.getNumberOfTimeSteps() );

		final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
					   AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > ret =
				new HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
						     AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > >();

		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : hyps ) {
			try {
				final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> ola = getOptimalLeftAssignment( hyp );
				if ( ola != null ) {
					ret.put( hyp, ola );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "An optimal left assignment could not be determined!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal left (to t-1) assignment given a
	 * segmentation hypothesis.
	 * For each segmentation hypothesis we know a set of outgoing edges
	 * (assignments) that describe the interpretation (fate) of this segmented
	 * cell. The ILP is set up such that only 1 such assignment can be chosen by
	 * the convex optimizer during the computation of the optimal MAP
	 * assignment.
	 *
	 * @return the optimal (choosen by the convex optimizer) assignment
	 *         describing the most likely data interpretation (MAP) towards the
	 *         previous time-point.
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > getOptimalLeftAssignment( final Hypothesis< ComponentTreeNode< DoubleType, ? > > hypothesis ) throws GRBException {
		return findActiveAssignment( edgeSets.getLeftNeighborhood( hypothesis ) );
	}

	/**
	 * Finds and returns the optimal right (to t+1) assignments at time-point t.
	 * For each segmentation hypothesis at t we collect all active assignments
	 * going towards the right (to t+1).
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time at which to look for active right-assignments.
	 *            Values for t make only sense if <code>>=0</code> and
	 *            <code>< nodes.getNumberOfTimeSteps() - 1.</code>
	 * @return a hash-map that maps from segmentation hypothesis to assignments
	 *         that (i) are active, and (i) go towards the right (to t+1).
	 *         Note that segmentation hypothesis that are not active will NOT be
	 *         included in the hash-map.
	 */
	public HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > getOptimalRightAssignments( final int t ) {
		assert ( t >= 0 );
		assert ( t < nodes.getNumberOfTimeSteps() - 1 );

		final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
					   AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > ret =
				new HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? > >,
							 AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > >();

		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp : hyps ) {
			try {
				final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> ora = getOptimalRightAssignment( hyp );
				if ( ora != null ) {
					ret.put( hyp, ora );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "An optimal right assignment could not be determined!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal right (to t+1) assignment given a
	 * segmentation hypothesis.
	 * For each segmentation hypothesis we know a set of outgoing edges
	 * (assignments) that describe the interpretation (fate) of this segmented
	 * cell. The ILP is set up such that only 1 such assignment can be chosen by
	 * the convex optimizer during the computation of the optimal MAP
	 * assignment.
	 *
	 * @return the optimal (choosen by the convex optimizer) assignment
	 *         describing the most likely data interpretation (MAP) towards the
	 *         next time-point.
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > getOptimalRightAssignment( final Hypothesis< ComponentTreeNode< DoubleType, ? > > hypothesis ) throws GRBException {
		return findActiveAssignment( edgeSets.getRightNeighborhood( hypothesis ) );
	}

	/**
	 * Finds the active assignment in a set of assignments.
	 * This method is thought to be called given a set that can only contain at
	 * max 1 active assignment. (It will always and exclusively return the first
	 * active assignment in the iteration order of the given set!)
	 *
	 * @return the one (first) active assignment in the given set of
	 *         assignments. (An assignment is active iff the binary ILP variable
	 *         associated with the assignment was set to 1 by the convex
	 *         optimizer!)
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > findActiveAssignment( final Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > set ) throws GRBException {
		if ( set == null ) return null;

		for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > a : set ) {
			if ( a.isChoosen() ) { return a; }
		}
		return null;
	}

}
/**
 *
 */
package com.jug;

import gurobi.GRBException;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.lp.AbstractAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;

/**
 * @author jug
 */
public class GrowthLine {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final List< GrowthLineFrame > frames;
	private GrowthLineTrackingILP ilp; //<
			// Hypothesis< ComponentTreeNode< DoubleType, ? > >,
			// AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? > > > > ilp;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the frames
	 */
	public List< GrowthLineFrame > getFrames() {
		return frames;
	}

	/**
	 * @return the ILP
	 */
	public GrowthLineTrackingILP getIlp() {
		return ilp;
	}

	// -------------------------------------------------------------------------------------
	// constructors
	// -------------------------------------------------------------------------------------
	public GrowthLine() {
		this.frames = new ArrayList< GrowthLineFrame >();
	}

	public GrowthLine( final List< GrowthLineFrame > frames ) {
		this.frames = frames;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * @return the number of frames (time-steps) in this <code>GrowthLine</code>
	 */
	public int size() {
		return frames.size();
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be appended as last frame
	 * @return true, if add was successful.
	 */
	public boolean add( final GrowthLineFrame frame ) {
		frame.setParent( this );
		return frames.add( frame );
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be prepended as first frame
	 * @return true, if add was successful.
	 */
	public void prepand( final GrowthLineFrame frame ) {
		frame.setParent( this );
		frames.add( 0, frame );
	}

	/**
	 * @param f
	 * @return
	 */
	public GrowthLineFrame get( final int i ) {
		return this.getFrames().get( i );
	}

	/**
	 * Builds up the ILP used to find the MAP-mapping.
	 */
	public void generateILP() {
		try {

			ilp = new GrowthLineTrackingILP( this );

			// Feed all segmentation-hypotheses as nodes to the ILP
			for ( int t = 0; t < this.size(); t++ ) {
				final GrowthLineFrame glf = this.getFrames().get( t );

				for ( final ComponentTreeNode< DoubleType, ? > ctRoot : glf.getComponentTree().roots() ) {
					getIlp().recursivelyAddCTNsAsHypotheses( t, ctRoot );
				}
			}

			// for all hypothesis from pairs of neighboring time-points ADD all
			// ASSIGNMENTS to ILP
			// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
			for ( int t = 0; t < getIlp().nodes.getAllHypotheses().size() - 1; t++ ) {
				final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> curHyps = getIlp().nodes.getHypothesesAt( t );
				final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> nxtHyps = getIlp().nodes.getHypothesesAt( t + 1 );

				// add EXIT assignments to time-point t
				getIlp().addExitAssignments( t, curHyps );

				// add MAPPING assignments to (t, t+1)
				getIlp().addMappingAssignments( t, curHyps, nxtHyps );

				// DIVISION assignments to (t, t+1)
				getIlp().addDivisionAssignments( t, curHyps, nxtHyps );
			}

			// UPDATE GUROBI-MODEL
			// - - - - - - - - - -
			getIlp().model.update();

			// Iterate over all assignments and ask them to add their
			// constraints to the model
			// - - - - - - - - - - - - - - - - - - - - - - - - - - - -
			for ( final List< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > innerList : getIlp().nodes.getAllAssignments() ) {
				for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> assignment : innerList ) {
					assignment.addConstraintsToLP();
				}
			}

			// Add the remaining ILP constraints
			// (those would be (i) and (ii) of 'Default Solution')
			// - - - - - - - - - - - - - - - - - - - - - - - - - -
			getIlp().addPathBlockingConstraint();
			getIlp().addExplainationContinuityConstraints();

			// UPDATE GUROBI-MODEL
			// - - - - - - - - - -
			getIlp().model.update();

		}
		catch ( final GRBException e ) {
			System.out.println( "Could not fill data into GrowthLineTrackingILP!" );
			e.printStackTrace();
		}
	}

	/**
	 * Runs the ILP.
	 */
	public void runILP() {
		getIlp().run();
	}

}

/**
 *
 */
package com.jug;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jug
 */
public class GrowthLine {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final List< GrowthLineFrame > frames;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the frames
	 */
	public List< GrowthLineFrame > getFrames() {
		return frames;
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
	 * @return the number of frames in this <code>GrowthLineMovie</code>
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
	 *
	 */
	public void generateSegmentationHypotheses() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param f
	 * @return
	 */
	public GrowthLineFrame get( final int i ) {
		return this.getFrames().get( i );
	}
}

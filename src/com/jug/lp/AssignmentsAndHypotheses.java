/**
 *
 */
package com.jug.lp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author jug
 */
public class AssignmentsAndHypotheses< A extends AbstractAssignment< H >, H extends Hypothesis< ? > > {

	List< List< A > > a_t;

	List< List< H > > h_t;

	Map< Object, H > hmap;

	public AssignmentsAndHypotheses() {
		a_t = new ArrayList< List< A > >();
		h_t = new ArrayList< List< H > >();
		hmap = new HashMap< Object, H >();
	}

	public void addTimeStep () {
		a_t.add( new ArrayList< A >() );
		h_t.add( new ArrayList< H >() );
	}


	public boolean addAssignment( final int t, final A a ) {
		while ( t >= a_t.size() ) {
			addTimeStep();
		}
		return a_t.get( t ).add( a );
	}

	public List< List< A > > getAllAssignments() {
		return a_t;
	}

	public List< A > getAssignmentsAt( final int t ) {
		return a_t.get( t );
	}


	public boolean addHypothesis( final int t, final H h ) {
		while ( t >= h_t.size() ) {
			addTimeStep();
		}
		if ( h_t.get( t ).add( h ) ) {
			hmap.put( h.getWrappedHypothesis(), h );
			return true;
		}
		return false;
	}

	public List< List< H > > getAllHypotheses() {
		return h_t;
	}

	public List< H > getHypothesesAt( final int t ) {
		return h_t.get( t );
	}

	/**
	 * Finds an Hypothesis that wraps the given Object.
	 *
	 * @param ctn
	 * @return the Hypothesis wrapping the given Object, or null in case this
	 *         object was not wrapped by any Hypothesis.
	 */
	public Hypothesis< ? > findHypothesisContaining( final Object ctn ) {
		return hmap.get( ctn );
	}
}

/**
 *
 */
package com.jug.lp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * The main purpose of this class is to manage and update the assignment
 * neighborhoods $A_{>>b_i^t}$ and $A_{b_i^t>>}$.
 *
 * @author jug
 */
public class HypothesisNeighborhoods< H extends Hypothesis< ? >, A extends AbstractAssignment< H > > {

	private final HashMap< H, Set< A > > rightNeighborhoods;

	private final HashMap< H, Set< A > > leftNeighborhoods;

	public HypothesisNeighborhoods() {
		rightNeighborhoods = new HashMap< H, Set< A > >();
		leftNeighborhoods = new HashMap< H, Set< A > >();
	}

	/**
	 * @return the rightNeighborhoods
	 */
	public HashMap< H, Set< A > > getRightNeighborhoods() {
		return rightNeighborhoods;
	}

	// /**
	// * @param rightNeighborhoods
	// * the rightNeighborhoods to set
	// */
	// public void setRightNeighborhoods( final HashMap< H, Set< A > >
	// rightNeighborhoods ) {
	// this.rightNeighborhoods = rightNeighborhoods;
	// }

	/**
	 * @return the leftNeighborhoods
	 */
	public HashMap< H, Set< A > > getLeftNeighborhoods() {
		return leftNeighborhoods;
	}

	// /**
	// * @param leftNeighborhoods
	// * the leftNeighborhoods to set
	// */
	// public void setLeftNeighborhoods( final HashMap< H, Set< A > >
	// leftNeighborhoods ) {
	// this.leftNeighborhoods = leftNeighborhoods;
	// }

	public boolean hasLeftNeighborhoods( final H h ) {
		return getLeftNeighborhood( h ) != null;
	}

	public boolean hasRightNeighborhoods( final H h ) {
		return getRightNeighborhood( h ) != null;
	}

	public boolean hasNeighborhoods( final H h ) {
		return hasLeftNeighborhoods( h ) && hasRightNeighborhoods( h );
	}

	public Set< A > getLeftNeighborhood( final H h ) {
		return leftNeighborhoods.get( h );
	}

	public Set< A > getRightNeighborhood( final H h ) {
		return rightNeighborhoods.get( h );
	}

	public boolean addToLeftNeighborhood ( final H h, final A a ) {
		if ( ! hasLeftNeighborhoods( h ) ) {
			leftNeighborhoods.put( h, new HashSet< A >() );
		}
		return getLeftNeighborhood( h ).add( a );
	}

	public boolean addToRightNeighborhood( final H h, final A a ) {
		if ( !hasRightNeighborhoods( h ) ) {
			rightNeighborhoods.put( h, new HashSet< A >() );
		}
		return getRightNeighborhood( h ).add( a );
	}

}

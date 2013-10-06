/**
 *
 */
package com.jug.lp;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.util.ComponentTreeUtils;

/**
 * @author jug
 */
public class LpUtils {

	/**
	 * Builds and returns the set Hup (given as a List). Hup is defines as the
	 * set up all Hypothesis in hyps that strictly above the Hypothesis hyp (in
	 * image space).
	 *
	 * @param hyp
	 *            the reference hypothesis.
	 * @param hyps
	 *            the set of all hypothesis of interest.
	 * @return a List of all Hypothesis from hyps that lie above the
	 *         segmentation hypothesis hyp.
	 */
	public static List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> getHup( final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp, final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> hyps ) {
		final List< Hypothesis< ComponentTreeNode< DoubleType, ? >>> Hup = new ArrayList< Hypothesis< ComponentTreeNode< DoubleType, ? >>>();
		for ( final Hypothesis< ComponentTreeNode< DoubleType, ? >> candidate : hyps ) {
			if ( ComponentTreeUtils.isAbove( candidate.getWrappedHypothesis(), hyp.getWrappedHypothesis() ) ) {
				Hup.add( candidate );
			}
		}
		return Hup;
	}
}

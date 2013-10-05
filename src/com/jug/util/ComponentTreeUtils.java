/**
 *
 */
package com.jug.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Localizable;
import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 *
 */
public class ComponentTreeUtils {

	/**
	 * @param tree
	 * @return
	 */
	public static List< ComponentTreeNode< DoubleType, ? >> getListOfLeavesInOrder( final ComponentTree< DoubleType, ? > tree ) {
		final List< ComponentTreeNode< DoubleType, ? > > leaves = new ArrayList< ComponentTreeNode< DoubleType, ? > >();

		for ( final ComponentTreeNode< DoubleType, ? > root : tree.roots() ) {
			recursivelyAddLeaves( root, leaves );
		}

		return leaves;
	}

	/**
	 * @param root
	 * @param leaves
	 */
	private static void recursivelyAddLeaves( final ComponentTreeNode< DoubleType, ? > node, final List< ComponentTreeNode< DoubleType, ? >> leaves ) {
		if ( node.getChildren().size() == 0 ) {
			leaves.add( node );
		} else {
			for ( final ComponentTreeNode< DoubleType, ? > child : node.getChildren() ) {
				recursivelyAddLeaves( child, leaves );
			}
		}
	}

	/**
	 * @param candidate
	 * @param hyp
	 * @return
	 */
	public static boolean isAbove( final ComponentTreeNode< DoubleType, ? > candidate, final ComponentTreeNode< DoubleType, ? > reference ) {
		final Pair< Integer, Integer > candMinMax = getTreeNodeInterval( candidate );
		final Pair< Integer, Integer > refMinMax = getTreeNodeInterval( reference );
		return candMinMax.getB().intValue() < refMinMax.getA().intValue();
	}

	/**
	 * @param candidate
	 * @param hyp
	 * @return
	 */
	public static boolean isBelow( final ComponentTreeNode< DoubleType, ? > candidate, final ComponentTreeNode< DoubleType, ? > reference ) {
		final Pair< Integer, Integer > candMinMax = getTreeNodeInterval( candidate );
		final Pair< Integer, Integer > refMinMax = getTreeNodeInterval( reference );
		return candMinMax.getA().intValue() > refMinMax.getB().intValue();
	}

	/**
	 * Returns the smallest and largest value on the x-axis that is spanned by
	 * this component-tree-node.
	 * Note that this function really only makes sense if the comp.-tree was
	 * built on a one-dimensional image (as it is the case for my current
	 * MotherMachine stuff...)
	 *
	 * @param node
	 *            the node in question.
	 * @return a <code>Pair</code> or two <code>Integers</code> giving the
	 *         leftmost and rightmost point on the x-axis that is covered by
	 *         this component-tree-node respectively.
	 */
	public static Pair< Integer, Integer > getTreeNodeInterval( final ComponentTreeNode< DoubleType, ? > node ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final Iterator< Localizable > componentIterator = node.iterator();
		while ( componentIterator.hasNext() ) {
			final int pos = componentIterator.next().getIntPosition( 0 );
			min = Math.min( min, pos );
			max = Math.max( max, pos );
		}
		return new ValuePair< Integer, Integer >( new Integer( min ), new Integer( max ) );
	}

	// public static double[] getFunctionValues( final ComponentTreeNode<
	// DoubleType, ? > node ) {
	// Pair< Integer, Integer > interval = getTreeNodeInterval( node );
	//
	// double[] ret = new double[ interval.b.intValue() - interval.a.intValue()
	// ];
	//
	// final Iterator< Localizable > componentIterator = node.iterator();
	// while ( componentIterator.hasNext() ) {
	// final int pos = componentIterator.next().getIntPosition( 0 ) -
	// interval.a.intValue();
	// ret[pos] = componentIterator.
	// }
	// return
	// }

	/**
	 * @param to
	 * @return
	 */
	public static List< ComponentTreeNode< DoubleType, ? >> getRightNeighbors( final ComponentTreeNode< DoubleType, ? > node ) {
		final ArrayList< ComponentTreeNode< DoubleType, ? >> ret = new ArrayList< ComponentTreeNode< DoubleType, ? >>();

		ComponentTreeNode< DoubleType, ? > rightNeighbor = getRightNeighbor( node );
		if ( rightNeighbor != null ) {
			ret.add( rightNeighbor );
			while ( rightNeighbor.getChildren().size() > 0 ) {
				rightNeighbor = rightNeighbor.getChildren().get( 0 );
				ret.add( rightNeighbor );
			}
		}

		return ret;
	}

	/**
	 * @param node
	 * @return
	 */
	private static ComponentTreeNode< DoubleType, ? > getRightNeighbor( final ComponentTreeNode< DoubleType, ? > node ) {
		// TODO Note that we do not find the right neighbor in case the
		// component tree has several roots and the
		// right neighbor is somewhere down another root.
		final ComponentTreeNode< DoubleType, ? > father = node.getParent();

		if ( father != null ) {
			final int idx = father.getChildren().indexOf( node );
			if ( idx + 1 < father.getChildren().size() ) {
				return father.getChildren().get( idx + 1 );
			} else {
				return getRightNeighbor( father );
			}
		}
		return null;
	}

	/**
	 * @param ct
	 * @return
	 */
	public static int countNodes( final ComponentTree< DoubleType, ? > ct ) {
		int nodeCount = ct.roots().size();;
		for ( final ComponentTreeNode< DoubleType, ? > root : ct.roots() ) {
			nodeCount += countNodes( root );
		}
		return nodeCount;
	}

	/**
	 * @param root
	 * @return
	 */
	public static int countNodes( final ComponentTreeNode< DoubleType, ? > ctn ) {
		int nodeCount = ctn.getChildren().size();
		for ( final ComponentTreeNode< DoubleType, ? > child : ctn.getChildren() ) {
			nodeCount += countNodes( child );
		}
		return nodeCount;
	}

	/**
	 * @param ct
	 * @return
	 */
	public static List< ComponentTreeNode< DoubleType, ? >> getListOfNodes( final ComponentTree< DoubleType, ? > ct ) {
		final ArrayList< ComponentTreeNode< DoubleType, ? >> ret = new ArrayList< ComponentTreeNode< DoubleType, ? >>();
		for ( final ComponentTreeNode< DoubleType, ? > root : ct.roots() ) {
			ret.add( root );
			addListOfNodes( root, ret );
		}
		return ret;
	}

	/**
	 * @param root
	 * @param list
	 */
	private static void addListOfNodes( final ComponentTreeNode< DoubleType, ? > ctn, final ArrayList< ComponentTreeNode< DoubleType, ? >> list ) {
		for ( final ComponentTreeNode< DoubleType, ? > child : ctn.getChildren() ) {
			list.add( child );
			addListOfNodes( child, list );
		}
	}

	/**
	 * @param ctnLevel
	 * @return
	 */
	public static ArrayList< ComponentTreeNode< DoubleType, ? >> getAllChildren( final ArrayList< ComponentTreeNode< DoubleType, ? >> ctnLevel ) {
		final ArrayList< ComponentTreeNode< DoubleType, ? >> nextCtnLevel = new ArrayList< ComponentTreeNode< DoubleType, ? >>();
		for ( final ComponentTreeNode< DoubleType, ? > ctn : ctnLevel ) {
			for ( final ComponentTreeNode< DoubleType, ? > child : ctn.getChildren() ) {
				nextCtnLevel.add( child );
			}
		}
		return nextCtnLevel;
	}

	/**
	 * @param ctn
	 * @return
	 */
	public static int getLevelInTree( final ComponentTreeNode< DoubleType, ? > ctn ) {
		int level = 0;
		ComponentTreeNode< DoubleType, ? > runner = ctn;
		while ( runner.getParent() != null ) {
			level++;
			runner = runner.getParent();
		}
		return level;
	}
}

/**
 *
 */
package com.jug.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.HashMap;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

import com.jug.lp.AbstractAssignment;
import com.jug.lp.DivisionAssignment;
import com.jug.lp.ExitAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.lp.MappingAssignment;
import com.jug.util.ComponentTreeUtils;


/**
 * @author jug
 */
public class AssignmentView extends Component {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	private static final long serialVersionUID = -2920396224787446598L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final int width;
	private final int height;

	private boolean doFilterData = false;
	private int filter;

	private HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > data;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public AssignmentView( final int height ) {
		this.width = 150;
		this.height = height;
		this.setPreferredSize( new Dimension( width, height - 40 ) );
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Turns of filtering and shows all the given data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > data ) {
		doFilterData = false;
		this.data = data;

		this.repaint();
	}

	/**
	 * Turns of filtering and shows only the filtered data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 * @param filter
	 *            must be one of the values
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_MAPPING</code>,
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_DIVISION</code>, or
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_EXIT</code>.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > data, final int filter ) {
		assert ( filter == GrowthLineTrackingILP.ASSIGNMENT_EXIT ||
				 filter == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ||
				 filter == GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		doFilterData = true;
		this.filter = filter;
		this.data = data;

		this.repaint();
	}

	/**
	 * In this overwritten method we added filtering and calling
	 * <code>drawAssignment(...)</code>.
	 *
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paint( final Graphics g ) {
		if ( data == null ) return;

		for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> assignment : data.values() ) {
			if ( doFilterData && assignment.getType() != filter ) continue;
			drawAssignment( g, assignment );
		}
	}

	/**
	 * Checks the type of assignment we have and call the corresponding drawing
	 * method.
	 *
	 * @param g
	 * @param assignment
	 */
	private void drawAssignment( final Graphics g, final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> assignment ) {
		final int type = assignment.getType();

		final Graphics2D g2 = ( Graphics2D ) g;
		final Dimension size = getSize();

		if ( type == GrowthLineTrackingILP.ASSIGNMENT_EXIT ) {
			drawExitAssignment( g, g2, ( ExitAssignment ) assignment, size );
		} else if ( type == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ) {
			drawMappingAssignment( g, g2, ( MappingAssignment ) assignment, size );
		} else if ( type == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
			drawDivisionAssignment( g, g2, ( DivisionAssignment ) assignment, size );
		}
	}

	/**
	 * This methods draws the given mapping-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param ma
	 *            a mapping-assignment that should be visualized.
	 * @param size
	 */
	private void drawMappingAssignment( final Graphics g, final Graphics2D g2, final MappingAssignment ma, final Dimension size ) {
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> leftHyp = ma.getSourceHypothesis();
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> rightHyp = ma.getDestinationHypothesis();

		final Pair< Integer, Integer > limitsLeft = ComponentTreeUtils.getTreeNodeInterval( leftHyp.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRight = ComponentTreeUtils.getTreeNodeInterval( rightHyp.getWrappedHypothesis() );

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( 0, limitsLeft.a.intValue() );
		polygon.lineTo( 0, limitsLeft.b.intValue() );
		polygon.lineTo( this.width, limitsRight.b.intValue() );
		polygon.lineTo( this.width, limitsRight.a.intValue() );
		polygon.closePath();

		g2.setPaint( Color.BLUE );
		g2.draw( polygon );
		g2.setPaint( Color.BLUE.brighter().brighter() );
		g2.fill( polygon );
//		System.out.println( "just drew a mapping!" );
	}

	/**
	 * This methods draws the given division-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param da
	 *            a division-assignment that should be visualized.
	 * @param size
	 */
	private void drawDivisionAssignment( final Graphics g, final Graphics2D g2, final DivisionAssignment da, final Dimension size ) {
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> leftHyp = da.getSourceHypothesis();
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> rightHypUpper = da.getUpperDesinationHypothesis();
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> rightHypLower = da.getLowerDesinationHypothesis();

		final Pair< Integer, Integer > limitsLeft = ComponentTreeUtils.getTreeNodeInterval( leftHyp.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRightUpper = ComponentTreeUtils.getTreeNodeInterval( rightHypUpper.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRightLower = ComponentTreeUtils.getTreeNodeInterval( rightHypLower.getWrappedHypothesis() );

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( 0, limitsLeft.a.intValue() );
		polygon.lineTo( 0, limitsLeft.b.intValue() );
		polygon.lineTo( this.width, limitsRightLower.b.intValue() );
		polygon.lineTo( this.width, limitsRightLower.a.intValue() );
		polygon.lineTo( 0, ( limitsLeft.b.intValue() - limitsLeft.a.intValue() ) / 2 );
		polygon.lineTo( this.width, limitsRightUpper.b.intValue() );
		polygon.lineTo( this.width, limitsRightUpper.a.intValue() );
		polygon.closePath();

		g2.setPaint( Color.GREEN );
		g2.draw( polygon );
		g2.setPaint( Color.GREEN.brighter().brighter() );
		g2.fill( polygon );
//		System.out.println( "just drew a mapping!" );
	}

	/**
	 * This methods draws the given exit-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param ea
	 *            a exit-assignment that should be visualized.
	 * @param size
	 */
	private void drawExitAssignment( final Graphics g, final Graphics2D g2, final ExitAssignment ea, final Dimension size ) {
		final Hypothesis< ComponentTreeNode< DoubleType, ? >> hyp = ea.getAssociatedHypothesis();
		final Pair< Integer, Integer > limits = ComponentTreeUtils.getTreeNodeInterval( hyp.getWrappedHypothesis() );

		g2.setPaint( Color.RED );
		g2.drawRect( 0, limits.a.intValue(), this.getWidth() / 5, limits.b.intValue() );
		g2.setPaint( Color.RED.brighter().brighter() );
		g2.fillRect( 0, limits.a.intValue(), this.getWidth() / 5, limits.b.intValue() );
//		System.out.println( "just drew a term!" );
	}

	/**
	 * Sets new data without modifying the filter setting.
	 *
	 * @param data
	 */
	public void setData( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > data ) {
		this.data = data;
		this.repaint();
	}
}

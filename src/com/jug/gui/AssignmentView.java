/**
 *
 */
package com.jug.gui;

import gurobi.GRBException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

import com.jug.MotherMachine;
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
public class AssignmentView extends JComponent implements MouseInputListener {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	private static final long serialVersionUID = -2920396224787446598L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final int width;
	private final int height;

	private final int offsetY;

	private boolean doFilterDataByType = false;
	private int filterAssignmentType;

	private boolean doFilterDataByCost = false;
	private double filterMinCost = -100.0;
	private double filterMaxCost = 100.0;

	private HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data;

	private boolean isMouseOver = false;
	private int mousePosX;
	private int mousePosY;
	private int currentCostLine;

	private boolean isDragging = false;
	private int dragX;
	private int dragY;
	private double dragStepWeight = 0;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public AssignmentView( final int height ) {
		this( height, -GrowthLineTrackingILP.CUTOFF_COST, GrowthLineTrackingILP.CUTOFF_COST );
		this.doFilterDataByCost = false;
	}

	/**
	 * @param height
	 * @param filterMinCost
	 * @param filterMaxCost
	 */
	public AssignmentView( final int height, final double filterMinCost, final double filterMaxCost ) {
		this.offsetY = MotherMachine.GL_OFFSET_TOP;
		this.width = 90;
		this.height = height;
		this.setPreferredSize( new Dimension( width, height ) );
		this.addMouseListener( this );
		this.addMouseMotionListener( this );
		this.doFilterDataByCost = true;
		this.filterMinCost = filterMinCost;
		this.filterMaxCost = filterMaxCost;
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
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data ) {
		doFilterDataByType = false;
		doFilterDataByCost = false;
		this.data = data;

		this.repaint();
	}

	/**
	 * Turns of filtering by type, turns on filtering by cost, and shows all the
	 * given data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data, final double minCostToShow, final double maxCostToShow ) {
		doFilterDataByType = false;
		this.data = data;

		doFilterDataByCost = true;
		this.filterMinCost = minCostToShow;
		this.filterMaxCost = maxCostToShow;

		this.repaint();
	}

	/**
	 * Turns on filtering by type and shows only the filtered data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 * @param typeToFilter
	 *            must be one of the values
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_MAPPING</code>,
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_DIVISION</code>, or
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_EXIT</code>.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data, final int typeToFilter ) {
		assert ( typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_EXIT ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		this.display( data, typeToFilter, Double.MIN_VALUE, Double.MAX_VALUE );
	}

	/**
	 * Turns on filtering by type and by cost and shows only the filtered data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 * @param typeToFilter
	 *            must be one of the values
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_MAPPING</code>,
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_DIVISION</code>, or
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_EXIT</code>.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data, final int typeToFilter, final double minCostToShow, final double maxCostToShow ) {
		assert ( typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_EXIT ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		doFilterDataByType = true;
		this.filterAssignmentType = typeToFilter;

		doFilterDataByCost = true;
		this.filterMinCost = minCostToShow;
		this.filterMaxCost = maxCostToShow;
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

		this.currentCostLine = 0;
		for ( final Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> > setOfAssignments : data.values() ) {
			for ( final AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> assignment : setOfAssignments ) {
				if ( doFilterDataByType && assignment.getType() != filterAssignmentType ) {
					continue;
				}
				try {
					if ( doFilterDataByCost && ( assignment.getCost() < this.filterMinCost || assignment.getCost() > this.filterMaxCost ) ) {
						continue;
					}
				}
				catch ( final GRBException e ) {
					e.printStackTrace();
				}
				drawAssignment( g, assignment );
			}
		}

		if ( this.isDragging ) {
			g.setColor( Color.GREEN.darker() );
			g.drawString( String.format( "min: %.4f", this.filterMinCost ), 0, 10 );
			g.setColor( Color.RED.darker() );
			g.drawString( String.format( "max: %.4f", this.filterMaxCost ), 0, 30 );
			g.setColor( Color.GRAY );
			g.drawString( String.format( "dlta %.4f", this.dragStepWeight ), 0, 50 );
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

		final int x1 = 0;
		final int y1 = offsetY + limitsLeft.a.intValue();
		final int x2 = 0;
		final int y2 = offsetY + limitsLeft.b.intValue();
		final int x3 = this.width;
		final int y3 = offsetY + limitsRight.b.intValue();
		final int x4 = this.width;
		final int y4 = offsetY + limitsRight.a.intValue();

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( x1, y1 );
		polygon.lineTo( x2, y2 );
		polygon.lineTo( x3, y3 );
		polygon.lineTo( x4, y4 );
		polygon.closePath();

		g2.setPaint( new Color( 25 / 256f, 65 / 256f, 165 / 256f, 0.2f ) );
		g2.fill( polygon );
		g2.setPaint( new Color( 25 / 256f, 65 / 256f, 165 / 256f, 1.0f ) );
		g2.draw( polygon );

		// System.out.println( String.format( "(%d,%d) -- (%d,%d,%d,%d)", this.mousePosX, this.mousePosY, x1, y1, x3, y3 ) );
		if ( !this.isDragging && this.isMouseOver && polygon.contains( this.mousePosX, this.mousePosY ) ) {
			try {
				final double cost = ma.getCost();
				g2.drawString( String.format( "c=%.4f", cost ), 10, this.mousePosY - 10 - this.currentCostLine * 20 );
				this.currentCostLine++;
			}
			catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}
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

		final int x1 = 0;
		final int y1 = offsetY + limitsLeft.a.intValue();
		final int x2 = 0;
		final int y2 = offsetY + limitsLeft.b.intValue();
		final int x3 = this.width;
		final int y3 = offsetY+limitsRightLower.b.intValue();
		final int x4 = this.width;
		final int y4 = offsetY + limitsRightLower.a.intValue();
		final int x5 = this.width / 3;
		final int y5 = offsetY + ( 2 * ( limitsLeft.a.intValue() + limitsLeft.b.intValue() ) / 2 +
							       1 * ( limitsRightUpper.b.intValue() + limitsRightLower.a.intValue() ) / 2 ) / 3;
		final int x6 = this.width;
		final int y6 = offsetY + limitsRightUpper.b.intValue();
		final int x7 = this.width;
		final int y7 = offsetY + limitsRightUpper.a.intValue();

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( x1, y1 );
		polygon.lineTo( x2, y2 );
		polygon.lineTo( x3, y3 );
		polygon.lineTo( x4, y4 );
		polygon.lineTo( x5, y5 );
		polygon.lineTo( x6, y6 );
		polygon.lineTo( x7, y7 );
		polygon.closePath();

		g2.setPaint( new Color( 250 / 256f, 150 / 256f, 40 / 256f, 0.2f ) );
		g2.fill( polygon );
		g2.setPaint( new Color( 250 / 256f, 150 / 256f, 40 / 256f, 1.0f ) );
		g2.draw( polygon );

		// System.out.println( String.format( "(%d,%d) -- (%d,%d,%d,%d)", this.mousePosX, this.mousePosY, x1, y1, x3, y3 ) );
		if ( !this.isDragging && this.isMouseOver && polygon.contains( this.mousePosX, this.mousePosY ) ) {
			try {
				final double cost = da.getCost();
				g2.drawString( String.format( "c=%.4f", cost ), 10, this.mousePosY - 10 - this.currentCostLine * 20 );
				this.currentCostLine++;
			}
			catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}
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

		final int x1 = 0;
		final int x2 = this.getWidth() / 5;
		final int y1 = offsetY + limits.a.intValue();
		final int y2 = y1 + limits.b.intValue() - limits.a.intValue();

		g2.setPaint( new Color( 1f, 0f, 0f, 0.2f ) );
		g2.fillRect( x1, y1, x2 - x1, y2 - y1 );
		g2.setPaint( Color.RED );
		g2.drawRect( x1, y1, x2 - x1, y2 - y1 );

		// System.out.println( String.format( "(%d,%d) -- (%d,%d,%d,%d)", this.mousePosX, this.mousePosY, x1, y1, x2, y2 ) );
		if ( !this.isDragging && this.isMouseOver && this.mousePosX > x1 && this.mousePosX < x2 && this.mousePosY > y1 && this.mousePosY < y2 ) {
			try {
				final double cost = ea.getCost();
				g2.drawString( String.format( "c=%.4f", cost ), 10, this.mousePosY - 10 - this.currentCostLine * 20 );
				this.currentCostLine++;
			}
			catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets new data without modifying the filter setting.
	 *
	 * @param data
	 */
	public void setData( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data ) {
		this.data = data;
		this.repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked( final MouseEvent e ) {
		System.out.println( "Mouse clicked..." );
	}

	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed( final MouseEvent e ) {
		if ( e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) {
			this.isDragging = true;
			this.dragX = e.getX();
			this.dragY = e.getY();
		}
		repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased( final MouseEvent e ) {
		this.isDragging = false;
		repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered( final MouseEvent e ) {
		this.isMouseOver = true;
	}

	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited( final MouseEvent e ) {
		this.isMouseOver = false;
		this.repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged( final MouseEvent e ) {
		this.doFilterDataByCost = true;

		final double minstep = 0.1;
		final double xsensitivity = 15.0;
		final int dX = e.getX() - this.dragX;
		final int dY = this.dragY - e.getY();

		final double fac = Math.pow( 2, Math.abs( ( xsensitivity + dX ) / xsensitivity ) );
		if ( dX > 0 ) {
			this.dragStepWeight = minstep * fac;
		} else {
			this.dragStepWeight = minstep / fac;
		}

		if ( e.getButton() == MouseEvent.BUTTON1 ) {
			this.filterMaxCost += dY * this.dragStepWeight;
		}
		if ( e.getButton() == MouseEvent.BUTTON3 ) {
			this.filterMinCost += dY * this.dragStepWeight;
		}

		this.dragY = e.getY();
		repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved( final MouseEvent e ) {
		this.mousePosX = e.getX();
		this.mousePosY = e.getY();
		this.repaint();
	}
}

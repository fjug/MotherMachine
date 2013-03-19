/**
 *
 */
package com.jug.gui;

import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.lp.AbstractAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;

/**
 * @author jug
 */
public class AssignmentViewer extends JTabbedPane implements ChangeListener {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	private static final long serialVersionUID = 6588846114839723373L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private AssignmentView allAssignments;
	private AssignmentView mappingAssignments;
	private AssignmentView divisionAssignments;
	private AssignmentView exitAssignments;

	private HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> data;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	/**
	 * @param dimension
	 */
	public AssignmentViewer( final int height ) {
		this.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0 ) );
		buildGui( height );
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Builds the user interface.
	 */
	private void buildGui( final int height ) {
		allAssignments = new AssignmentView( height );
		mappingAssignments = new AssignmentView( height );
		divisionAssignments = new AssignmentView( height );
		exitAssignments = new AssignmentView( height );

		allAssignments.display( data );
		mappingAssignments.display( data, GrowthLineTrackingILP.ASSIGNMENT_MAPPING, -100, 100 );
		divisionAssignments.display( data, GrowthLineTrackingILP.ASSIGNMENT_DIVISION, -100, 100 );
		exitAssignments.display( data, GrowthLineTrackingILP.ASSIGNMENT_EXIT, -100, 100 );

		this.add( "all", allAssignments );
		this.add( "mappings", mappingAssignments );
		this.add( "divisions", divisionAssignments );
		this.add( "exits", exitAssignments );
	}

	/**
	 * Receives and visualizes a new HashMap of assignments.
	 *
	 * @param hashMap
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< ComponentTreeNode< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< ComponentTreeNode< DoubleType, ? >>> >> hashMap ) {
		this.data = hashMap;
		allAssignments.setData( data );
		mappingAssignments.setData( data );
		divisionAssignments.setData( data );
		exitAssignments.setData( data );
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		if ( this.getSelectedComponent().equals( allAssignments ) ) {
			allAssignments.setData( data );
		} else if ( this.getSelectedComponent().equals( mappingAssignments ) ) {
			mappingAssignments.setData( data );
		} else if ( this.getSelectedComponent().equals( divisionAssignments ) ) {
			divisionAssignments.setData( data );
		} else if ( this.getSelectedComponent().equals( exitAssignments ) ) {
			exitAssignments.setData( data );
		}
	}

}

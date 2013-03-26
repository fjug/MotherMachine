/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.formats.gui.ExtensionFileFilter;
import net.imglib2.Localizable;
import net.imglib2.algorithm.componenttree.ComponentTree;
import net.imglib2.algorithm.componenttree.ComponentTreeNode;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.math.plot.Plot2DPanel;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.MotherMachine;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;

/**
 * @author jug
 */
public class MotherMachineGui extends JPanel implements ChangeListener, ActionListener {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	/**
	 * Parameter: how many pixels wide is the image containing the selected
	 * GrowthLine?
	 */
	private static final int GL_WIDTH_TO_SHOW = 70;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	public MotherMachineModel model;

	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (left one in active assignments view).
	 */
	IntervalView< DoubleType > viewImgLeftActive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (center one in active assignments view).
	 */
	IntervalView< DoubleType > viewImgCenterActive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (right one in active assignments view).
	 */
	IntervalView< DoubleType > viewImgRightActive;

	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (left one in inactive assignments view).
	 */
	IntervalView< DoubleType > viewImgLeftInactive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (center one in inactive assignments view).
	 */
	IntervalView< DoubleType > viewImgCenterInactive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (right one in inactive assignments view).
	 */
	IntervalView< DoubleType > viewImgRightInactive;

	// -------------------------------------------------------------------------------------
	// gui-fields
	// -------------------------------------------------------------------------------------
	private Viewer2DCanvas imgCanvasActiveLeft;
	private Viewer2DCanvas imgCanvasActiveCenter;
	private Viewer2DCanvas imgCanvasActiveRight;

	private Viewer2DCanvas imgCanvasInactiveLeft;
	private Viewer2DCanvas imgCanvasInactiveCenter;
	private Viewer2DCanvas imgCanvasInactiveRight;

	private JSlider sliderGL;
	private JSlider sliderTime;

	private JTabbedPane tabsViews;
	private CountOverviewPanel panelCountingView;
	private JPanel panelSegmentationAndAssignmentView;
	private JPanel panelDetailedDataView;
	private Plot2DPanel plot;

	private AssignmentViewer leftAssignmentViewer;
	private AssignmentViewer rightAssignmentViewer;

	private JButton btnOptimize;
	private JButton btnOptimizeAll;
	private JButton btnOptimizeRemainingAndExport;

	// -------------------------------------------------------------------------------------
	// construction & gui creation
	// -------------------------------------------------------------------------------------
	/**
	 * Construction
	 *
	 * @param mmm
	 *            the MotherMachineModel to show
	 */
	public MotherMachineGui( final MotherMachineModel mmm ) {
		super( new BorderLayout() );

		this.model = mmm;

		buildGui();
		dataToDisplayChanged();
	}

	/**
	 * Builds the GUI.
	 */
	private void buildGui() {

		final JPanel panelContent = new JPanel( new BorderLayout() );
		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;
		final JLabel labelHelper;

		// --- Slider for time and GL -------------

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, model.getCurrentGL().size() - 1, 0 );
		sliderTime.setValue( 1 );
		model.setCurrentGLF( sliderTime.getValue() );
		sliderTime.addChangeListener( this );
		sliderTime.setMajorTickSpacing( 10 );
		sliderTime.setMinorTickSpacing( 2 );
		sliderTime.setPaintTicks( true );
		sliderTime.setPaintLabels( true );
		sliderTime.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 3 ) );
		panelHorizontalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper.setBorder( BorderFactory.createEmptyBorder( 5, 10, 0, 5 ) );
		panelHorizontalHelper.add( new JLabel( " t = " ), BorderLayout.WEST );
		panelHorizontalHelper.add( sliderTime, BorderLayout.CENTER );
		panelContent.add( panelHorizontalHelper, BorderLayout.SOUTH );

		sliderGL = new JSlider( JSlider.VERTICAL, 0, model.mm.getGrowthLines().size() - 1, 0 );
		sliderGL.setValue( 0 );
		sliderGL.addChangeListener( this );
		sliderGL.setMajorTickSpacing( 5 );
		sliderGL.setMinorTickSpacing( 1 );
		sliderGL.setPaintTicks( true );
		sliderGL.setPaintLabels( true );
		sliderGL.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 3 ) );
		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelVerticalHelper.setBorder( BorderFactory.createEmptyBorder( 10, 10, 0, 5 ) );
		panelVerticalHelper.add( new JLabel( "GL#" ), BorderLayout.NORTH );
		panelVerticalHelper.add( sliderGL, BorderLayout.CENTER );
		add( panelVerticalHelper, BorderLayout.WEST );

		// --- All the TABs -------------

		tabsViews = new JTabbedPane();
		tabsViews.addChangeListener( this );

		panelCountingView = new CountOverviewPanel();
		panelSegmentationAndAssignmentView = buildSegmentationAndAssignmentView();
		panelDetailedDataView = buildDetailedDataView();

		tabsViews.add( "Cell Counting", panelCountingView );
		tabsViews.add( "Segm. & Assingments", panelSegmentationAndAssignmentView );
		tabsViews.add( "Detailed Data View", panelDetailedDataView );

		tabsViews.setSelectedComponent( panelSegmentationAndAssignmentView );

		// --- Controls ----------------------------------
		btnOptimize = new JButton( "Optimize" );
		btnOptimize.addActionListener( this );
		btnOptimizeAll = new JButton( "Optimize All" );
		btnOptimizeAll.addActionListener( this );
		btnOptimizeRemainingAndExport = new JButton( "Opt. Remaining & Export" );
		btnOptimizeRemainingAndExport.addActionListener( this );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.RIGHT, 5, 0 ) );
		panelHorizontalHelper.add( btnOptimize );
		panelHorizontalHelper.add( btnOptimizeAll );
		panelHorizontalHelper.add( btnOptimizeRemainingAndExport );
		add( panelHorizontalHelper, BorderLayout.SOUTH );

		// --- Final adding and layout steps -------------

		panelContent.add( tabsViews, BorderLayout.CENTER );
		add( panelContent, BorderLayout.CENTER );

		// - - - - - - - - - - - - - - - - - - - - - - - -
		//  KEYSTROKE SETUP (usingInput- and ActionMaps)
		// - - - - - - - - - - - - - - - - - - - - - - - -
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 't' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'g' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'a' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 's' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'd' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'o' ), "MMGUI_bindings" );

		this.getActionMap().put( "MMGUI_bindings", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				if ( e.getActionCommand().equals( "t" ) ) {
					sliderTime.requestFocus();
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "g" ) ) {
					sliderGL.requestFocus();
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "a" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelCountingView ) ) {
						tabsViews.setSelectedComponent( panelCountingView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "s" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelSegmentationAndAssignmentView ) ) {
						tabsViews.setSelectedComponent( panelSegmentationAndAssignmentView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "d" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelDetailedDataView ) ) {
						tabsViews.setSelectedComponent( panelDetailedDataView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "o" ) ) {
					btnOptimize.doClick();
				}
			}
		} );
	}

	/**
	 * @param panelCurationViewHelper
	 * @return
	 */
	private JPanel buildSegmentationAndAssignmentView() {
		final JPanel panelContent = new JPanel( new FlowLayout( FlowLayout.CENTER, 0, 10 ) );

		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;
		JLabel labelHelper;

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// --- Left data viewer (t-1) -------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t-1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveLeft = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveLeft, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.GRAY ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelContent.add( panelVerticalHelper );

		// --- Left assignment viewer (t-1 -> t) -------------
		panelVerticalHelper = new JPanel( new BorderLayout() );
		// - - - - - -
		leftAssignmentViewer = new AssignmentViewer( ( int ) model.mm.getImgRaw().dimension( 1 ), this );
		if ( ilp != null )
			leftAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( model.getCurrentTime() - 1 ) );
		panelVerticalHelper.add( leftAssignmentViewer, BorderLayout.CENTER );
		panelContent.add( panelVerticalHelper );

		// --- Center data viewer (t) -------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveCenter = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveCenter, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 3, 3, 3, 3, Color.RED ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelContent.add( panelVerticalHelper );

		// --- Right assignment viewer (t -> t+1) -------------
		panelVerticalHelper = new JPanel( new BorderLayout() );
		// - - - - - -
		rightAssignmentViewer = new AssignmentViewer( ( int ) model.mm.getImgRaw().dimension( 1 ), this );
		if ( ilp != null )
			rightAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( model.getCurrentTime() ) );
		panelVerticalHelper.add( rightAssignmentViewer, BorderLayout.CENTER );
		panelContent.add( panelVerticalHelper );

		// ---  Right data viewer (t+1) -------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t+1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveRight = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveRight, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.GRAY ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelContent.add( panelVerticalHelper );

		return panelContent;
	}

	/**
	 * @return
	 */
	private JPanel buildDetailedDataView() {
		final JPanel panelDataView = new JPanel( new BorderLayout() );

		plot = new Plot2DPanel();
		updatePlotPanels();
		plot.setPreferredSize( new Dimension( 500, 500 ) );
		panelDataView.add( plot, BorderLayout.CENTER );

		return panelDataView;
	}

	/**
	 * Removes all plots from the plot panel and adds new ones showing the data
	 * corresponding to the current slider setting.
	 */
	private void updatePlotPanels() {

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// Intensity plot
		// --------------
		plot.removeAllPlots();

		final double[] yMidline = model.getCurrentGLF().getMirroredCenterLineValues( model.mm.getImgTemp() );
		final double[] ySegmentationData = model.getCurrentGLF().getGapSeparationValues( model.mm.getImgTemp() );
		final double[] yAvg = new double[ yMidline.length ];
		final double constY = SimpleFunctionAnalysis.getSum( ySegmentationData ) / ySegmentationData.length;
		for ( int i = 0; i < yAvg.length; i++ )
			yAvg[ i ] = constY;

		plot.addLinePlot( "Midline Intensities", new Color( 127, 127, 255 ), yMidline );
		plot.addLinePlot( "Segmentation data", new Color( 80, 255, 80 ), ySegmentationData );
		plot.addLinePlot( "avg. fkt-value", new Color( 200, 64, 64 ), yAvg );

		plot.setFixedBounds( 1, 0.0, 1.0 );

		// ComponentTreeNodes
		// ------------------
		if ( ilp != null ) {
			final ComponentTree< DoubleType, ? > ct = model.getCurrentGLF().getComponentTree();

			final int numCTNs = ComponentTreeUtils.countNodes( ct );
			final double[][] xydxdyCTNBorders = new double[ numCTNs ][ 4 ];
			final int t = sliderTime.getValue();
			final double[][] xydxdyCTNBordersActive = new double[ ilp.getOptimalSegmentation( t ).size() ][ 4 ];

			int i = 0;
			for ( final ComponentTreeNode< DoubleType, ? > root : ct.roots() ) {
				System.out.println( "" );
				int level = 0;
				ArrayList< ComponentTreeNode< DoubleType, ? >> ctnLevel = new ArrayList< ComponentTreeNode< DoubleType, ? >>();
				ctnLevel.add( root );
				while ( ctnLevel.size() > 0 ) {
					for ( final ComponentTreeNode< DoubleType, ? > ctn : ctnLevel ) {
						addBoxAtIndex( i, ctn, xydxdyCTNBorders, ySegmentationData, level );
						System.out.print( String.format( "%.4f;\t", ilp.localCost( t, ctn ) ) );
						i++;
					}
					ctnLevel = ComponentTreeUtils.getAllChildren( ctnLevel );
					level++;
					System.out.println( "" );
				}

				i = 0;
				for ( final ComponentTreeNode< DoubleType, ? > ctn : ilp.getOptimalSegmentation( t ) ) {
					addBoxAtIndex( i, ctn, xydxdyCTNBordersActive, ySegmentationData, ComponentTreeUtils.getLevelInTree( ctn ) );
					i++;
				}
			}
			plot.addBoxPlot( "Seg. Hypothesis", new Color( 127, 127, 127, 255 ), xydxdyCTNBorders );
			if ( ilp.getOptimalSegmentation( t ).size() > 0 ) {
				plot.addBoxPlot( "Active Seg. Hypothesis", new Color( 255, 0, 0, 255 ), xydxdyCTNBordersActive );
			}
		}
	}

	/**
	 * @param index
	 * @param ctn
	 * @param boxDataArray
	 * @param ydata
	 * @param level
	 */
	private void addBoxAtIndex( final int index, final ComponentTreeNode< DoubleType, ? > ctn, final double[][] boxDataArray, final double[] ydata, final int level ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final Iterator< Localizable > componentIterator = ctn.iterator();
		while ( componentIterator.hasNext() ) {
			final int pos = componentIterator.next().getIntPosition( 0 );
			min = Math.min( min, pos );
			max = Math.max( max, pos );
		}
		final int maxLocation = SimpleFunctionAnalysis.getMax( ydata, min, max ).a.intValue();
		final int leftLocation = min;
		final int rightLocation = max;
		final double maxLocVal = ydata[ maxLocation ];
		final double minVal = SimpleFunctionAnalysis.getMin( ydata, min, max ).b.doubleValue();
//					xydxdyCTNBorders[ i ] = new double[] { 0.5 * ( leftLocation + rightLocation ) + 1, 0.5 * ( minVal + maxLocVal ), rightLocation - leftLocation, maxLocVal - minVal };
		boxDataArray[ index ] = new double[] { 0.5 * ( leftLocation + rightLocation ) + 1, 1.0 - level * 0.05 - 0.02, rightLocation - leftLocation, 0.02 };
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------


	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Picks the right hyperslice in Z direction in imgRaw and sets an
	 * View.offset according to the current offset settings. Note: this method
	 * does not and should not invoke a repaint!
	 */
	public void dataToDisplayChanged() {

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// IF 'COUNTING VIEW' VIEW IS ACTIVE
		// =================================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelCountingView ) ) {
			if ( ilp != null ) {
				panelCountingView.showData( model.getCurrentGL() );
			} else {
				panelCountingView.showData( null );
			}
		}

		// IF SEGMENTATION AND ASSIGNMENT VIEW IS ACTIVE
		// =============================================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelSegmentationAndAssignmentView ) ) {
			// - - t-1 - - - - - -

			if ( model.getCurrentGLFsPredecessor() != null ) {
				final GrowthLineFrame glf = model.getCurrentGLFsPredecessor();
				viewImgLeftActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
				imgCanvasActiveLeft.setScreenImage( glf, viewImgLeftActive );
			} else {
				// show something empty
				imgCanvasActiveLeft.setEmptyScreenImage();
			}

			// - - t+1 - - - - - -

			if ( model.getCurrentGLFsSuccessor() != null ) {
				final GrowthLineFrame glf = model.getCurrentGLFsSuccessor();
				viewImgRightActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
				imgCanvasActiveRight.setScreenImage( glf, viewImgRightActive );
			} else {
				// show something empty
				imgCanvasActiveRight.setEmptyScreenImage();
			}

			// - -  t  - - - - - -

			final GrowthLineFrame glf = model.getCurrentGLF();
			viewImgCenterActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
			imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );

			// - -  assignment-views  - - - - - -

			if ( ilp != null ) {
				final int t = sliderTime.getValue();
				if ( t == 0 ) {
					leftAssignmentViewer.display( null );
				} else {
					leftAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( t - 1 ) );
				}
				if ( t == sliderTime.getMaximum() ) {
					rightAssignmentViewer.display( null );
				} else {
					rightAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( t ) );
				}
			} else {
				leftAssignmentViewer.display( null );
				rightAssignmentViewer.display( null );
			}
		}

		// IF DETAILED DATA VIEW IS ACTIVE
		// ===============================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelDetailedDataView ) ) {
			updatePlotPanels();
		}
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {

		if ( e.getSource().equals( sliderGL ) ) {
			model.setCurrentGL( sliderGL.getValue(), sliderTime.getValue() );
		}

		if ( e.getSource().equals( sliderTime ) ) {
			model.setCurrentGLF( sliderTime.getValue() );
		}

		dataToDisplayChanged();
		this.repaint();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( btnOptimize ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					if ( model.getCurrentGL().getIlp() == null ) {
						System.out.println( "Generating ILP..." );
						model.getCurrentGL().generateILP();
					} else {
						System.out.println( "Using existing ILP (possibly containing user-defined ground-truth bits)..." );
					}
					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );
					dataToDisplayChanged();
				}
			} );
			t.start();
		}
		if ( e.getSource().equals( btnOptimizeAll ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					int i = 0;
					final int glCount = model.mm.getGrowthLines().size();
					for ( final GrowthLine gl : model.mm.getGrowthLines() ) {
						i++;
						System.out.println( String.format( "Generating ILP #%d of %d...", i, glCount ) );
						gl.generateILP();
						System.out.println( String.format( "Running ILP #%d of %d...", i, glCount ) );
						gl.runILP();
					}
					System.out.println( "...done!" );
					dataToDisplayChanged();
				}
			} );
			t.start();
		}
		if ( e.getSource().equals( btnOptimizeRemainingAndExport ) ) {
			final MotherMachineGui self = this;
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					final JFileChooser fc = new JFileChooser( MotherMachine.DEFAULT_PATH );
					fc.addChoosableFileFilter( new ExtensionFileFilter( new String[] { "csv", "CSV" }, "CVS-file" ) );

					if ( fc.showSaveDialog( self ) == JFileChooser.APPROVE_OPTION ) {
						File file = fc.getSelectedFile();
						if ( !file.getAbsolutePath().endsWith( ".csv" ) && !file.getAbsolutePath().endsWith( ".CSV" ) ) {
							file = new File( file.getAbsolutePath() + ".csv" );
						}
						MotherMachine.DEFAULT_PATH = file.getParent();

						final Vector< Vector< String >> dataToExport = new Vector< Vector< String >>();

						int i = 0;
						final int glCount = model.mm.getGrowthLines().size();
						for ( final GrowthLine gl : model.mm.getGrowthLines() ) {
							i++;
							if ( gl.getIlp() == null ) {
								System.out.println( String.format( "\nGenerating ILP #%d of %d...", i, glCount ) );
								gl.generateILP();
								System.out.println( String.format( "Running ILP #%d of %d...", i, glCount ) );
								gl.runILP();
							}

							dataToExport.add( gl.getDataVector() );
						}

						System.out.println( "Exporting data..." );
						Writer out = null;
						try {
							out = new OutputStreamWriter( new FileOutputStream( file ) );

							// writing header line
							int rowNum = 0;
							out.write( ", " );
							for ( int colNum = 0; colNum < dataToExport.get( 0 ).size(); colNum++ ) {
								out.write( String.format( "t=%d, ", rowNum ) );
								rowNum++;
							}
							out.write( "\n" );
							// writing GL-data-rows
							int totalCellCount = 0;
							for ( final Vector< String > rowInData : dataToExport ) {
								rowNum++;
								out.write( String.format( "GL%d, ", rowNum ) );
								int lastValue = 0;
								for ( final String datum : rowInData ) {
									out.write( datum + ", " );
									try {
										lastValue = Integer.parseInt( datum );
									}
									catch ( final NumberFormatException nfe ) {
										lastValue = 0;
									}
								}
								totalCellCount += lastValue;
								out.write( "\n" );
							}
							out.write( "\nTotal cell count:, " + totalCellCount );
							out.close();
						}
						catch ( final FileNotFoundException e1 ) {
							JOptionPane.showMessageDialog( self, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
							e1.printStackTrace();
						}
						catch ( final IOException e1 ) {
							JOptionPane.showMessageDialog( self, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
							e1.printStackTrace();
						}
						System.out.println( "...done!" );
						dataToDisplayChanged();
					}
				}
			} );
			t.start();
		}
	}

}

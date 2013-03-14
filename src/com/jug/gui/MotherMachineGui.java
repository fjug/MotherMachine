/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.display.XYProjector;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.GrowthLineFrame;

/**
 * @author jug
 */
public class MotherMachineGui extends JPanel implements ChangeListener {

	final protected class Viewer2DCanvas extends JComponent {

		private static final long serialVersionUID = 8284204775277266994L;

		private final int w;
		private final int h;
		private XYProjector projector;
		private ARGBScreenImage screenImage;
		private IntervalView< DoubleType > view;
		private GrowthLineFrame glf;

		public Viewer2DCanvas( final int w, final int h ) {
			super();
			this.w = w;
			this.h = h;
			setPreferredSize( new Dimension( w, h ) );
			this.screenImage = new ARGBScreenImage( w, h );
			this.projector = null;
			this.view = null;
			this.glf = null;
		}

		/**
		 * Sets the image data to be displayed when paintComponent is called.
		 *
		 * @param glf
		 *            the GrowthLineFrameto be displayed
		 * @param viewImg
		 *            an IntervalView<DoubleType> containing the desired view
		 *            onto the raw image data
		 */
		public void setScreenImage( final GrowthLineFrame glf, final IntervalView< DoubleType > viewImg ) {
			setEmptyScreenImage();
			this.projector = new XYProjector< DoubleType, ARGBType >( viewImg, screenImage, new RealARGBConverter< DoubleType >( 0, 1 ) );
			this.view = viewImg;
			this.glf = glf;
		}

		/**
		 * Prepares to display an empty image.
		 */
		public void setEmptyScreenImage() {
			screenImage = new ARGBScreenImage( w, h );
			this.projector = null;
			this.view = null;
			this.glf = null;
		}


		@Override
		public void paintComponent( final Graphics g ) {
			try {
				if ( projector != null ) {
					projector.map();
				}
				glf.drawCenterLine( screenImage, view );
				//TODO NOT nice... do something against that, please!
				final int t = glf.getParent().getFrames().indexOf( glf );
				glf.drawOptimalSegmentation( screenImage, view, glf.getParent().getIlp().getOptimalSegmentation( t ) );
			}
			catch ( final ArrayIndexOutOfBoundsException e ) {
				// this can happen if a growth line, due to shift, exists in one
				// frame, and does not exist in others.
				// If for this growth line we want to visualize a time where the
				// GrowthLine is empty, the projector
				// throws a ArrayIndexOutOfBoundsException that I catch
				// hereby... ;)
				System.err.println( "ArrayIndexOutOfBoundsException in paintComponent of MMGUI!" );
				// e.printStackTrace();
			}
			catch ( final NullPointerException e ) {
				System.err.println( "View or glf not yet set in MotherMachineGui!" );
				e.printStackTrace();
			}
			g.drawImage( screenImage.image(), 0, 0, w, h, null );
		}
	}

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
	 * The XYProjector for the left Viewer2DCanvas
	 */
	@SuppressWarnings( "rawtypes" )
	protected XYProjector projectorLeft;

	/**
	 * The XYProjector for the central Viewer2DCanvas
	 */
	@SuppressWarnings( "rawtypes" )
	protected XYProjector projectorCenter;

	/**
	 * The XYProjector for the right Viewer2DCanvas
	 */
	@SuppressWarnings( "rawtypes" )
	protected XYProjector projectorRight;

	/**
	 * The pixel offset in z-direction to the part of the raw image data that is
	 * supposed to be shown in the central ScreenImage.
	 */
	private long screenImageOffsetZ;

	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (left one).
	 */
	IntervalView< DoubleType > viewImgLeft;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (center one).
	 */
	IntervalView< DoubleType > viewImgCenter;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (right one).
	 */
	IntervalView< DoubleType > viewImgRight;

	// -------------------------------------------------------------------------------------
	// gui-fields
	// -------------------------------------------------------------------------------------
	private Viewer2DCanvas imgCanvasLeft;
	private Viewer2DCanvas imgCanvasCenter;
	private Viewer2DCanvas imgCanvasRight;

	private JSlider sliderGL;
	private JSlider sliderTime;

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

		final JPanel panelCurationView = new JPanel( new BorderLayout() );
		final JPanel panelCurationViewHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 25, 10 ) );
		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;
		JLabel labelHelper;

		// ----------------

//		final JLabel labelCurationView = new JLabel( "Segmentation and Tracking" );
//		labelCurationView.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
//		panelCurationView.add( labelCurationView, BorderLayout.NORTH );

		// ----------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t-1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasLeft = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasLeft, BorderLayout.CENTER );
		panelCurationViewHelper.add( panelVerticalHelper );

		// ----------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasCenter = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasCenter, BorderLayout.CENTER );
		panelCurationViewHelper.add( panelVerticalHelper );

		// ----------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t+1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasRight = new Viewer2DCanvas( GL_WIDTH_TO_SHOW, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasRight, BorderLayout.CENTER );
		panelCurationViewHelper.add( panelVerticalHelper );

		// ----------------

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

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, model.getCurrentGL().size() - 1, 0 );
		sliderTime.setValue( 1 );
		model.setCurrentGLF( sliderTime.getValue() );
		sliderTime.addChangeListener( this );
		sliderTime.setMajorTickSpacing( 5 );
		sliderTime.setMinorTickSpacing( 1 );
		sliderTime.setPaintTicks( true );
		sliderTime.setPaintLabels( true );
		sliderTime.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 3 ) );
		panelHorizontalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper.setBorder( BorderFactory.createEmptyBorder( 5, 10, 0, 5 ) );
		panelHorizontalHelper.add( new JLabel( " t = " ), BorderLayout.WEST );
		panelHorizontalHelper.add( sliderTime, BorderLayout.CENTER );
		add( panelHorizontalHelper, BorderLayout.SOUTH );

		// ----------------

		panelCurationView.add( panelCurationViewHelper, BorderLayout.CENTER );
		add( panelCurationView, BorderLayout.CENTER );

		// - - - - - - - - - - - -
		// KEYSTROKE SETUP (using
		// Input- and ActionMaps)
		// - - - - - - - - - - - -
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 't' ), "GLV_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'g' ), "GLV_bindings" );

		this.getActionMap().put( "GLV_bindings", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				if ( e.getActionCommand().equals( "t" ) ) {
					sliderTime.requestFocus();
				}
				if ( e.getActionCommand().equals( "g" ) ) {
					sliderGL.requestFocus();
				}
			}
		} );
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
	private void dataToDisplayChanged() {

		// - - t-1 - - - - - -

		if ( model.getCurrentGLFsPredecessor() != null ) {
			final GrowthLineFrame glf = model.getCurrentGLFsPredecessor();
			viewImgLeft = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
			imgCanvasLeft.setScreenImage( glf, viewImgLeft );
		} else {
			// show something empty
			imgCanvasLeft.setEmptyScreenImage();
		}

		// - - t+1 - - - - - -

		if ( model.getCurrentGLFsSuccessor() != null ) {
			final GrowthLineFrame glf = model.getCurrentGLFsSuccessor();
			viewImgRight = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
			imgCanvasRight.setScreenImage( glf, viewImgRight );
		} else {
			// show something empty
			imgCanvasRight.setEmptyScreenImage();
		}

		// - -  t  - - - - - -

		final GrowthLineFrame glf = model.getCurrentGLF();
		viewImgCenter = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetZ() ), glf.getOffsetX() - GL_WIDTH_TO_SHOW / 2, glf.getOffsetY() );
		imgCanvasCenter.setScreenImage( glf, viewImgCenter );

	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {

		if ( e.getSource().equals( sliderGL ) ) {
			model.setCurrentGL( sliderGL.getValue() );
			dataToDisplayChanged();
			this.repaint();
		}

		if ( e.getSource().equals( sliderTime ) ) {
			model.setCurrentGLF( sliderTime.getValue() );
			dataToDisplayChanged();
			this.repaint();
		}
	}

}

/**
 *
 */
package com.jug.lp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;


/**
 * Partially implemented class for everything that wants to be an assignment.
 * The main purpose of such a class is to store the value of the corresponding
 * Gurobi assignment variable and the ability to add assignment specific
 * constraints to the ILP (model).
 *
 * @author jug
 */
public abstract class AbstractAssignment< H extends Hypothesis< ? > > {

	private int type;

	protected GRBModel model;

	private GRBVar ilpVar;

	/**
	 * Creates an assignment...
	 *
	 * @param type
	 * @param cost
	 */
	public AbstractAssignment( final int type, final GRBVar ilpVariable, final GRBModel model ) {
		this.setType( type );
		setGRBVar( ilpVariable );
		setGRBModel( model );
	}


	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType( final int type ) {
		this.type = type;
	}

	/**
	 * @return the ilpVar
	 */
	public GRBVar getGRBVar() {
		return ilpVar;
	}

	/**
	 * @param ilpVar
	 *            the ilpVar to set
	 */
	public void setGRBVar( final GRBVar ilpVar ) {
		this.ilpVar = ilpVar;
	}

	/**
	 * @param model
	 *            GRBModel instance (the ILP)
	 */
	public void setGRBModel( final GRBModel model ) {
		this.model = model;
	}

	/**
	 * @return the cost
	 * @throws GRBException
	 */
	public double getCost() throws GRBException {
		return getGRBVar().get( GRB.DoubleAttr.Obj );
	}

	/**
	 * @param cost
	 *            the cost to set
	 * @throws GRBException
	 */
	public void setCost( final double cost ) throws GRBException {
		getGRBVar().set( GRB.DoubleAttr.ObjVal, cost );
	}

	/**
	 * @return true, if the ilpVar of this Assignment is equal to 1.0.
	 * @throws GRBException
	 */
	public boolean isChoosen() throws GRBException {
		return ( getGRBVar().get( GRB.DoubleAttr.X ) == 1.0 );
	}

	/**
	 * Abstract method that will, once implemented, add a set of assignment
	 * related constraints to the ILP (model) later to be solved by Gurobi.
	 *
	 * @throws GRBException
	 */
	public abstract void addConstraintsToLP() throws GRBException;
}

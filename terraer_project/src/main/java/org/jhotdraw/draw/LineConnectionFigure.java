/*
 * @(#)BezierBezierLineConnection.java  1.0.2  2007-05-02
 *
 * Copyright (c) 1996-2007 by the original authors of JHotDraw
 * and all its contributors ("JHotDraw.org")
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * JHotDraw.org ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * JHotDraw.org.
 */

package org.jhotdraw.draw;

import static org.jhotdraw.draw.AttributeKeys.STROKE_COLOR;
import static org.jhotdraw.draw.AttributeKeys.TEXT_COLOR;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.jhotdraw.geom.BezierPath;
import org.jhotdraw.util.ResourceBundleUtil;
import org.jhotdraw.xml.DOMInput;
import org.jhotdraw.xml.DOMOutput;

/**
 * A LineConnection is a standard implementation of the
 * ConnectionFigure interface. The interface is implemented with BezierFigure.
 *
 *
 *
 * @author Werner Randelshofer
 * @version 1.0.2 2007-05-02 Set connector variables directly when reading in
 * connectors.
 * <br>1.0.1 2006-02-06 Fixed redo bug.
 * <br>1.0 23. Januar 2006 Created.
 */
public class LineConnectionFigure extends LineFigure
        implements ConnectionFigure {
    private Connector    startConnector;
    private Connector    endConnector;
    private Liner liner;
    
    public static boolean validateEnabled = false;
    
    private static int counter = 0;
    protected String title ;
    ModelValidation validation = new ModelValidation();

    
    /**
     * Handles figure changes in the start and the
     * end figure.
     */
    private ConnectionHandler connectionHandler = new ConnectionHandler(this);
    private static class ConnectionHandler extends FigureAdapter {
        private LineConnectionFigure owner;
        private ConnectionHandler(LineConnectionFigure owner) {
            this.owner = owner;
        }
        @Override public void figureRemoved(FigureEvent evt) {
            // The commented lines below must stay commented out.
            // This is because, we must not set our connectors to null,
            // in order to support reconnection using redo.
            /*
            if (evt.getFigure() == owner.getStartFigure()
            || evt.getFigure() == owner.getEndFigure()) {
                owner.setStartConnector(null);
                owner.setEndConnector(null);
            }*/
            owner.fireFigureRequestRemove();
        }
        
        @Override public void figureChanged(FigureEvent e) {
            if (e.getSource() == owner.getStartFigure() ||
                    e.getSource() == owner.getEndFigure()) {
                owner.willChange();
                owner.updateConnection();
                owner.changed();
            }
        }
    };
    
    /** Creates a new instance. */
    public LineConnectionFigure() {
    	ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
    	if (this.getStartFigure()!= null || this.getEndFigure() != null){    	
           	title=labels.getString("connectionTitle") + this.getStartFigure().getToolTipText(null) + " "
           			+ labels.getString("connectionTitle2") + this.getEndFigure().getToolTipText(null);
       	}else{
        	title=labels.getString("createElbowConnectionAtributo")+Integer.toString(counter);
        	}
    }
    // DRAWING
    // SHAPE AND BOUNDS
    /**
     * Ensures that a connection is updated if the connection
     * was moved.
     */
    public void transform(AffineTransform tx) {
        super.transform(tx);
        updateConnection(); // make sure that we are still connected
    }
    // ATTRIBUTES
    // EDITING
    /**
     * Gets the handles of the figure. It returns the normal
     * PolylineHandles but adds ChangeConnectionHandles at the
     * start and end.
     */
    public Collection<Handle> createHandles(int detailLevel) {
        ArrayList<Handle> handles = new ArrayList<Handle>(getNodeCount());
        
        switch (detailLevel) {
            case 0 :
                if (getLiner() == null) {
                    handles.add(new BezierOutlineHandle(this));
                    for (int i = 1, n = getNodeCount() - 1; i < n; i++) {
                        handles.add(new BezierNodeHandle(this, i));
                    }
                }
                handles.add(new ConnectionStartHandle(this));
                handles.add(new ConnectionEndHandle(this));
                break;
        }
        return handles;
    }
    
// CONNECTING
    /**
     * Tests whether a figure can be a connection target.
     * ConnectionFigures cannot be connected and return false.
     */
    public boolean canConnect() {
        return false;
    }
    public void updateConnection() {
        willChange();
        if (getStartConnector() != null) {
            Point2D.Double start = getStartConnector().findStart(this);
            if(start != null) {
                setStartPoint(start);
            }
        }
        if (getEndConnector() != null) {
            Point2D.Double end = getEndConnector().findEnd(this);
            
            if(end != null) {
                setEndPoint(end);
            }
        }
        
        if (validateEnabled){
        	TEXT_COLOR.set(this, validation.validateLineConnection(this));
        	STROKE_COLOR.set(this, validation.validateLineConnection(this));
        }else{
        	TEXT_COLOR.set(this, Color.black);
        	STROKE_COLOR.set(this, Color.black);
        }
                
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
        title=labels.getString("connectionTitle") + this.getStartFigure().getToolTipText(null) + " "
       			+ labels.getString("connectionTitle2") + this.getEndFigure().getToolTipText(null);
        org.jhotdraw.draw.TerraFigureTree.getInstance().refresh(this);
        changed();
    }
    
    public void validate() {
        super.validate();
        lineout();
    }
    
    public boolean canConnect(Connector start, Connector end) {
    	return start.getOwner().canConnect() && end.getOwner().canConnect();
    }
    
    public Connector getEndConnector() {
        return endConnector;
    }
    
    public Figure getEndFigure() {
        return (endConnector == null) ? null : endConnector.getOwner();
    }
    
    public Connector getStartConnector() {
        return startConnector;
    }
    
    public Figure getStartFigure() {
        return (startConnector == null) ? null : startConnector.getOwner();
    }
    public void setEndConnector(Connector newEnd) {
        if (newEnd != endConnector) {
            if (endConnector != null) {
                getEndFigure().removeFigureListener(connectionHandler);
                if (getStartFigure() != null) {
                    if (getDrawing() != null) {
                        handleDisconnect(getStartConnector(), getEndConnector());
                    }
                }
            }
            endConnector = newEnd;
            if (endConnector != null) {
                getEndFigure().addFigureListener(connectionHandler);
                if (getStartFigure() != null && getEndFigure() != null) {
                    if (getDrawing() != null) {
                        handleConnect(getStartConnector(), getEndConnector());
                        updateConnection();
                    }
                }
            }
        }
    }
    
    public void setStartConnector(Connector newStart) {
        if (newStart != startConnector) {
            if (startConnector != null) {
                getStartFigure().removeFigureListener(connectionHandler);
                if (getEndFigure() != null) {
                    handleDisconnect(getStartConnector(), getEndConnector());
                }
            }
            startConnector = newStart;
            if (startConnector != null) {
                getStartFigure().addFigureListener(connectionHandler);
                if (getStartFigure() != null && getEndFigure() != null) {
                    handleConnect(getStartConnector(), getEndConnector());
                    updateConnection();
                }
            }
        }
    }
    
    
    
    // COMPOSITE FIGURES
    // LAYOUT
    /*
    public Liner getBezierPathLayouter() {
        return (Liner) getAttribute(BEZIER_PATH_LAYOUTER);
    }
    public void setBezierPathLayouter(Liner newValue) {
        setAttribute(BEZIER_PATH_LAYOUTER, newValue);
    }
    /**
     * Lays out the connection. This is called when the connection
     * itself changes. By default the connection is recalculated
     * /
    public void layoutConnection() {
        if (getStartConnector() != null && getEndConnector() != null) {
            willChange();
            Liner bpl = getBezierPathLayouter();
            if (bpl != null) {
                bpl.lineout(this);
            } else {
                if (getStartConnector() != null) {
                    Point2D.Double start = getStartConnector().findStart(this);
                    if(start != null) {
                        basicSetStartPoint(start);
                    }
                }
                if (getEndConnector() != null) {
                    Point2D.Double end = getEndConnector().findEnd(this);
     
                    if(end != null) {
                        basicSetEndPoint(end);
                    }
                }
            }
            changed();
        }
    }
     */
    // CLONING
    // EVENT HANDLING
    /**
     * This method is invoked, when the Figure is being removed from a Drawing.
     * This method invokes handleConnect, if the Figure is connected.
     *
     * @see #handleConnect
     */
    public void addNotify(Drawing drawing) {
        super.addNotify(drawing);
        
        if (getStartConnector() != null && getEndConnector() != null) {
            handleConnect(getStartConnector(), getEndConnector());
            updateConnection();
        }
    }
    
    /**
     * This method is invoked, when the Figure is being removed from a Drawing.
     * This method invokes handleDisconnect, if the Figure is connected.
     *
     * @see #handleDisconnect
     */
    public void removeNotify(Drawing drawing) {
        if (getStartConnector() != null && getEndConnector() != null) {
            handleDisconnect(getStartConnector(), getEndConnector());
        }
        // Note: we do not set the connectors to null here, because we
        // need them when we are added back to a drawing again. For example,
        // when an undo is performed, after the LineConnection has been
        // deleted.
        /*
        setStartConnector(null);
        setEndConnector(null);
         */
        super.removeNotify(drawing);
    }
    
    /**
     * Handles the disconnection of a connection.
     * Override this method to handle this event.
     * <p>
     * Note: This method is only invoked, when the Figure is part of a
     * Drawing. If the Figure is removed from a Drawing, this method is
     * invoked on behalf of the removeNotify call to the Figure.
     *
     * @see #removeNotify
     */
    protected void handleDisconnect(Connector start, Connector end) {
    }
    
    /**
     * Handles the connection of a connection.
     * Override this method to handle this event.
     * <p>
     * Note: This method is only invoked, when the Figure is part of a
     * Drawing. If the Figure is added to a Drawing this method is invoked
     * on behalf of the addNotify call to the Figure.
     *
     * @see #addConnect
     */
    protected void handleConnect(Connector start, Connector end) {
    }
    
    
    public LineConnectionFigure clone() {
        LineConnectionFigure that = (LineConnectionFigure) super.clone();
        that.connectionHandler = new ConnectionHandler(that);
        if (this.liner != null) {
            that.liner = (Liner) this.liner.clone();
        }
        that.title = this.title.substring(0, title.length()-1) +Integer.toString(++counter);
        // FIXME - For safety reasons, we clone the connectors, but they would
        // work, if we continued to use them. Maybe we should state somewhere
        // whether connectors should be reusable, or not.
        // To work properly, that must be registered as a figure listener
        // to the connected figures.
        if (this.startConnector != null) {
            that.startConnector = (Connector) this.startConnector.clone();
            that.getStartFigure().addFigureListener(that.connectionHandler);
        }
        if (this.endConnector != null) {
            that.endConnector = (Connector) this.endConnector.clone();
            that.getEndFigure().addFigureListener(that.connectionHandler);
        }
        if (that.startConnector != null && that.endConnector != null) {
            //that.handleConnect(that.getStartConnector(), that.getEndConnector());
            that.updateConnection();
        }
        return that;
    }
    
    public void remap(Map oldToNew) {
        willChange();
        super.remap(oldToNew);
        Figure newStartFigure = null;
        Figure newEndFigure = null;
        if (getStartFigure() != null) {
            newStartFigure = (Figure) oldToNew.get(getStartFigure());
            if (newStartFigure == null) newStartFigure = getStartFigure();
        }
        if (getEndFigure() != null) {
            newEndFigure = (Figure) oldToNew.get(getEndFigure());
            if (newEndFigure == null) newEndFigure = getEndFigure();
        }
        
        if (newStartFigure != null) {
            setStartConnector(newStartFigure.findCompatibleConnector(getStartConnector(),  true));
        }
        if (newEndFigure != null) {
            setEndConnector(newEndFigure.findCompatibleConnector(getEndConnector(),  false));
        }
        
        updateConnection();
        changed();
    }
    
    
    public boolean canConnect(Connector start) {
        return start.getOwner().canConnect();
    }
    
    /**
     * Handles a mouse click.
     */
    public boolean handleMouseClick(Point2D.Double p, MouseEvent evt, DrawingView view) {
        if (getLiner() == null &&
                evt.getClickCount() == 2) {
            willChange();
            final int index = splitSegment(p, (float) (5f / view.getScaleFactor()));
            if (index != -1) {
                final BezierPath.Node newNode = getNode(index);
                fireUndoableEditHappened(new AbstractUndoableEdit() {
                    public void redo() throws CannotRedoException {
                        super.redo();
                        willChange();
                        addNode(index, newNode);
                        changed();
                    }
                    
                    public void undo() throws CannotUndoException {
                        super.undo();
                        willChange();
                        removeNode(index);
                        changed();
                    }
                    
                });
                changed();
                return true;
            }
        }
        return false;
    }
    // PERSISTENCE
    protected void readPoints(DOMInput in) throws IOException {
        super.readPoints(in);
        in.openElement("startConnector");
        setStartConnector((Connector) in.readObject());
        in.closeElement();
        in.openElement("endConnector");
        setEndConnector((Connector) in.readObject());
        in.closeElement();
    }
    public void read(DOMInput in) throws IOException {
        readAttributes(in);
        readLiner(in);
        
        // Note: Points must be read after Liner, because Liner influences
        // the location of the points.
        readPoints(in);
    }
    protected void readLiner(DOMInput in) throws IOException {
        if (in.getElementCount("liner") > 0) {
            in.openElement("liner");
            liner = (Liner) in.readObject();
            in.closeElement();
        } else {
            liner = null;
        }
        
    }
    public void write(DOMOutput out) throws IOException {
        writePoints(out);
        writeAttributes(out);
        writeLiner(out);
    }
    protected void writeLiner(DOMOutput out) throws IOException {
        if (liner != null) {
            out.openElement("liner");
            out.writeObject(liner);
            out.closeElement();
        }
    }
    protected void writePoints(DOMOutput out) throws IOException {
        super.writePoints(out);
        out.openElement("startConnector");
        out.writeObject(getStartConnector());
        out.closeElement();
        out.openElement("endConnector");
        out.writeObject(getEndConnector());
        out.closeElement();
    }
    
    public void setLiner(Liner newValue) {
        this.liner = newValue;
    }
    
    
    public void setNode(int index, BezierPath.Node p) {
        if (index != 0 && index != getNodeCount() - 1) {
            if (getStartConnector() != null) {
                Point2D.Double start = getStartConnector().findStart(this);
                if(start != null) {
                    setStartPoint(start);
                }
            }
            if (getEndConnector() != null) {
                Point2D.Double end = getEndConnector().findEnd(this);
                
                if(end != null) {
                    setEndPoint(end);
                }
            }
        }
        super.setNode(index, p);
    }
    /*
    public void basicSetPoint(int index, Point2D.Double p) {
        if (index != 0 && index != getNodeCount() - 1) {
            if (getStartConnector() != null) {
                Point2D.Double start = getStartConnector().findStart(this);
                if(start != null) {
                    basicSetStartPoint(start);
                }
            }
            if (getEndConnector() != null) {
                Point2D.Double end = getEndConnector().findEnd(this);
     
                if(end != null) {
                    basicSetEndPoint(end);
                }
            }
        }
        super.basicSetPoint(index, p);
    }
     */
    public void lineout() {
        if (liner != null) {
            liner.lineout(this);
        }
    }
    /**
     * FIXME - Liner must work with API of LineConnection!
     */
    public BezierPath getBezierPath() {
        return path;
    }
    
    
    public Liner getLiner() {
        return liner;
    }
    
    public void setStartPoint(Point2D.Double p) {
        setPoint(0, p);
    }
    
    public void setPoint(int index, Point2D.Double p) {
        setPoint(index, 0, p);
    }
    
    public void setEndPoint(Point2D.Double p) {
        setPoint(getNodeCount() - 1, p);
    }
    
    public void reverseConnection() {
        if (startConnector != null && endConnector != null) {
            handleDisconnect(startConnector, endConnector);
            Connector tmpC = startConnector;
            startConnector = endConnector;
            endConnector = tmpC;
            Point2D.Double tmpP = getStartPoint();
            setStartPoint(getEndPoint());
            setEndPoint(tmpP);
            handleConnect(startConnector, endConnector);
            updateConnection();
        }
    }
    
   
    public String toString(){
    	return title;
    }
}
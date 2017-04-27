
package org.jhotdraw.draw.notation.finalversion;

import java.awt.Color;
import java.awt.geom.Point2D.Double;
import java.io.IOException;

import org.jhotdraw.draw.AbstractCompositeFigure;
import org.jhotdraw.draw.AttributeKeys;
import org.jhotdraw.draw.EllipseFigure;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.draw.FigureAdapter;
import org.jhotdraw.draw.FigureEvent;
import org.jhotdraw.draw.GroupFigure;
import org.jhotdraw.draw.TerraResizeEventFunctions;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.util.ResourceBundleUtil;
import org.jhotdraw.xml.DOMInput;
import org.jhotdraw.xml.DOMOutput;

public class AtributoDerivadoFigureChen extends GroupFigure implements IChangeNotationListern {
	private TextFigure tf;
	private EllipseFigure ef;
    private static int counter = 0;
    private TerraResizeEventFunctions EventFunctions;
    private String sql;
    private String currentNotation;

	public AtributoDerivadoFigureChen(){
    	super();
    	currentNotation = NotationSelectAction.SelectChenAction.ID;
		NotationSelectAction.addListern(this);
    }
    
    public AtributoDerivadoFigureChen init(){
    	ef=new EllipseFigure();
    	ef.setAttribute(AttributeKeys.FILL_COLOR, new Color(255, 235, 235));
		ef.setAttribute(AttributeKeys.STROKE_DASHES, new double[] { 5.0 });
    	
    	ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");

    	tf=new TextFigure(labels.getString("createAtributoDerivado")+Integer.toString(counter++));
    	this.add(ef);
    	this.add(tf);
    	this.EventFunctions=new TerraResizeEventFunctions(this,ef,tf);
    	this.tf.addFigureListener(new FigureAdapter(){
			@Override
			public void figureAttributeChanged(FigureEvent e){
				EventFunctions.figureTextChanged(e);
			}
			
			@Override
			public void figureChanged(FigureEvent e) {
				EventFunctions.figureSizeChanged();
			}
    	});
    	return this;
	}
    
    @Override
	public String getToolTipText(Double p) {
		return this.toString();
	}

    public AbstractCompositeFigure clone() {
		AtributoDerivadoFigureChen f = new AtributoDerivadoFigureChen().init();

		f.willChange();
		f.ef.setBounds(this.ef.getBounds());
		f.tf.setBounds(this.tf.getBounds());
		f.changed();

		return f;
	}
	
	public String toString(){
		return tf.getText().replaceAll("\\s+", "_");
	}

    public void read(DOMInput in) throws IOException {
    	super.read(in);
    	this.sql = in.getAttribute("sql", null);
    	
        java.util.Collection<Figure> lst=getDecomposition();
        for( Figure f : lst){
            if(f instanceof TextFigure){
                tf=(TextFigure)f;
            }
            else if(f instanceof EllipseFigure){
                ef=(EllipseFigure)f;
            }
        }
    }   
    
    @Override
	public void write(DOMOutput out) throws IOException {
		super.write(out);
		out.addAttribute("sql", this.sql);
	}
	
	public String getSql() {
		return this.sql;
	}
	
	public void setSql(String sql) {
		this.sql = sql;
	}

	@Override
	public void notifyChange(String notation) {
		currentNotation = notation;
		if (this.currentNotation.equals(NotationSelectAction.SelectChenAction.ID)) {
			this.setVisible(true);
		} else if (this.currentNotation.equals(NotationSelectAction.SelectCrossFootAction.ID)) {
			this.setVisible(false);
		} else if (this.currentNotation.equals(NotationSelectAction.SelectIDEF1XAction.ID)) {
			this.setVisible(false);
		}
		
	}
}
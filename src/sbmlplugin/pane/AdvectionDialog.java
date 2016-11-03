package sbmlplugin.pane;

import ij.gui.GenericDialog;

import org.sbml.libsbml.AdvectionCoefficient;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.SpatialParameterPlugin;

// TODO: Auto-generated Javadoc
/**
 * Spatial SBML Plugin for ImageJ.
 *
 * @author Kaito Ii <ii@fun.bio.keio.ac.jp>
 * @author Akira Funahashi <funa@bio.keio.ac.jp>
 * Date Created: Jan 21, 2016
 */
public class AdvectionDialog {
	
	/** The parameter. */
	private Parameter parameter;
	
	/** The gd. */
	private GenericDialog gd;
	
	/** The bool. */
	private final String[] bool = {"true","false"};
	
	/** The model. */
	private Model model;
	
	/**
	 * Instantiates a new advection dialog.
	 *
	 * @param model the model
	 */
	public AdvectionDialog(Model model){
		this.model = model;
	}
	
	/**
	 * Show dialog.
	 *
	 * @return the parameter
	 */
	public Parameter showDialog(){
		gd = new GenericDialog("Add Advection Coefficient");
		gd.setResizable(true);
		gd.pack();
	
		gd.addStringField("id:", "");
		gd.addNumericField("value:", 0, 1);
		gd.addRadioButtonGroup("constant:", bool, 1, 2, "true");
		gd.addChoice("species:", SBMLProcessUtil.listIdToStringArray(model.getListOfSpecies()), null);
		gd.addChoice("coordinate:", SBMLProcessUtil.lcoord, null);
		
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
	
		parameter = model.createParameter();
		setParameterData();
		
		return parameter;
	}

	/**
	 * Show dialog.
	 *
	 * @param parameter the parameter
	 * @return the parameter
	 */
	public Parameter showDialog(Parameter parameter){
		this.parameter = parameter;
		SpatialParameterPlugin sp = (SpatialParameterPlugin) parameter.getPlugin("spatial");
		AdvectionCoefficient ac = sp.getAdvectionCoefficient();
		gd = new GenericDialog("Edit Advection Coefficient");
		gd.setResizable(true);
		gd.pack();
		
		gd.addStringField("id:", parameter.getId());
		gd.addNumericField("value:", parameter.getValue(), 1);
		gd.addRadioButtonGroup("constant:", bool, 1, 2, String.valueOf(parameter.getConstant()));
		gd.addChoice("species:", SBMLProcessUtil.listIdToStringArray(model.getListOfSpecies()), ac.getVariable());
		gd.addChoice("coordinate:", SBMLProcessUtil.lcoord, SBMLProcessUtil.coordinateIndexToString(ac.getCoordinate()));
	
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
				
		setParameterData();
		
		return parameter;
	}
		
	/**
	 * Sets the parameter data.
	 */
	private void setParameterData(){
		String str = gd.getNextString();
		if (str.indexOf(' ')!=-1)
				str = str.replace(' ', '_');
		parameter.setId(str);
		parameter.setValue(gd.getNextNumber());
		parameter.setConstant(Boolean.getBoolean(gd.getNextRadioButton()));
		SpatialParameterPlugin sp = (SpatialParameterPlugin) parameter.getPlugin("spatial");
		AdvectionCoefficient ac = sp.isSetAdvectionCoefficient() ? sp.getAdvectionCoefficient() : sp.createAdvectionCoefficient();
		String var = gd.getNextChoice();
		ac.setVariable(var);
		ac.setCoordinate(gd.getNextChoice());
	}
}
package jp.ac.keio.bio.fun.xitosbml.xitosbml;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.spatial.Geometry;
import org.sbml.jsbml.ext.spatial.SpatialModelPlugin;

import ij.ImagePlus;
import ij.plugin.PlugIn;
import jp.ac.keio.bio.fun.xitosbml.image.CreateImage;
import jp.ac.keio.bio.fun.xitosbml.image.Filler;
import jp.ac.keio.bio.fun.xitosbml.image.ImageBorder;
import jp.ac.keio.bio.fun.xitosbml.image.ImageEdit;
import jp.ac.keio.bio.fun.xitosbml.image.ImageExplorer;
import jp.ac.keio.bio.fun.xitosbml.image.Interpolater;
import jp.ac.keio.bio.fun.xitosbml.image.SpatialImage;
import jp.ac.keio.bio.fun.xitosbml.pane.TabTables;
import jp.ac.keio.bio.fun.xitosbml.visual.DomainStruct;
import jp.ac.keio.bio.fun.xitosbml.visual.Viewer;


// TODO: Auto-generated Javadoc
/**
 * The Class MainSpatial.
 */
public abstract class MainSpatial implements PlugIn{

	/** The document. */
	protected SBMLDocument document;
	
	/** The model. */
	protected Model model;
	
	/** The spatialplugin. */
	protected SpatialModelPlugin spatialplugin;

	/** The imgexp. */
	protected ImageExplorer imgexp;
	
	/** The hash domain types. */
	private HashMap<String, Integer> hashDomainTypes;
	
	/** The hash sampled value. */
	protected HashMap<String, Integer> hashSampledValue;
	
	/** The viewer. */
	protected Viewer viewer;
	
	/** The sp img. */
	protected SpatialImage spImg;
	
	/**
	 * Gui.
	 */
	protected void gui() {
		hashDomainTypes = new HashMap<String, Integer>();
		hashSampledValue = new HashMap<String, Integer> ();
		imgexp = new ImageExplorer(hashDomainTypes,hashSampledValue);
		while (imgexp.isVisible()) {
			synchronized (hashDomainTypes) {
				synchronized (hashSampledValue) {
					
				}
			}
		}
	}
	
	/**
	 * Compute img.
	 */
	protected void computeImg(){
		Interpolater interpolater = new Interpolater();
		HashMap<String, ImagePlus> hashDomFile = imgexp.getDomFile();
		interpolater.interpolate(hashDomFile);
		Filler fill = new Filler();

		for(Entry<String, ImagePlus> e : hashDomFile.entrySet())
			hashDomFile.put(e.getKey(), fill.fill(e.getValue()));
		
		CreateImage creIm = new CreateImage(imgexp.getDomFile(), hashSampledValue);
		spImg = new SpatialImage(hashSampledValue, hashDomainTypes, creIm.getCompoImg());
		ImagePlus img = fill.fill(spImg);
		spImg.setImage(img);
		ImageBorder imgBorder = new ImageBorder(spImg);
		spImg.updateImage(imgBorder.getStackImage());

		new ImageEdit(spImg);
	}
	
	/**
	 * Visualize.
	 *
	 * @param spImg the sp img
	 */
	protected void visualize (SpatialImage spImg){
		viewer = new Viewer();
		viewer.view(spImg);
	}
	
	/**
	 * Adds the S bases.
	 */
	protected void addSBases(){
		ListOf<Parameter> lop = model.getListOfParameters();
		ListOf<Species> los = model.getListOfSpecies();
		TabTables tt = new TabTables(model);
		
		while(tt.isRunning()){
			synchronized(lop){
				synchronized(los){
					
				}
			}
		}
	}
	
	/**
	 * Show domain structure.
	 */
	protected void showDomainStructure(){
		spatialplugin = (SpatialModelPlugin)model.getPlugin("spatial");
		Geometry g = spatialplugin.getGeometry();
		new DomainStruct().show(g);	
	}
	
	/**
	 * Show step.
	 *
	 * @param spImg the sp img
	 */
	protected void showStep(SpatialImage spImg){
		visualize(spImg);
	}

	/**
	 * Prints the.
	 */
	protected void print(){
		String docStr;
		try {
			docStr = new TidySBMLWriter().writeSBMLToString(document);
			System.out.println(docStr);
		} catch (SBMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
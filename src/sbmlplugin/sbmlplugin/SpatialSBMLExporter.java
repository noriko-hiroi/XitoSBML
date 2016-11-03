package sbmlplugin.sbmlplugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

import javax.vecmath.Point3d; 

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.Unit.Kind;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.spatial.AdjacentDomains;
import org.sbml.jsbml.ext.spatial.Boundary;
import org.sbml.jsbml.ext.spatial.CompartmentMapping;
import org.sbml.jsbml.ext.spatial.CompressionKind;
import org.sbml.jsbml.ext.spatial.CoordinateComponent;
import org.sbml.jsbml.ext.spatial.CoordinateKind;
import org.sbml.jsbml.ext.spatial.DataKind;
import org.sbml.jsbml.ext.spatial.Domain;
import org.sbml.jsbml.ext.spatial.DomainType;
import org.sbml.jsbml.ext.spatial.Geometry;
import org.sbml.jsbml.ext.spatial.GeometryKind;
import org.sbml.jsbml.ext.spatial.InteriorPoint;
import org.sbml.jsbml.ext.spatial.InterpolationKind;
import org.sbml.jsbml.ext.spatial.ParametricGeometry;
import org.sbml.jsbml.ext.spatial.ParametricObject;
import org.sbml.jsbml.ext.spatial.PolygonKind;
import org.sbml.jsbml.ext.spatial.SampledField;
import org.sbml.jsbml.ext.spatial.SampledFieldGeometry;
import org.sbml.jsbml.ext.spatial.SampledVolume;
import org.sbml.jsbml.ext.spatial.SpatialCompartmentPlugin;
import org.sbml.jsbml.ext.spatial.SpatialModelPlugin;
import org.sbml.jsbml.ext.spatial.SpatialParameterPlugin;
import org.sbml.jsbml.ext.spatial.SpatialPoints;
import org.sbml.jsbml.ext.spatial.SpatialSymbolReference;

import sbmlplugin.image.SpatialImage;
import sbmlplugin.util.PluginInfo;

// TODO: Auto-generated Javadoc
/**
 * The Class SpatialSBMLExporter.
 */
public class SpatialSBMLExporter{

  /** The document. */
  private SBMLDocument document;
  
  /** The model. */
  private Model model;
  
  /** The spatialplugin. */
  private SpatialModelPlugin spatialplugin;
  
  /** The spatialcompplugin. */
  //private ReqSBasePlugin reqplugin;
  private SpatialCompartmentPlugin spatialcompplugin;
  
  /** The geometry. */
  private Geometry geometry;
  
  /** The hash domain types. */
  private HashMap<String, Integer> hashDomainTypes;
  
  /** The hash sampled value. */
  private HashMap<String, Integer> hashSampledValue;
  
  /** The hash domain num. */
  private HashMap<String, Integer> hashDomainNum;
  
  /** The hash dom interior pt. */

  private HashMap<String,Point3d> hashDomInteriorPt;
  
  /** The adjacents list. */
  private ArrayList<ArrayList<String>> adjacentsList;
  
  /** The raw. */
  private byte[] raw;
  
  /** The depth. */
  private int width, height, depth;
  
  /** The unit. */
  private String unit;
  
  /** The delta. */
  private Point3d delta;

	/**
	 * Instantiates a new spatial SBML exporter.
	 */
	public SpatialSBMLExporter() {
		document = new SBMLDocument(3,1);
		document.setPackageRequired("req", false);
		document.setPackageRequired("spatial", true);
		model = document.createModel();

		SBasePlugin basePlugin = (model.getPlugin("spatial"));
		spatialplugin = (SpatialModelPlugin) basePlugin;
		if (spatialplugin == null) {
			System.exit(1);
		}
	}

	/**
	 * Instantiates a new spatial SBML exporter.
	 *
	 * @param spImg the sp img
	 */
	public SpatialSBMLExporter(SpatialImage spImg) {
		this();
		this.hashDomainTypes = spImg.getHashDomainTypes();
		this.hashSampledValue = spImg.getHashSampledValue();
		this.hashDomainNum = spImg.getHashDomainNum();
		this.hashDomInteriorPt = spImg.getHashDomInteriorPt();
		this.raw = spImg.getRaw();
		this.width = spImg.getWidth();
		this.height = spImg.getHeight();
		this.depth = spImg.getDepth();
		this.adjacentsList = spImg.getAdjacentsList();
		this.delta = spImg.getDelta();
		model = document.getModel();
		spatialplugin = (SpatialModelPlugin) model.getPlugin("spatial");
		unit = spImg.getUnit();
	}

	/**
	 * Creates the geometry elements.
	 */
	public void createGeometryElements() {
		geometry = spatialplugin.createGeometry();
		geometry.setCoordinateSystem(GeometryKind.cartesian);
		addCoordinates();
		addDomainTypes();
		addDomains();
		addAdjacentDomains();
		addGeometryDefinitions();
		addUnits();
	}

	/**
	 * Adds the geometry definitions.
	 */
//TODO determine to use compressed or uncompressed data
	public void addGeometryDefinitions() {
		SampledFieldGeometry sfg = geometry.createSampledFieldGeometry();
		sfg.setSpatialId("mySampledFieldGeometry");
		sfg.setIsActive(true);
		sfg.setSampledField("mySampledField");
		for (Entry<String, Integer> e : hashDomainTypes.entrySet()) {
			// if ((e.getValue() == 3 && depth > 2) || (e.getValue() == 2 &&
			// depth == 1)) {
			if (e.getValue() == 3) {
				SampledVolume sv = sfg.createSampledVolume();
				sv.setSpatialId(e.getKey());
				sv.setDomainType(e.getKey());
				sv.setSampledValue(hashSampledValue.get(e.getKey()));
				// sv.setMinValue(0); sv.setMaxValue(0);
			}
		}
		SampledField sf = geometry.createSampledField();
		sf.setSpatialId("mySampledField");
		sf.setDataType(DataKind.UINT8);
		// sf.setCompression(jsbmlConstants.SPATIAL_COMPRESSIONKIND_DEFLATED);
		sf.setCompression(CompressionKind.uncompressed);
		sf.setNumSamples1(width);
		sf.setNumSamples2(height);
		// if(depth > 1)
		sf.setNumSamples3(depth);
		sf.setInterpolation(InterpolationKind.nearestneighbor);
		// byte[] compressed = compressRawData(raw);
		// if (compressed != null)
		// sf.setSamples(byteArrayToIntArray(compressed),compressed.length);
		
		String s = Arrays.toString(byteArrayToIntArray(raw));
		s = s.replace("[", "");
		s = s.replace("]", "");
		s = s.replace(",", "");
		sf.setSamples(s);
		sf.setSamplesLength(raw.length);
	}

	/**
	 * Compress raw data.
	 *
	 * @param raw the raw
	 * @return the byte[]
	 */
	public byte[] compressRawData(byte[] raw) {
		Deflater compresser = new Deflater();
		compresser.setLevel(Deflater.BEST_COMPRESSION);
		compresser.setInput(raw);
		compresser.finish();
		int size;
		byte[] buffer = new byte[1024];
		byte[] compressed = null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		while (true) {
			size = compresser.deflate(buffer);
			stream.write(buffer, 0, size);
			if (compresser.finished()) {
				break;
			}
		}
		compressed = stream.toByteArray();
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		return compressed;
	}

	/**
	 * Byte array to int array.
	 *
	 * @param compressed the compressed
	 * @return the int[]
	 */
	public int[] byteArrayToIntArray(byte[] compressed) {
		int[] intArray = new int[compressed.length];
		for (int i = 0; i < compressed.length; i++) {
			intArray[i] = compressed[i] & 0xff;
		}
		return intArray;
	}

	/**
	 * Adds the adjacent domains.
	 */
	public void addAdjacentDomains() { // adds membrane domains and adjacents
		WeakHashMap<String, Integer> hashMembrane = new WeakHashMap<String, Integer>();
		for (ArrayList<String> e : adjacentsList) {
			String one = e.get(0).substring(0, e.get(0).length());
			one = one.replaceAll("[0-9]", "");
			String two = e.get(1).substring(0, e.get(1).length());
			two = two.replaceAll("[0-9]", "");
			//DomainType dt = geometry.getListOfDomainTypes().get(one + "_" + two + "_membrane");
			 DomainType dt = getDomainType(one + "_" + two + "_membrane");
			 
			if (hashMembrane.containsKey(dt.getSpatialId())) {
				hashMembrane.put(dt.getSpatialId(), hashMembrane.get(dt.getSpatialId()) + 1);
			} else {
				hashMembrane.put(dt.getSpatialId(), 0);
			}
			one = e.get(0).substring(0, e.get(0).length());
			two = e.get(1).substring(0, e.get(1).length());
			for (int i = 0; i < 2; i++) { // add info about adjacent domain
				AdjacentDomains adj = geometry.createAdjacentDomain();
				adj.setSpatialId(one + "_" + two + "_membrane_" + e.get(i));
				adj.setDomain1(dt.getSpatialId() + hashMembrane.get(dt.getSpatialId()));
				adj.setDomain2(e.get(i));
			}
		}
	}

	/**
	 * Adds the domains.
	 */
	public void addDomains() {
     for(Entry<String,Integer> e : hashDomainTypes.entrySet()){    			//add domains to corresponding domaintypes
    	 //DomainType dt = geometry.getListOfDomainTypes().get(e.getKey());
    	 DomainType dt = getDomainType(e.getKey());
 
 		
 		for (int i = 0; i < hashDomainNum.get(e.getKey()); i++) { // add each domain
			Domain dom = geometry.createDomain();
			String id = dt.getSpatialId() + i;
			dom.setSpatialId(id);
			dom.setDomainType(dt.getSpatialId());
			if (!dt.getSpatialId().matches(".*membrane")) {
				  InteriorPoint ip = dom.createInteriorPoint();
				  Point3d p = hashDomInteriorPt.get(id);
				  ip.setCoord1(p.x);
				  ip.setCoord2(p.y);
				  if(depth > 1) ip.setCoord3(p.z);  
				}
			}
     	}   
	}	

	// since the listOf getter refers to the id not spatial id for domain type
	public DomainType getDomainType(String id){
	   	 for(DomainType d: geometry.getListOfDomainTypes()){
    		 if(d.getSpatialId().equals(id)){
    			 return d;
    		 }
    	 }
		return null;
	}
	
  	/**
	   * Adds the domain types.
	   */
	public void addDomainTypes() {
		for (Entry<String, Integer> e : hashDomainTypes.entrySet()) {
			// DomainTypes
			DomainType dt = geometry.createDomainType();
			dt.setSpatialId(e.getKey());
			dt.setSpatialDimensions(e.getValue());
		
			// Compartment may need changes for name and id
			if(model.getListOfCompartments().get(e.getKey()) != null)
				continue;
			Compartment c = model.createCompartment();
			c.setSpatialDimensions(e.getValue());
			//c.setSpatialDimensions(3);
			c.setConstant(true);
			c.setId(e.getKey());
			c.setName(e.getKey());
			
			spatialcompplugin = (SpatialCompartmentPlugin) c.getPlugin("spatial");
			CompartmentMapping cm = new CompartmentMapping();
			cm.setSpatialId(e.getKey() + c.getId());
			cm.setDomainType(e.getKey());
			// TODO 
			//cm.setUnitSize(delta.x * delta.y * delta.z);
			cm.setUnitSize(1);

			spatialcompplugin.setCompartmentMapping(cm);
		}
	}

	/**
	 * Adds the coordinates.
	 */
	public void addCoordinates() { 
		CoordinateComponent ccx = geometry.createCoordinateComponent();
		ccx.setSpatialId("coordx");
		ccx.setType(CoordinateKind.cartesianX);
		if(unit != null) ccx.setUnits(unit);
		setCoordinateBoundary(ccx, "X", 0, width, delta.x);
		CoordinateComponent ccy = geometry.createCoordinateComponent();
		ccy.setSpatialId("coordy");
		ccy.setType(CoordinateKind.cartesianY);
		if(unit != null)  ccy.setUnits(unit);
		setCoordinateBoundary(ccy, "Y", 0, height, delta.y);
		if (depth > 1) {
			CoordinateComponent ccz = geometry.createCoordinateComponent();
			ccz.setSpatialId("coordz");
			ccz.setType(CoordinateKind.cartesianZ);
			if(unit != null) ccz.setUnits(unit);
			setCoordinateBoundary(ccz, "Z", 0, depth, delta.z);
		}
	}

	/**
	 * Sets the coordinate boundary.
	 *
	 * @param cc the cc
	 * @param s the s
	 * @param min the min
	 * @param max the max
	 * @param delta the delta
	 */
	//TODO fix bound?
	public void setCoordinateBoundary(CoordinateComponent cc, String s, double min, double max, double delta) { 
	  Boundary bmin = new Boundary();
	  //bmin.setId(s + "min"); bmin.setValue(min * delta);
	  bmin.setSpatialId(s + "min"); bmin.setValue(min);
	  Boundary bmax = new Boundary();
	  //bmax.setId(s + "max"); bmax.setValue(max * delta);
	  bmax.setSpatialId(s + "max"); bmax.setValue(max);
	  cc.setBoundaryMaximum(bmax);
	  cc.setBoundaryMinimum(bmin);
	}
  
	/**
	 * Adds the coord parameter.
	 */
	public void addCoordParameter() {
		ListOf<CoordinateComponent> lcc = geometry.getListOfCoordinateComponents();
		Parameter p;
		CoordinateComponent cc;
		for (int i = 0; i < lcc.size(); i++) {
			cc = (CoordinateComponent) lcc.get(i);
			p = model.createParameter();
			p.setId(cc.getSpatialId());
			p.setConstant(true);
			p.setValue(0d);
			SpatialParameterPlugin spp = (SpatialParameterPlugin) p.getPlugin("spatial");
			SpatialSymbolReference ssr = new SpatialSymbolReference();
			ssr.setSpatialRef(cc.getSpatialId());
			spp.setParamType(ssr);
		}
	}
  
	/**
	 * Gets the model.
	 *
	 * @return the model
	 */
	public Model getModel(){
		return model;
	}
	
	/**
	 * Gets the document.
	 *
	 * @return the document
	 */
	public SBMLDocument getDocument(){
		return document;
	}
	
	/**
	 * Adds the units.
	 */
	public void addUnits(){
		if(unit == null) 
			return; 
		UnitDefinition ud = model.createUnitDefinition();
		ud.setId("length");
		Unit u = ud.createUnit();
		u.setKind(Kind.METRE);
		u.setExponent(1d);
		u.setScale(0);
		u.setMultiplier(getUnitMultiplier(unit));
	
		ud = model.createUnitDefinition();
		ud.setId("area");
		u = ud.createUnit();
		u.setKind(Kind.METRE);
		u.setExponent(2d);
		u.setScale(0);
		u.setMultiplier(getUnitMultiplier(unit));
		
		ud = model.createUnitDefinition();
		ud.setId("volume");
		u = ud.createUnit();
		u.setKind(Kind.METRE);
		u.setExponent(3d);
		u.setScale(0);
		u.setMultiplier(getUnitMultiplier(unit));
	}

	/**
	 * @param unit2
	 * @return
	 */
	private double getUnitMultiplier(String unit) {
		if(unit.equals("um")) return 0.000001;
		return 1;
	}
	
	/**
	 * Creates the parametric.
	 *
	 * @param hashVertices the hash vertices
	 * @param hashBound the hash bound
	 */
	public void createParametric(HashMap<String, List<Point3d>> hashVertices, HashMap<String, Point3d> hashBound) {
	    geometry = spatialplugin.createGeometry();
	    geometry.setCoordinateSystem(GeometryKind.cartesian);
	    addCoordinates(hashBound);                        
	    addDomainTypes();                         
	    addDomains();                           
	    addAdjacentDomains();  
	    addParaGeoDefinitions(hashVertices, hashBound);    
	  }
  
	/**
	 * Adds the coordinates.
	 *
	 * @param hashBound the hash bound
	 */
	public void addCoordinates(HashMap<String, Point3d> hashBound) { 
		CoordinateComponent ccx = geometry.createCoordinateComponent();
		ccx.setSpatialId("x");
		ccx.setType(CoordinateKind.cartesianX);
		if(unit != null) ccx.setUnits(unit);
		setCoordinateBoundary(ccx, "X", hashBound.get("min").x, hashBound.get("max").x, delta.x);
		CoordinateComponent ccy = geometry.createCoordinateComponent();
		ccy.setSpatialId("y");
		ccy.setType(CoordinateKind.cartesianY);
		if(unit !=null) ccy.setUnits(unit);
		setCoordinateBoundary(ccy, "Y", hashBound.get("min").y, hashBound.get("max").y, delta.x);
		CoordinateComponent ccz = geometry.createCoordinateComponent();
		ccz.setSpatialId("z");
		ccz.setType(CoordinateKind.cartesianZ);
		if(unit !=null) ccz.setUnits(unit);
		setCoordinateBoundary(ccz, "Z", hashBound.get("min").z, hashBound.get("max").z, delta.x);
	}
	
	/**
	 * Adds the para geo definitions.
	 *
	 * @param hashVertices the hash vertices
	 * @param hashBound the hash bound
	 */
	public void addParaGeoDefinitions(HashMap<String, List<Point3d>> hashVertices, HashMap<String, Point3d> hashBound) {
		ParametricGeometry pg = geometry.createParametricGeometry();
		pg.setIsActive(true);
		pg.setSpatialId("ParametricGeometry");
		
		for (Entry<String, List<Point3d>> e : hashVertices.entrySet()) {
			List<Point3d> list = e.getValue();
			ArrayList<Point3d> uniquePointSet = new ArrayList<Point3d>(new LinkedHashSet<Point3d>(list));
			SpatialPoints sp = new SpatialPoints(PluginInfo.SBMLLEVEL, PluginInfo.SBMLVERSION);
			//sp.setId(e.getKey() + "_vertices");
			sp.setCompression(CompressionKind.uncompressed);
			addUniqueVertices(sp, uniquePointSet);
			pg.setSpatialPoints(sp);
			
			ParametricObject po = pg.createParametricObject();
			po.setCompression(CompressionKind.uncompressed);
			po.setDataType(DataKind.DOUBLE);
			po.setPolygonType(PolygonKind.triangle);
			po.setDomainType(e.getKey());
			po.setSpatialId(e.getKey() + "_polygon");
			setPointIndex(po, list, uniquePointSet);	
		}	
	}

	/**
	 * Adds the unique vertices.
	 *
	 * @param sp the sp
	 * @param uniquePointSet the unique point set
	 */
	public void addUniqueVertices(SpatialPoints sp, ArrayList<Point3d> uniquePointSet){
		Iterator<Point3d> pIt = uniquePointSet.iterator();
		int count = 0;
		double[] d = new double[uniquePointSet.size() *3];
		
		while(pIt.hasNext()){
			Point3d point = pIt.next();
			d[count++] = point.x;
			d[count++] = point.y;
			d[count++] = point.z;
		}	
		
		String s = Arrays.toString(d);
		s = s.replace("[", "");
		s = s.replace("]", "");
		s = s.replace(",", "");
		sp.setArrayData(s);
	}
	
	/**
	 * Sets the point index.
	 *
	 * @param po the po
	 * @param list the list
	 * @param uniquePointSet the unique point set
	 */
	public void setPointIndex(ParametricObject po, List<Point3d> list, ArrayList<Point3d> uniquePointSet) {
		int[] points = new int[list.size()];
		
		for(int i = 0 ; i < list.size() ; i++)
			points[i] = uniquePointSet.indexOf(list.get(i));

		String s = Arrays.toString(points);
		s = s.replace("[", "");
		s = s.replace("]", "");
		s = s.replace(",", "");
		po.setPointIndex(s);
	}
}

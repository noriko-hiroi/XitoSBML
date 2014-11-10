
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

import org.sbml.libsbml.AdjacentDomains;
import org.sbml.libsbml.BoundaryMax;
import org.sbml.libsbml.BoundaryMin;
import org.sbml.libsbml.Compartment;
import org.sbml.libsbml.CompartmentMapping;
import org.sbml.libsbml.CoordinateComponent;
import org.sbml.libsbml.Domain;
import org.sbml.libsbml.DomainType;
import org.sbml.libsbml.Geometry;
import org.sbml.libsbml.ImageData;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.RequiredElementsSBasePlugin;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.SBMLNamespaces;
import org.sbml.libsbml.SBasePlugin;
import org.sbml.libsbml.SampledField;
import org.sbml.libsbml.SampledFieldGeometry;
import org.sbml.libsbml.SampledVolume;
import org.sbml.libsbml.SpatialCompartmentPlugin;
import org.sbml.libsbml.SpatialModelPlugin;
import org.sbml.libsbml.SpatialPkgNamespaces;
import org.sbml.libsbml.libsbml;

/**
 *
 */

/**
 * @author Akira Funahashi
 *
 */
public class SpatialSBMLExporter {
  static {
    System.loadLibrary("sbmlj");                //read system library sbmlj
  }
  SBMLDocument document;
  Model model;
  SBMLNamespaces sbmlns;                       //class to store SBML Level, version, namespace
  SpatialPkgNamespaces spatialns;
  SpatialModelPlugin spatialplugin;
  SpatialCompartmentPlugin spatialcompplugin;
  RequiredElementsSBasePlugin reqplugin;
  Geometry geometry;
  HashMap<String, Integer> hashDomainTypes;     //store domain type with corresponding dimension
  HashMap<String, Integer> hashSampledValue;
  HashMap<String, Integer> hashDomainNum;
  ArrayList<ArrayList<String>> adjacentsList;
  byte[] raw;
  int matrix[];
  int width, height, depth;

  /**
   *
   */
  public SpatialSBMLExporter() {                    //builds the framework of SBML document
    sbmlns = new SBMLNamespaces(3,1);           //create SBML name space with level 3 version 1
    sbmlns.addPackageNamespace("req", 1);   //add required element package
    sbmlns.addPackageNamespace("spatial", 1);  //add spatial processes package
    // SBML Document
    document = new SBMLDocument(sbmlns);              //construct document with name space
    document.setPackageRequired("req", true);        //set req package as required
    document.setPackageRequired("spatial", true);    //set spatial package as required
    model = document.createModel();  //create model using the document and return pointer


    // Create Spatial
    //
    // set the SpatialPkgNamespaces for Level 3 Version 1 Spatial Version 1
    //
    spatialns = new SpatialPkgNamespaces(3, 1, 1);    //create spatial package name space
    //
    // Get a SpatialModelPlugin object plugged in the model object.
    //
    // The type of the returned value of SBase::getPlugin() function is SBasePlugin, and
    // thus the value needs to be casted for the corresponding derived class.
    //
    reqplugin = (RequiredElementsSBasePlugin)model.getPlugin("req");  //get required elements plugin
    reqplugin.setMathOverridden("spatial");                           //req set overridden as spatial
    reqplugin.setCoreHasAlternateMath(true);                          

    SBasePlugin basePlugin = (model.getPlugin ("spatial"));
    spatialplugin = (SpatialModelPlugin)basePlugin;                  //get spatial plugin
    if (spatialplugin == null) {
      System.err.println("[Fatal Error] Layout Extension Level " + spatialns.getLevel () + " Version " + spatialns.getVersion () + " package version " + spatialns.getPackageVersion () + " is not registered.");
      System.exit(1);
    }

  }

  public SpatialSBMLExporter(RawSpatialImage ri) {
    this();
    this.hashDomainTypes = ri.hashDomainTypes;
    this.hashSampledValue = ri.hashSampledValue;
    this.hashDomainNum = ri.hashDomainNum;
    this.raw = ri.raw;
    this.width = ri.width;
    this.height = ri.height;
    this.depth = ri.depth;
    this.adjacentsList = ri.adjacentsList;
  }

  public void createGeometryElements() {          //creates the components and geometry layer of SBML
    //
    // Creates a Geometry object via SpatialModelPlugin object.
    //
    geometry = spatialplugin.getGeometry();     //get geometry of spatial plugin
    geometry.setCoordinateSystem("Cartesian");  //set to Cartesian coordinate
    addCoordinates();                      
    addDomainTypes();                         
    addDomains();                           
    addAdjacentDomains();                       
    addGeometryDefinitions();                 
  }

  public void addGeometryDefinitions(){
    SampledFieldGeometry sfg = geometry.createSampledFieldGeometry();   //create new geometry definition and add to ListOfGeometryDefinitions list
    sfg.setSpatialId("mySampledField");                       //inherit from AbstractSpatialNamedSBase
    ListOf losg = sfg.getListOfSampledVolumes();              //get ListOfSampledVolumes

    for (Entry<String, Integer> e : hashDomainTypes.entrySet()) {
      if (e.getValue() == 3) {                                      //if dimensions is 3
        SampledVolume sv = new SampledVolume();
        sv.setSpatialId(e.getKey()); sv.setDomainType(e.getKey());
        sv.setSampledValue( hashSampledValue.get(e.getKey())); sv.setMinValue(0); sv.setMaxValue(0);
        losg.append(sv);
      }
    }

    SampledField sf = sfg.createSampledField();     //create SampleField represent number of coordinates in each
    sf.setSpatialId("imgtest"); sf.setDataType("integer");
    sf.setInterpolationType("linear"); sf.setEncoding("compressed");
    sf.setNumSamples1(width); sf.setNumSamples2(height); sf.setNumSamples3(depth);

    
    // need improvement
    ImageData idata = sf.createImageData();          //create ImageData
			byte[] compressed = compressRawData(raw);
			if (compressed != null) {
				idata.setSamples(byteArrayToIntArray(compressed),compressed.length); // see below byteArrayToIntArray
				idata.setDataType("compressed");
			}
  }

  public byte[] compressRawData(byte[] raw) {           //compression of image
    Deflater compresser = new Deflater();
    compresser.setLevel(Deflater.BEST_COMPRESSION);
    compresser.setInput(raw);
    compresser.finish();
    int size;
    byte[] buffer = new byte[1024];
    byte[] compressed = null;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    while(true) {
      size = compresser.deflate(buffer);              //insert compressed data
      stream.write(buffer, 0, size);                  //write compressed date to buffer
      if(compresser.finished()) {
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

  public int[] byteArrayToIntArray(byte[] compressed) {
    int[] intArray = new int[compressed.length];
    for (int i = 0; i < compressed.length; i++) {
      intArray[i] = compressed[i] & 0xff;
    }
    return intArray;
  }

  public void addAdjacentDomains() {		//adds membrane domains and adjacents
	  ListOf loadj = geometry.getListOfAdjacentDomains();
	  WeakHashMap<String, Integer> hashMembrane = new WeakHashMap<String,Integer>();   
	  for(ArrayList<String> e : adjacentsList){
		 String one = e.get(0).substring(0, e.get(0).length() - 1 );
		 String two = e.get(1).substring(0, e.get(1).length() - 1 );
		 DomainType dt = geometry.getDomainType(one + "_" + two + "_membrane");
		 if(hashMembrane.containsKey(dt.getSpatialId())){
			 hashMembrane.put(dt.getSpatialId(), hashMembrane.get(dt.getSpatialId()) + 1);
		 }else{
			 hashMembrane.put(dt.getSpatialId(), 0);
		 }
		 System.out.println(dt.getId());
		  for (int i = 0; i < 2; i++) {                           //add info about adjacent domain
			  AdjacentDomains adj = new AdjacentDomains();                    //adjacent domain only account for membrane cytosol+ extracelluar matrix and cytosol + nucleus
			  adj.setSpatialId(dt.getSpatialId() + "_" + e.get(i));
			  adj.setDomain1(dt.getSpatialId() + hashMembrane.get(dt.getSpatialId()));
			  adj.setDomain2(e.get(i));
			  loadj.append(adj);
		  }
	  }
	  hashMembrane = null;
  }

  public static int unsignedToBytes(byte b) {
	  return b & 0xFF;
  }

  public void addDomains() {
     ListOf lodom = geometry.getListOfDomains();
     
     for(Entry<String,Integer> e : hashDomainTypes.entrySet()){    			//add domains to corresponding domaintypes
 		DomainType dt = geometry.getDomainType(e.getKey());
		Domain dom = new Domain();
			if (dt.getSpatialId().matches(".*membrane")) {
				for (int i = 0; i < hashDomainNum.get(e.getKey()); i++) {
					dom.setSpatialId(dt.getSpatialId() + i);
					dom.setImplicit(true);
					lodom.append(dom);
				}
			} else {
				for (int i = 0; i < hashDomainNum.get(e.getKey()); i++) { // add each domain
					dom.setSpatialId(dt.getSpatialId() + i);
					dom.setImplicit(false);
					lodom.append(dom);
				}
			}
     }
     
  }

  public void addDomainTypes() {                        //create domain types, domain, compartment info
    ListOf lodt = geometry.getListOfDomainTypes();

    for (Entry<String, Integer> e : hashDomainTypes.entrySet()) {       //for each domain types
    	// DomainTypes
      DomainType dt = new DomainType();
      dt.setSpatialId(e.getKey()); dt.setSpatialDimensions(e.getValue());
      lodt.append(dt);
      // Compartment								may need changes for name and id
      Compartment c = model.createCompartment();
      c.setSpatialDimensions(e.getValue());
      c.setConstant(true);                      //set compartment as a constant
      c.setId(e.getKey()); c.setName(e.getKey());

      spatialcompplugin = (SpatialCompartmentPlugin)c.getPlugin("spatial");   //create compartment mapping which relates compartment and domain type
      CompartmentMapping cm = spatialcompplugin.getCompartmentMapping();
      cm.setSpatialId(e.getKey()+c.getId());
      cm.setCompartment(c.getId());
      cm.setDomainType(e.getKey());
      cm.setUnitSize(1);
    }
  }

  public void addCoordinates() {                                //add coordinates x and y
    ListOf lcc = geometry.getListOfCoordinateComponents();
    CoordinateComponent ccx = new CoordinateComponent(spatialns);
    CoordinateComponent ccy = new CoordinateComponent(spatialns);
    CoordinateComponent ccz = new CoordinateComponent(spatialns);
    ccx.setSpatialId("x"); ccx.setComponentType("cartesianX"); ccx.setIndex(0); ccx.setSbmlUnit("um");    //setIndex, micrometer
    ccy.setSpatialId("y"); ccy.setComponentType("cartesianY"); ccy.setIndex(1); ccy.setSbmlUnit("um");
    ccz.setSpatialId("z"); ccz.setComponentType("cartesianZ"); ccz.setIndex(2); ccz.setSbmlUnit("um");
    setCoordinateBoundary(ccx, "X", 0, width);
    setCoordinateBoundary(ccy, "Y", 0, height);
    setCoordinateBoundary(ccz, "Z", 0, depth);
    lcc.append(ccx);                                //add coordinate x to listOfCoordinateComponents
    lcc.append(ccy);                               //add coordinate y to listOfCoordinateComponents
    lcc.append(ccz);                               //add coordinate z to listOfCoordinateComponents
  }

  public void setCoordinateBoundary(CoordinateComponent cc, String s, double min, double max) {         //set coordinate boundaries
    if (cc.getBoundaryMin() == null) cc.setBoundaryMin(new BoundaryMin(spatialns));
    if (cc.getBoundaryMax() == null) cc.setBoundaryMax(new BoundaryMax(spatialns));
    cc.getBoundaryMin().setSpatialId(s+"min");
    cc.getBoundaryMin().setValue(min);
    cc.getBoundaryMax().setSpatialId(s+"max");
    cc.getBoundaryMax().setValue(max);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
	int width  = 5, height = 5, depth = 3;
    HashMap<String, Integer> hashDomainTypes = new HashMap<String, Integer>();
    hashDomainTypes.put("EC", 3);
    hashDomainTypes.put("Nuc", 3);
    hashDomainTypes.put("Cyt", 3);
    hashDomainTypes.put("Cyt_EC_membrane", 2);
    hashDomainTypes.put("Nuc_Cyt_membrane", 2);
    HashMap<String, Integer> hashSampledValue = new HashMap<String, Integer>();
    hashSampledValue.put("EC", 0);
    hashSampledValue.put("Nuc", 1);
    hashSampledValue.put("Cyt", 2);
    HashMap<String,Integer> hashDomainNum = new HashMap<String,Integer>();
    hashDomainNum.put("EC", 1);
    hashDomainNum.put("Nuc", 1);
    hashDomainNum.put("Cyt", 1);
    hashDomainNum.put("Cyt_EC_membrane", 1);
    hashDomainNum.put("Nuc_Cyt_membrane", 1);
    ArrayList<ArrayList<Integer>> adjacentPixel = new ArrayList<ArrayList<Integer>>();
    ArrayList<Integer> temp = new ArrayList<Integer>();
    temp.add(0,1);
    adjacentPixel.add(temp);
    temp = new ArrayList<Integer>();
    adjacentPixel.add(temp);
    
    ArrayList<ArrayList<String>> adjacentsList = new ArrayList<ArrayList<String>>();
    ArrayList<String> sss = new ArrayList<String>();
    sss.add("Cyt0");
    sss.add("EC0");
    adjacentsList.add(sss);
    sss = new ArrayList<String>();
    sss.add("Nuc0");
    sss.add("Cyt0");
    adjacentsList.add(sss);
    
    System.out.print(adjacentsList.toString());
    
    /*
    byte[] raw = { 
         0,1,1,1,0,
         1,1,2,1,1,
         1,2,2,2,1,
         1,1,2,1,1,
         0,1,1,1,0
    };
    */
    byte[] len = { 
	         0,1,1,1,0,
	         1,1,2,1,1,
	         1,2,2,2,1,
	         1,1,2,1,1,
	         0,1,1,1,0
	    };		
    byte[] raw = null;
    
    for(int i = 0; i < 3 ; i++){
    	System.arraycopy(len, 0, raw, i * 25, 25);
    }
    
    RawSpatialImage ri = new RawSpatialImage(raw, width, height, depth, hashDomainTypes, hashSampledValue, hashDomainNum, adjacentsList);  //why does the length need to be squarerooted?
    SpatialSBMLExporter ts = new SpatialSBMLExporter(ri);
    ts.createGeometryElements();
    System.out.println(ts.document.getModel().getId());
    libsbml.writeSBMLToFile(ts.document, "out2.xml");
  }

}

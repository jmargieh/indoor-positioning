package main.java.com.indoor.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import main.java.com.indoor.helpers.CustomComparator;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.GeometricShapeFactory;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {
	
	final double gridMinX1 = -417.0;
	final double gridMaxX2 = 305.0;
	final double gridMinY1 = -72.0;
	final double gridMaxY2 = 82.0;
	final double gridSize = 10.0;
	
	//points json file
	private InputStream pointsInputStream = IndoorWebService.class.getResourceAsStream("/json/points.json");
	//obstacles json file - generally polygons
	private InputStream obstaclesInputStream = IndoorWebService.class.getResourceAsStream("/json/obstacles.json");
	//points arrayList initialization
	private List<SimpleFeature> pointsArray = new ArrayList<>();
	// obstacles arrayList initialization
	private List<SimpleFeature> obstaclesArray = new ArrayList<>();
	// points located on obstacles
	private List<SimpleFeature> pointsInObstaclesArray = new ArrayList<>();
	// HashMap with key:deviceid & SimpleFeature points List sorted by timestamp.
	private HashMap<String, List<SimpleFeature>> deviceMap = new HashMap<String, List<SimpleFeature>>();
	// HashMap with key:deviceid & LineString: the device path
	private HashMap<String, LineString> devicePathMap = new HashMap<String, LineString>();
	
	//private ArrayList<? extends Geometry> pointsArray;
	//private ArrayList<? extends Geometry> obstaclesArray;

	// close Input Streams
	private void closeInputStreams() throws IOException {
		pointsInputStream.close();
		obstaclesInputStream.close();
	}

	// initializating pointsArray and obstaclesArray
	public void initArrayLists() {

		SimpleFeature feature;
		FeatureJSON io = new FeatureJSON();
		//Geometry g;
		// get points.json as InputStream
		try {
			Reader rd = GeoJSONUtil.toReader(this.pointsInputStream);
			FeatureIterator<SimpleFeature> features = io.streamFeatureCollection(rd);
			while (features.hasNext()) {
				feature = features.next();
				//g = (Geometry) feature.getAttribute("geometry");
				this.pointsArray.add(feature);
			}

			rd = GeoJSONUtil.toReader(this.obstaclesInputStream);
			features = io.streamFeatureCollection(rd);
			while (features.hasNext()) {
				feature = features.next();
				//g = (Geometry) feature.getAttribute("geometry");
				this.obstaclesArray.add(feature);
			}
			
			closeInputStreams();

		} catch (IOException e) {
			System.err.print(e.getMessage());
		}

	}

	@GET
	@Path("/points")
	public Response isPointInObstacle() throws IOException {

		initArrayLists();

		for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();) {
			Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
			for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {
				SimpleFeature simplefeature = pIterator.next();
				if (obstacle.contains((Geometry)simplefeature.getAttribute("geometry"))) {
					// remove the point from pointsArray
					//pIterator.remove();
					// add the problematic point to pointsInObstaclesArray
					pointsInObstaclesArray.add(simplefeature);
				}

			}
		}
		
		putPointsIntoDeviceHashMap();
		createDevicesPaths();
		createGridMap();
		
		
		String Res="";
		for (LineString line : this.devicePathMap.values()) {
		    boolean flag = isPathCrossingObstacle(line);
		    if(flag){
		    	Res += "true | ";
		    }else{
		    	Res += "false |";
		    }
		    
		}
		
		
		return Response.status(200).entity(Res + "   -----If reached here we succeed!").build();

	}
	
	
	private void putPointsIntoDeviceHashMap() {
		
		for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {
			SimpleFeature simplefeature = pIterator.next();
			if( this.deviceMap.containsKey(simplefeature.getID()) == false ) {
				List<SimpleFeature> deviceArray = new ArrayList<>();
				deviceArray.add(simplefeature);
				this.deviceMap.put(simplefeature.getID(), deviceArray);
			}else {
				this.deviceMap.get(simplefeature.getID()).add(simplefeature);
			}
		}
		sortDeviceHashMapListByTimeStamp();
	}
	
	
	private void sortDeviceHashMapListByTimeStamp() {
		
		for (List<SimpleFeature> list : this.deviceMap.values()) {
		    Collections.sort(list,new CustomComparator());
		}
	}
	
	
	// gets something like (Geometry)pIterator.next().getAttribute("geometry")) 
	private double calculateDistance(Geometry p1, Geometry p2) {
		return p1.distance(p2);
	}
	
	// gets something like (Geometry)pIterator.next().getAttribute("geometry"))
	private Geometry createCircle(Geometry p1, double radius) {
		
		double xCoordinate = p1.getCoordinate().x;
		double yCoordinate = p1.getCoordinate().y;
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(32);
		shapeFactory.setCentre(new Coordinate(xCoordinate, yCoordinate));
		shapeFactory.setSize(radius * 2);
		return shapeFactory.createCircle();
	}
	
	// TODO : construct a lineString from given set of points
	private void createDevicesPaths() {
		
		int counter = 0;
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		
		for (List<SimpleFeature> list : this.deviceMap.values()) {
			Coordinate[] coords  = new Coordinate[list.size()];
			for (Iterator<SimpleFeature> pIterator = list.iterator(); pIterator.hasNext();) {
				Geometry g = (Geometry)pIterator.next().getAttribute("geometry");
				double xCoordinate = g.getCoordinate().x;
				double yCoordinate = g.getCoordinate().y;
				coords[counter++] = new Coordinate(xCoordinate, yCoordinate);
			}
			counter = 0;
			LineString line = geometryFactory.createLineString(coords);
			this.devicePathMap.put(list.get(0).getID(), line);
		}
		
	}
	
	// checks if a path/Stringline is crossing an obstacle.
	private boolean isPathCrossingObstacle(Geometry line) {
		for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();){
			Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
			if(line.crosses(obstacle)){
				return true;
			}
		}
		return false;
	}
	
	// creates a Grid Map : Reference : http://docs.geotools.org/latest/userguide/extension/grid.html
	// each feature is a polygon of size 10x10 
	private void createGridMap() {
		
		try{
			ReferencedEnvelope gridBounds = new ReferencedEnvelope(this.gridMinX1, this.gridMaxX2, this.gridMinY1, this.gridMaxY2, DefaultGeographicCRS.WGS84);
			SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, gridSize);
			SimpleFeatureCollection collection = grid.getFeatures();
			
			double matrixRows = Math.floor( (this.gridMaxY2  - this.gridMinY1)/this.gridSize );
			double matrixCols = Math.floor( (this.gridMaxX2  - this.gridMinX1)/this.gridSize );
			
			if(matrixCols*matrixRows != collection.size()){
				Exception e = new Exception("matrix Rows and cols don't fit with SimpleFeatureCollection collection Grid Size");
				throw e;
			}
			
			// if reached here that means everything is OK with the collection size and Matrix to be allocated is fine
			SimpleFeature gridMapMatrix[][] = new SimpleFeature[(int)matrixCols][(int)matrixRows];
			SimpleFeatureIterator iterator=collection.features();
			try {
				
				for(int i=(int) (matrixRows-1) ; i>0 ; i++){
					for(int j=0 ; j<(int) (matrixCols) && iterator.hasNext() ; j++){
						SimpleFeature feature = iterator.next();
						gridMapMatrix[i][j] = feature;
					}
				}
			}
			finally {
				iterator.close();
				}
			}catch(IOException e){
				System.err.println( e.getMessage() );
			} catch (Exception e) {
				System.err.println( e.getMessage() );
			}
		
	}
	
}

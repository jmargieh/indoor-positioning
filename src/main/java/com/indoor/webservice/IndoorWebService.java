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

import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
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
	//
	private HashMap<String, List<SimpleFeature>> deviceMap = new HashMap<String, List<SimpleFeature>>();
	
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
		
		return Response.status(200).entity("If reached here we succeed!").build();

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
		
		for (List<SimpleFeature> value : this.deviceMap.values()) {
		    Collections.sort(value,new CustomComparator());
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
	private Geometry createLineString() {
		
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Coordinate[] coords  = new Coordinate[] {new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(8, 6) };
		LineString line = geometryFactory.createLineString(coords);
		return line;
	}
	
	// checks if a path/Stringline is crossing an obstacle.
	private boolean isPathCrossingObstacle(Geometry line) {
		return true;
	}
	
	// TODO : will need a method that takes the array points and sort it by time
	// TODO : will need an arrayList of array Lists, each cell will have an arrayList of the broadcasting device.

}

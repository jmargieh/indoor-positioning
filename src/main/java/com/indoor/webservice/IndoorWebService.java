package main.java.com.indoor.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import main.java.com.indoor.helpers.CustomComparator;
import main.java.com.indoor.helpers.CustomLineString;
import main.java.com.indoor.helpers.GridSquare;
import main.java.com.indoor.helpers.CustomLineString.Direction;

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
import com.vividsolutions.jts.geom.Point;


@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {
	
	//space coords
	final double gridMinX1 = -417.0;
	final double gridMaxX2 = 305.0;
	final double gridMinY1 = -72.0;
	final double gridMaxY2 = 82.0;
	// space coords
	
	final double gridSize = 10.0;
	
	final int minimumNumberOfSamples = 3;
	
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
	// GridMap Matrix Reference : http://docs.geotools.org/latest/userguide/extension/grid.html
	private GridSquare gridMapMatrix[][];

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

		/*
		 * puts points in pointsArray
		 * puts obstacles in obstaclesArray
		 */
		initArrayLists();

		// loop over obstacles array and remove the point from the pointsArray if it's in obstacles
		// and add it to pointsInObstaclesArray.
		for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();) {
			Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
			for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {
				SimpleFeature simplefeature = pIterator.next();
				if (obstacle.contains((Geometry)simplefeature.getAttribute("geometry"))) {
					// remove the point from pointsArray
					// we don't need this point since in the n+1 device we need to relocate the point
					pIterator.remove();
					// add the problematic point to pointsInObstaclesArray
					pointsInObstaclesArray.add(simplefeature);
				}

			}
		}
		
		putPointsIntoDeviceHashMap();
		//createDevicesPaths(); TODO : look at function dif
		createGridMap();
		deleteCustomLineStringCrossingObstacle();
		updateRateAndPointsGridSquaresNew();
		updateRateCustomLineInGrid();
		
		return Response.status(200).entity("   -----If reached here we succeed!").build();

	}
	
	/*
	 * // this.deviceMap = HashMap with key:deviceid & SimpleFeature points List sorted by timestamp.
	 */
	private void putPointsIntoDeviceHashMap() {
		
		for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {
			SimpleFeature simplefeature = pIterator.next();
			// if device id dosen't exist, create deviceArray with the current point
			// and add it to the this.deviceMap HashMap.
			// else add the point to the existed deviceArray, by the ID
			if( this.deviceMap.containsKey(simplefeature.getID()) == false ) {
				List<SimpleFeature> deviceArray = new ArrayList<>();
				deviceArray.add(simplefeature);
				this.deviceMap.put(simplefeature.getID(), deviceArray);
			}else {
				this.deviceMap.get(simplefeature.getID()).add(simplefeature);
			}
		}
		// remove devices with less than minimumSamples
		removeDevicesWithLessThanMinimumSamples();
		// loop over the HashMap values (ArrayList) and sort it by device's timestamp. 
		sortDeviceHashMapListByTimeStamp();
	}
	
	/*
	 * loop over deviceMap, and remove devices with less than minimum number of samplings minimumNumberOfSamples.
	 */
	private void removeDevicesWithLessThanMinimumSamples() {
        Iterator<Map.Entry<String, List<SimpleFeature>>> iterator = this.deviceMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, List<SimpleFeature>> entry = iterator.next();
            if(entry.getValue().size() < this.minimumNumberOfSamples){
            	iterator.remove();
            }
        }
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
	
	// construct a lineString from given set of points and save it into a devicePathMap
	// key : deviceID    value: LineString (costructed from the device's points)
	// TODO : will need to modify this function to give path from  the start and end of each lineString. 
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
	
	/*
	 * update the GridSquare rate
	 * rate : number of points in the GridSquare.
	 */
	private void updateRateGridSquares(){
		for(int i=0;i<this.gridMapMatrix.length;i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {
					SimpleFeature simplefeature = pIterator.next();
					if( ((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).contains((Geometry)simplefeature.getAttribute("geometry")) ){
						gridMapMatrix[i][j].setRate(gridMapMatrix[i][j].getRate()+1);
					}
				}

			}
		}
	}
	
	
	private void updateRateAndPointsGridSquaresNew() {
		for(int i=0;i<this.gridMapMatrix.length;i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				Iterator<Map.Entry<String, List<SimpleFeature>>> iterator = this.deviceMap.entrySet().iterator();
				while(iterator.hasNext()){
					Map.Entry<String, List<SimpleFeature>> entry = iterator.next();
					for (Iterator<SimpleFeature> pIterator = entry.getValue().iterator(); pIterator.hasNext();) {
						SimpleFeature simplefeaturePoint = pIterator.next();
						if( ((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).contains((Geometry)simplefeaturePoint.getAttribute("geometry")) ){
							gridMapMatrix[i][j].setRate(gridMapMatrix[i][j].getRate()+1);
							gridMapMatrix[i][j].addPointToGridSquare(simplefeaturePoint);
						}
					}
				}
			}
		}
	}
	
	
	/*
	 * update the rate of the CustomLineString, according to it's direction.
	 */
	private void updateRateCustomLineInGrid(){
		for(int i=0; i<this.gridMapMatrix.length; i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				List<CustomLineString> customLineStringArray = this.gridMapMatrix[i][j].getLineStringArray();
				for (Iterator<CustomLineString> cIterator = customLineStringArray.iterator(); cIterator.hasNext();){
					CustomLineString cls = cIterator.next();
					if(cls.getDirection() == Direction.UP){
						cls.setRate(this.gridMapMatrix[i-1][j].getRate());
					}
					if(cls.getDirection() == Direction.RIGHT){
						cls.setRate(this.gridMapMatrix[i][j+1].getRate());
					}
					if(cls.getDirection() == Direction.DOWN){
						cls.setRate(this.gridMapMatrix[i+1][j].getRate());
					}
					if(cls.getDirection() == Direction.LEFT){
						cls.setRate(this.gridMapMatrix[i][j-1].getRate());
					}
					
				}
			}
		}
	}
	
	/*
	 * loop over the customLineStringArray of each GridSquare in gridMapMatrix, and remove lineString crossing obstacles.
	 */
	private void deleteCustomLineStringCrossingObstacle() {
		boolean isListChanged;
		for (int i = 0 ; i < this.gridMapMatrix.length ; i++) {
			for(int j=0 ; j<this.gridMapMatrix[i].length; j++ ){
				isListChanged = false;
				List<CustomLineString> customLineStringArray = this.gridMapMatrix[i][j].getLineStringArray();
				for (Iterator<CustomLineString> cIterator = customLineStringArray.iterator(); cIterator.hasNext();) {
					Geometry line = (Geometry)cIterator.next().getLine();
					if(isPathCrossingObstacle(line)){
						cIterator.remove();
						isListChanged = true;
					}
				}
				if(isListChanged){
					this.gridMapMatrix[i][j].setLineStringArray(customLineStringArray);
				}
			}
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
	
	private Geometry createLineString(Point p1, Point p2){
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Coordinate[] coords = new Coordinate[] {new Coordinate(p1.getCoordinate().x, p1.getCoordinate().y), new Coordinate(p2.getCoordinate().x, p2.getCoordinate().y)};
		LineString line = geometryFactory.createLineString(coords);
		return line;
	}
	

	
	// creates a Grid Map : Reference : http://docs.geotools.org/latest/userguide/extension/grid.html
	// each feature is a polygon of size 10x10 (gridSize x gridSize)
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
			// create GridSquareMatrix
			this.gridMapMatrix = new GridSquare[(int)matrixRows][(int)matrixCols];
			// get the features
			SimpleFeatureIterator iterator=collection.features();
			try {
				// loop over the matrix and initialize the GridMapMatrix
				for(int i=(int) (matrixRows-1) ; i>=0 ; i--){
					for(int j=0 ; j<(int) (matrixCols) && iterator.hasNext() ; j++){
						SimpleFeature feature = iterator.next();
						this.gridMapMatrix[i][j] = new GridSquare(Integer.toString(i)+"-"+Integer.toString(j), feature,0);
					}
				}
			}finally {
				iterator.close();
				}
			
				/*
				 * loop over the gridMapMatrix, and foreach Polygon g, get the centeroidPoint
				 * and create a LineString between the current Polygon and it's neighbors.
				 * |---------|
				 * |         |
				 * |    *    |    * -> the centroid point of the polygon connected with the neighbors
				 * |         |         from the right,left,up and down.
				 * |---------|
				 */
				for(int i=(int) (matrixRows-1) ; i>=0 ; i--){
					for(int j=0 ; j<(int) (matrixCols) ; j++){
						
						Geometry g = (Geometry)this.gridMapMatrix[i][j].getSquare().getAttribute("element");
						Point centroidPoint = g.getCentroid();
						Geometry line = null;
						
						if(j > 0) // if not most left column then compute lineString to left
						{
							Geometry gLeft = (Geometry)this.gridMapMatrix[i][j-1].getSquare().getAttribute("element");
							Point centroidPointLeft = gLeft.getCentroid();
							line = createLineString(centroidPoint, centroidPointLeft);
							this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"LEFT"));
						}
						if(j < (int) (matrixCols - 1)) // if not most right column then compute lineString to right
						{
							Geometry gRight = (Geometry)this.gridMapMatrix[i][j+1].getSquare().getAttribute("element");
							Point centroidPointRight = gRight.getCentroid();
							line = createLineString(centroidPoint, centroidPointRight);
							this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"RIGHT"));
							
						}
						
						if(i > 0) // if not most upper row
						{
							Geometry gUp = (Geometry)this.gridMapMatrix[i-1][j].getSquare().getAttribute("element");
							Point centroidPointUp = gUp.getCentroid();
							line = createLineString(centroidPoint, centroidPointUp);
							this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"UP"));
						}
						if(i < (int)(matrixRows - 1)) // if not last row
						{
							Geometry gDown = (Geometry)this.gridMapMatrix[i+1][j].getSquare().getAttribute("element");
							Point centroidPointDown = gDown.getCentroid();
							line = createLineString(centroidPoint, centroidPointDown);
							this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"DOWN"));
						}
						
						
						//GridSquare[i][j] = feature; // to be modified add new(..,..,..,..);
					}
				}
			
			}catch(IOException e){
				System.err.println("error IO:" + e.getMessage() );
			} catch (Exception e) {
				System.err.println("error Exception:" + e.getMessage() );
			}
		
	}
	
}

package main.java.com.indoor.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import main.java.com.indoor.helpers.CustomComparator;
import main.java.com.indoor.helpers.CustomLineString;
import main.java.com.indoor.helpers.GridSquare;
import main.java.com.indoor.helpers.CustomLineString.Direction;
import main.java.com.indoor.helpers.SuperSimpleFeaturePoint;

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
	
	final double distanationLineRateWeight = 0.4;
	final double sourceLineRateWeight = 0.6; 
	
	final double gridSize = 10.0;
	
	final int minimumNumberOfSamples = 3;
	
	//points json file
	private InputStream pointsInputStream = IndoorWebService.class.getResourceAsStream("/json/points.json");
	//obstacles json file - generally polygons
	private InputStream obstaclesInputStream = IndoorWebService.class.getResourceAsStream("/json/obstacles.json");
	
	// Device n+1 (the device we get after we did the pre-processing)
	private InputStream deviceInputStream = IndoorWebService.class.getResourceAsStream("/json/device.json");
	// new device id
	private String newDeviceId;
	// new device points array
	private List<SimpleFeature> newDevicePointsArray = new ArrayList<>();
	// HashMap with key:deviceid & SimpleFeature points List sorted by timestamp.
	private HashMap<String, List<SuperSimpleFeaturePoint>> deviceSuperSimpleFeaturePointMap = new HashMap<String, List<SuperSimpleFeaturePoint>>();
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
		deviceInputStream.close();
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
			
			rd = GeoJSONUtil.toReader(this.deviceInputStream);
			features = io.streamFeatureCollection(rd);
			while (features.hasNext()) {
				feature = features.next();
				//g = (Geometry) feature.getAttribute("geometry");
				this.newDeviceId = feature.getID();
				this.newDevicePointsArray.add(feature);
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
					//pIterator.remove();
					// add the problematic point to pointsInObstaclesArray
					pointsInObstaclesArray.add(simplefeature);
				}

			}
		}
		
		// adds the new device points to the point array
		// this will not remove points in obstacles because we need to relocate them.
		this.pointsArray.addAll(newDevicePointsArray);
		
		
		putPointsIntoDeviceHashMap();
		//createDevicesPaths(); TODO : look at function dif
		createGridMap();
		deleteCustomLineStringCrossingObstacle();
		updateRateAndPointsGridSquaresNew();
		updateRateCustomLineInGrid();
		updateGridSquaresWithinObstacles();
		initDeviceSuperSimpleFeaturePointMap(); // a duplicate 
		reLocatePointsInObstacle();
		
		//findAllPosibblePaths(2, 5);
		
		return Response.status(200).entity("   -----If reached here we succeed!").build();

	}
	
	/*
	 * need to loop over deviceMap and covert it into DeviceSuperSimpleFeaturePointMap
	 * (The same map with different class) -> this is done in order to user same heuristic function
	 * and retreive indexes i,j in future automaticaly
	 */
	private void initDeviceSuperSimpleFeaturePointMap() {
		Iterator<Map.Entry<String, List<SimpleFeature>>> iterator = this.deviceMap.entrySet().iterator();
		Map.Entry<String, List<SimpleFeature>> entry;
		SimpleFeature simplefeature;
		int [] indexes;
        while(iterator.hasNext()){
            entry = iterator.next();
            List<SuperSimpleFeaturePoint> deviceSuperSimpleFeatureArray = new ArrayList<>();
            this.deviceSuperSimpleFeaturePointMap.put(entry.getKey(), deviceSuperSimpleFeatureArray);
            // loop over device points List
            for (Iterator<SimpleFeature> pIterator = entry.getValue().iterator(); pIterator.hasNext();) {
				simplefeature = pIterator.next();
				indexes = getPointIndexesInGridSquare(simplefeature);
				this.deviceSuperSimpleFeaturePointMap.get(entry.getKey()).add(new SuperSimpleFeaturePoint(simplefeature,indexes[0],indexes[1]));
			}
        }	
	}
	
	private void reLocatePointsInObstacle() {
		Iterator<Map.Entry<String, List<SuperSimpleFeaturePoint>>> iterator = this.deviceSuperSimpleFeaturePointMap.entrySet().iterator();
		Map.Entry<String, List<SuperSimpleFeaturePoint>> entry;
		SuperSimpleFeaturePoint superSimpleFeature;
		int[] newIndexes;
        while(iterator.hasNext()){
            entry = iterator.next();
            for (int i=0; i<entry.getValue().size(); i++ ) {
            	superSimpleFeature = entry.getValue().get(i);
            	if(isPointInObstacle(superSimpleFeature.getSimpleFeaturePoint())) {
    				newIndexes = heuristicCalculation(superSimpleFeature,i, entry.getKey());
    				superSimpleFeature.setRowIndex(newIndexes[0]);
    				superSimpleFeature.setColumnIndex(newIndexes[1]);
    			}
            }
        }
		/*
		for (int i = 0; i < this.deviceMap.size(); i++) {
			NewDeviceSimpleFeaturePoint ndsfp = this.newDeviceSimpleFeaturePointArray.get(i);
			if(isPointInObstacle(ndsfp.getSimpleFeaturePoint())) {
				int[] newIndexes = heuristicCalculation(ndsfp,i);
				ndsfp.setRowIndex(newIndexes[0]);
				ndsfp.setColumnIndex(newIndexes[1]);
			}
		}
		*/
	}
	
	/*
	 * returns the indexes of GridSquare Matrix, where to relocate the point
	 */
	private int[] heuristicCalculation(SuperSimpleFeaturePoint ndsfp, int index, String deviceId) {
		int [] indexes =  {ndsfp.getRowIndex(),ndsfp.getColumnIndex()};
		double res, max=0;
		int startColumn = ndsfp.getColumnIndex(),startRow = ndsfp.getRowIndex(),endColumn = ndsfp.getColumnIndex(),endRow = ndsfp.getRowIndex();
		
		if(ndsfp.getRowIndex() > 0 ) {
			startRow = ndsfp.getRowIndex() -1 ;
		}
		if(ndsfp.getRowIndex() < this.gridMapMatrix.length) {
			endRow = ndsfp.getRowIndex() + 1;
		}
		if(ndsfp.getColumnIndex() > 0 ) {
			startColumn = ndsfp.getColumnIndex() -1 ;
		}
		if(ndsfp.getColumnIndex() < this.gridMapMatrix[0].length) {
			endColumn = ndsfp.getColumnIndex() + 1;
		}
		
		for(int i= startRow; i<=endRow; i++) {
			for (int j = startColumn; j<=endColumn; j++){
				if( i!= ndsfp.getRowIndex() || j!= ndsfp.getColumnIndex() ){
					Geometry g = (Geometry)this.gridMapMatrix[i][j].getSquare().getAttribute("element");
					Point centroidPoint = g.getCentroid();
					if(index+1 < this.deviceSuperSimpleFeaturePointMap.get(deviceId).size()) {
						double distance = calculateDistance(centroidPoint,(Geometry)this.deviceSuperSimpleFeaturePointMap.get(deviceId).get(index+1).getSimpleFeaturePoint().getAttribute("geometry"));
						res = (1/distance) + gridMapMatrix[i][j].getRate();
						if(res > max) {
							max = res;
							indexes[0] = i;
							indexes[1] = j;
						}
					}
				}
			}
		}
		
		return indexes;
	}
	
	private boolean isPointInObstacle(SimpleFeature point) {
		for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();){
			Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
			if (obstacle.contains((Geometry)point.getAttribute("geometry"))) {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * get indexes of the GridSquare that cointains the simplefeaturePoint
	 */
	private int[] getPointIndexesInGridSquare(SimpleFeature simplefeaturePoint) {
		int [] indexes = new int[2];
		for(int i=0;i<this.gridMapMatrix.length;i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				if( ((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).contains((Geometry)simplefeaturePoint.getAttribute("geometry")) ){
					indexes[0] = i;
					indexes[1] = j;
					return indexes;
				}
			}
		}
		// if reached here the point is completely outside of the space. Let's hope there's not points out of space :)
		return null;
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
		double sourceLineRate;
		for(int i=0; i<this.gridMapMatrix.length; i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				List<CustomLineString> customLineStringArray = this.gridMapMatrix[i][j].getLineStringArray();
				for (Iterator<CustomLineString> cIterator = customLineStringArray.iterator(); cIterator.hasNext();){
					CustomLineString cls = cIterator.next();
					sourceLineRate = this.gridMapMatrix[i][j].getRate();
					if(cls.getDirection() == Direction.UP){
						cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i-1][j].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
					}
					if(cls.getDirection() == Direction.RIGHT){
						cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i][j+1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
					}
					if(cls.getDirection() == Direction.DOWN){
						cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i+1][j].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
					}
					if(cls.getDirection() == Direction.LEFT){
						cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i][j-1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
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
	
	/**
	 * the function will update IsInObstacle to true if the GridSquare intersects with an obstacle.
	 */
	private void updateGridSquaresWithinObstacles() {
		for(int i=0;i<this.gridMapMatrix.length;i++){
			for(int j=0;j<this.gridMapMatrix[i].length; j++){
				for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();) {
					SimpleFeature simplefeature = oIterator.next();
					if( ((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).intersects((Geometry)simplefeature.getAttribute("geometry")) ){
						gridMapMatrix[i][j].setIsInObstacle(true);
					}
				}

			}
		}
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
	

	/*
	 *  x is number of 1's - right or left
	 *  n is the length of the path.
	 *  return a binary string which defines the paths from source point to the destination point
	 */
	private List<String> findAllPosibblePaths(int x, int n) {

		List<String> possiblePaths = new ArrayList<>();
		
	    Set<BigInteger> result = new LinkedHashSet<>();
	    for (int j = x; j > 0; j--) {
	        Set<BigInteger> a = new LinkedHashSet<>();

	        for (int i = 0; i < n - j + 1; i++) {
	            if (j == x) {
	                a.add(BigInteger.ZERO.flipBit(i));
	            } else {
	                for (BigInteger num : result) {
	                    if (num != null && !num.testBit(i) && (i >= (n - j) || num.getLowestSetBit() >= i-1))
	                        a.add(num.setBit(i));
	                }
	            }
	        }
	        result = a;
	    }

	    String zeros = new String(new char[n]).replace("\0", "0");
	    for (BigInteger i : result) {
	        String binary = i.toString(2);
	        //System.out.println(zeros.substring(0, n - binary.length()) + binary);
	        possiblePaths.add(zeros.substring(0, n - binary.length()) + binary);
	    }
	    
	    return possiblePaths;

	}}

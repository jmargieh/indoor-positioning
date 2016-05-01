package main.java.com.indoor.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.com.indoor.helpers.AStar;
import main.java.com.indoor.helpers.CustomComparator;
import main.java.com.indoor.helpers.CustomLineString;
import main.java.com.indoor.helpers.GridSquare;
import main.java.com.indoor.helpers.SuperSimpleFeaturePoint;
import main.java.com.indoor.helpers.CustomLineString.Direction;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class IndoorNavigationProcessing {

		private String result = "success"; // a status indicating the processing result
	
		//space coords
		final double gridMinX1 = -417.0;
		final double gridMaxX2 = 305.0;
		final double gridMinY1 = -72.0;
		final double gridMaxY2 = 82.0;
		// space coords
		
		final double distanationLineRateWeight = 0.4;
		final double sourceLineRateWeight = 0.6; 
		// thate minimum gridSize we could reach on our laptops is 0.625
		// otherwise OutOfMemoryError Exception will be thrown - GC overhead limit exceeded
		final double gridSize = 0.625; //0.625; //1.25; //2.5; //5.0; // 0.75; //10.0 // 2.5 --> the smaller gridSize the better 
		
		final int minimumNumberOfSamples = 3;
		final double pointDistanceThreshold = 10; // should be 10 -- >
		//points json file
		private InputStream pointsInputStream;// = IndoorNavigationProcessing.class.getResourceAsStream("/json/points.json");
		//obstacles json file - generally polygons
		private InputStream obstaclesInputStream;// = IndoorNavigationProcessing.class.getResourceAsStream("/json/obstacles.json");
		// Device n+1 (the device we get after we did the pre-processing)
		private InputStream deviceInputStream;// = IndoorNavigationProcessing.class.getResourceAsStream("/json/device.json");
		// new device id
		private String newDeviceId;
		// new device points array
		private List<SimpleFeature> newDevicePointsArray = new ArrayList<>();
		// HashMap with key:deviceid & SuperSimpleFeature points List sorted by timestamp.
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
		
		private AStar aStar;
		
		public GridSquare[][] getGridMapMatrix() {
			return this.gridMapMatrix;
		}
		
		public String getResult() {
			return this.result;
		}
		
		// close Input Streams
		private void closeInputStreams() throws IOException {
			pointsInputStream.close();
			obstaclesInputStream.close();
			deviceInputStream.close();
		}

		// initializating pointsArray and obstaclesArray from inputStreams
		private void initArrayLists() {

			SimpleFeature feature;
			FeatureJSON io = new FeatureJSON();
			//Geometry g;
			// get points.json as InputStream
			try {
				Reader rd = GeoJSONUtil.toReader(this.pointsInputStream);
				FeatureIterator<SimpleFeature> features = io.streamFeatureCollection(rd);
				while (features.hasNext()) {
					feature = features.next();
					this.pointsArray.add(feature);
				}

				rd = GeoJSONUtil.toReader(this.obstaclesInputStream);
				features = io.streamFeatureCollection(rd);
				while (features.hasNext()) {
					feature = features.next();
					this.obstaclesArray.add(feature);
				}
				
				rd = GeoJSONUtil.toReader(this.deviceInputStream);
				features = io.streamFeatureCollection(rd);
				while (features.hasNext()) {
					feature = features.next();
					this.newDeviceId = feature.getID();
					this.newDevicePointsArray.add(feature);
				}
				closeInputStreams();

			} catch (IOException e) {
				this.result = e.getMessage();
				System.err.print(e.getMessage());
			}

		}
		
		public IndoorNavigationProcessing(String space, String obstacles, String points) {
			
			try {
				this.pointsInputStream = new URL("http://localhost:8080/indoor-positioning/upload/" + points + ".json").openStream();
				//obstacles json file - generally polygons
				this.obstaclesInputStream = new URL ("http://localhost:8080/indoor-positioning/upload/" + obstacles + ".json").openStream();
				// Device n+1 (the device we get after we did the pre-processing)
				this.deviceInputStream = IndoorNavigationProcessing.class.getResourceAsStream("/json/device.json");
			} catch (FileNotFoundException e) {
				this.result = e.getMessage();
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				this.result = e.getMessage();
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				this.result = e.getMessage();
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			initArrayLists();

			/*
			 * for each point checks if it's located on obstacle and add it to points in obstacles array.
			 */
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
			
			//Processing..
			
			putPointsIntoDeviceHashMap();
			createGridMap();
			deleteCustomLineStringCrossingObstacle();
			updateRateAndPointsGridSquares();
			updateRateCustomLineInGrid();
			updateGridSquaresWithinObstacles();
			initDeviceSuperSimpleFeaturePointMap(); // a duplicate
			this.aStar = new AStar(this.gridMapMatrix);
			reLocatePointsInObstacle();
			constructDevicesPathCoordinates(); // creates path coordinates of devices
			createDevicesPath(); // constructs a LineString from coordinates
			
			try {
				closeInputStreams();
			} catch (IOException e) {
				this.result = e.getMessage();
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		/*
		 * need to loop over deviceMap and covert it into DeviceSuperSimpleFeaturePointMap
		 * (The same map with different class) -> this is done in order to use same heuristic function
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

		private void constructDevicesPathCoordinates() {
			Iterator<Map.Entry<String, List<SuperSimpleFeaturePoint>>> iterator = this.deviceSuperSimpleFeaturePointMap.entrySet().iterator();
			Map.Entry<String, List<SuperSimpleFeaturePoint>> entry = null; // <deviceId,
			SuperSimpleFeaturePoint superSimpleFeaturePoint1, superSimpleFeaturePoint2;
			List<int[]> pathCoordinates = new ArrayList<int[]>();
			while(iterator.hasNext()){
				try {
					entry = iterator.next();
					for (int i=0; i<entry.getValue().size()-1; i++ ) {
						superSimpleFeaturePoint1 = entry.getValue().get(i);
						superSimpleFeaturePoint2 = entry.getValue().get(i+1);
						this.aStar.setGridMapMatrix(this.gridMapMatrix); // need update gridMapMatrix of aStar since we're removing devices in case no possible path exist
						pathCoordinates.addAll(aStar.findShortestPath(superSimpleFeaturePoint1.getRowIndex(), superSimpleFeaturePoint1.getColumnIndex(), superSimpleFeaturePoint2.getRowIndex(), superSimpleFeaturePoint2.getColumnIndex()));
						if (i+1 == entry.getValue().size()-1) {
							int [] lastPointIndexes =  {superSimpleFeaturePoint2.getRowIndex(),superSimpleFeaturePoint2.getColumnIndex()};
							pathCoordinates.add(lastPointIndexes);
						}
					}
					
					updateGridMapMatrixCustomLineStringAndGridSquareRates(pathCoordinates);
					
					// here contruct a new super simple feature arraylist and add it to the device id
					
					// creating a copy of an existing superSimpleFeature (same attributes)
					SuperSimpleFeaturePoint superSimpleFeature = entry.getValue().get(0); // get one of the superSimpleFeatures
					SimpleFeatureType type = superSimpleFeature.getSimpleFeaturePoint().getType(); // get type
					SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type); // initiate builder of the type
					builder.init(superSimpleFeature.getSimpleFeaturePoint()); // init
					
					List<SuperSimpleFeaturePoint> superSimpleFeaturePointsList = new ArrayList<SuperSimpleFeaturePoint>();
					for (int i = 0; i < pathCoordinates.size(); i++) {
						SimpleFeature simpleFeaturePoint = builder.buildFeature( superSimpleFeature.getSimpleFeaturePoint().getID()); // make copy with same device id
						Geometry gridSquare = (Geometry)gridMapMatrix[pathCoordinates.get(i)[0]][pathCoordinates.get(i)[1]].getSquare().getAttribute("element");
	    				Geometry point = gridSquare.getCentroid();
	    				simpleFeaturePoint.setAttribute("geometry", point.toText());
	    				simpleFeaturePoint.setAttribute("timestamp", i+1);
	    				simpleFeaturePoint.setAttribute("title", "point_".concat(String.valueOf(i+1)));
						SuperSimpleFeaturePoint superSimpleFeaturePoint = new SuperSimpleFeaturePoint(simpleFeaturePoint, pathCoordinates.get(i)[0], pathCoordinates.get(i)[1]);
						superSimpleFeaturePointsList.add(superSimpleFeaturePoint);
					}					
					entry.getValue().clear(); // clear all existing points
					entry.getValue().addAll(superSimpleFeaturePointsList); // add the new points
					
				}catch (NullPointerException e) {
					if (e.getMessage() != null && (e.getMessage().compareTo("No Possible Path") == 0) && entry != null) {
						//update GridSquare Rate since this device will not be considered in calculations.
						updateGridSquareRateBeforeDeviceRemove(entry.getKey());
						iterator.remove(); // remove device that has no possible path between two points
					}else {
						this.result = e.getMessage();
						e.printStackTrace();
					}
				}
			}
		}
		private CustomLineString.Direction getDirectionFromTwoPoint(int[] point1 , int[] point2){
			int si = point1[0] , sj = point2[0], ei = point1[1] , ej = point2[1];
			CustomLineString.Direction direction = null;
			if(si == sj){ // point on the same row
				if(ei > ej){
					direction = Direction.LEFT;
				}
				else if(ei < ej){
					direction = Direction.RIGHT;
				}
			}
			if(ei == ej){// point on same column
				if(si < sj){ 
					direction = Direction.DOWN;
				}
				else{
					direction = Direction.UP;
				}
			}
			if (ei > ej){
				if(si > sj){
					direction = Direction.DIAGONALUPLEFT;
				}
				else if(si < sj){
					direction = Direction.DIAGONALDOWNLEFT;		
				}
			}
			else if(ej > ei){
				if( si > sj){
					direction = Direction.DIAGONALUPRIGHT;
				}
				else if( si < sj){
					direction = Direction.DIAGONALDOWNRIGHT;
				}
			}
				
			return direction;
		}
		
		/**
		 * 
		 * @param pathCoordinates coordinates of certain device
		 */
		private void updateGridMapMatrixCustomLineStringAndGridSquareRates(List<int[]> pathCoordinates) {
			// will need to loop over pathCoordinates and update GridMapMatrix Line String according
			// to the Custom Line String Direction.
			CustomLineString.Direction direction = null;
			for(int i=0; i<pathCoordinates.size()-1; i++){
				this.gridMapMatrix[pathCoordinates.get(i)[0]][pathCoordinates.get(i)[1]].increaseGridSquareRateBy(1.0);
				direction = getDirectionFromTwoPoint(pathCoordinates.get(i) , pathCoordinates.get(i+1) );
				this.gridMapMatrix[pathCoordinates.get(i)[0]][pathCoordinates.get(i)[1]].getCustomLineStringByDirection(direction).increaseRateBy(1.0);
			}
		}
		
		private void reLocatePointsInObstacle() {
			Iterator<Map.Entry<String, List<SuperSimpleFeaturePoint>>> iterator = this.deviceSuperSimpleFeaturePointMap.entrySet().iterator();
			Map.Entry<String, List<SuperSimpleFeaturePoint>> entry = null; // <deviceId,
			SuperSimpleFeaturePoint superSimpleFeature;
			int[] newIndexes;
	        while(iterator.hasNext()){
	        	try {
	        		entry = iterator.next();
		            for (int i=0; i<entry.getValue().size(); i++ ) {
		            	superSimpleFeature = entry.getValue().get(i);
		            	Geometry obstacle = isPointInObstacle(superSimpleFeature.getSimpleFeaturePoint());           	
		            	if(obstacle != null) {
								//newIndexes = heuristicCalculation(superSimpleFeature,i, entry.getKey(), obstacle);
		    					newIndexes = heuristicCalculation(superSimpleFeature,i, entry.getKey());
								superSimpleFeature.setRowIndex(newIndexes[0]);
			    				superSimpleFeature.setColumnIndex(newIndexes[1]);
			    				Geometry gridSquare = (Geometry)gridMapMatrix[newIndexes[0]][newIndexes[1]].getSquare().getAttribute("element");
			    				Geometry point = gridSquare.getCentroid();
			    				// point.toText() will return POINT (x,y) - change point in obstacle coordinates to be the centroid of the
			    				// gridSquare coordinates.
			    				superSimpleFeature.getSimpleFeaturePoint().setAttribute("geometry", point.toText());
		    			}
		            }
	        	}catch (IllegalAccessException e) {
	        		this.result = e.getMessage();
					e.printStackTrace();
				} catch (NullPointerException e) {
					if (e.getMessage() != null && (e.getMessage().compareTo("No Possible Path") == 0) && entry != null) {
						//update GridSquare Rate since this device will not be considered in calculations.
						updateGridSquareRateBeforeDeviceRemove(entry.getKey());
						iterator.remove(); // remove device that has no possible path between two points
					}else {
						this.result = e.getMessage();
						e.printStackTrace();
					}
				}
	        }
		}
	
		private void updateGridSquareRateBeforeDeviceRemove(String deviceId) {
			List<SuperSimpleFeaturePoint> ssfpArrayList = this.deviceSuperSimpleFeaturePointMap.get(deviceId);
			SuperSimpleFeaturePoint superSimpleFeaturePoint = null;
			int rowIndex, columnIndex;
			for (int i=0 ; i< ssfpArrayList.size() ; i++){
				superSimpleFeaturePoint= ssfpArrayList.get(i);
				rowIndex = superSimpleFeaturePoint.getRowIndex();
				columnIndex = superSimpleFeaturePoint.getColumnIndex();
				this.gridMapMatrix[rowIndex][columnIndex].setRate(this.gridMapMatrix[rowIndex][columnIndex].getRate() - 1);
			}
			
		}
		
		private int[] heuristicCalculation(SuperSimpleFeaturePoint ndsfp, int index, String deviceId) throws IllegalAccessException,NullPointerException {
			int [] newIndexes = new int[2];
			int [] originalPointIndexes =  {ndsfp.getRowIndex(),ndsfp.getColumnIndex()};
			int [] previousPointIndexes = new int[2], nextPointIndexes = new int[2];
			// checks if has a next point (point are sorted by timestamp)
			if(index+1 < this.deviceSuperSimpleFeaturePointMap.get(deviceId).size()) {
				nextPointIndexes[0] = this.deviceSuperSimpleFeaturePointMap.get(deviceId).get(index+1).getRowIndex(); //i
				nextPointIndexes[1] = this.deviceSuperSimpleFeaturePointMap.get(deviceId).get(index+1).getColumnIndex(); //j
			}
			// checks if has a previous point (point are sorted by timestamp)
			if(index-1 >= 0) {
				previousPointIndexes[0] = this.deviceSuperSimpleFeaturePointMap.get(deviceId).get(index-1).getRowIndex(); //i
				previousPointIndexes[1] = this.deviceSuperSimpleFeaturePointMap.get(deviceId).get(index-1).getColumnIndex(); //j
			}
			try {
				List<int[]> pathCoordinates = new ArrayList<int[]>();
				this.aStar.setGridMapMatrix(this.gridMapMatrix);
				pathCoordinates = aStar.findShortestPath(previousPointIndexes[0], previousPointIndexes[1], nextPointIndexes[0], nextPointIndexes[1]);
				// add destination point to pathCoordinates arraylist since it's not been added by AStar
				int[] indexes = { nextPointIndexes[0], nextPointIndexes[1]};
				pathCoordinates.add(indexes);
				Geometry obstaclePoint = (Geometry)ndsfp.getSimpleFeaturePoint().getAttribute("geometry");
				Geometry gridSquare = (Geometry)gridMapMatrix[pathCoordinates.get(1)[0]][pathCoordinates.get(1)[1]].getSquare().getAttribute("element");
				Geometry point = gridSquare.getCentroid();
				double minimumDistance = calculateDistance(obstaclePoint,point);
				newIndexes[0] = pathCoordinates.get(1)[0];
				newIndexes[1] = pathCoordinates.get(1)[1];
				// start loop from 2 ignore first two elements (source/and selected minimum)
				for(int i = 2; i < pathCoordinates.size() - 1; i++) {
					gridSquare = (Geometry)gridMapMatrix[pathCoordinates.get(i)[0]][pathCoordinates.get(i)[1]].getSquare().getAttribute("element");
					point = gridSquare.getCentroid();
					if(calculateDistance(obstaclePoint,point) < minimumDistance) {
						minimumDistance = calculateDistance(obstaclePoint,point);
						newIndexes[0] = pathCoordinates.get(i)[0];
						newIndexes[1] = pathCoordinates.get(i)[1];
					}
				}
			}catch(NullPointerException e) {
				throw e;
			}
						
			return newIndexes;
			
		}
				

		private Geometry isPointInObstacle(SimpleFeature point) {
			for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();){
				Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
				if (obstacle.contains((Geometry)point.getAttribute("geometry"))) {
					return obstacle;
				}
			}
			return null; //otherwise
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
			//removeDevicesWithPointsDistanceAboveThreshold(); // need to be umcommented later
		}
		
		private void removeDevicesWithPointsDistanceAboveThreshold() {
			Iterator<Map.Entry<String, List<SimpleFeature>>> iterator = this.deviceMap.entrySet().iterator();
			List<SimpleFeature> devicePointsList;
			boolean removeDevice;
			while(iterator.hasNext()){
				removeDevice = false;
				Map.Entry<String, List<SimpleFeature>> entry = iterator.next();
				devicePointsList = entry.getValue();
				for(int i = 0; i < devicePointsList.size() - 1; i++) {
					if(calculateDistance((Geometry)devicePointsList.get(i).getAttribute("geometry"), (Geometry)devicePointsList.get(i+1).getAttribute("geometry")) > this.pointDistanceThreshold) {
						removeDevice = true;
						break;
					}
				}
				// remove device if distance between 2 consecutive points is bigger than defined threshold
				if(removeDevice) {
					iterator.remove();
				}
			}
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
		
		// gets something like (Geometry)pIterator.next().getAttribute("geometry"))
		private Geometry createRectangle(double x1, double y1, double x2, double y2) {

			GeometryFactory geometryFactory = new GeometryFactory();
			GeometryBuilder gb = new GeometryBuilder(geometryFactory);
			return gb.box(x1, y1, x2, y2);
		}
		
		// construct a lineString from given set of points and save it into a devicePathMap
		// key : deviceID    value: LineString (costructed from the device's points)
		// TODO : will need to modify this function to give path from  the start and end of each lineString. 
		private void createDevicesPath() {
			
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
		private void updateRateAndPointsGridSquares() {
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
						if(cls.getDirection() == Direction.DIAGONALDOWNLEFT){
							cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i+1][j-1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
						}
						if(cls.getDirection() == Direction.DIAGONALDOWNRIGHT){
							cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i+1][j+1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
						}
						if(cls.getDirection() == Direction.DIAGONALUPLEFT){
							cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i-1][j-1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
						}
						if(cls.getDirection() == Direction.DIAGONALUPRIGHT){
							cls.setRate((this.distanationLineRateWeight * this.gridMapMatrix[i-1][j+1].getRate()) + (this.sourceLineRateWeight * sourceLineRate));
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
						if( ((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).intersects((Geometry)simplefeature.getAttribute("geometry")) ||
								((Geometry)(gridMapMatrix[i][j].getSquare().getAttribute("element"))).contains((Geometry)simplefeature.getAttribute("geometry")) ){
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
							// add a Diagonal LineStrings.
							if(i > 0 && j < (int) (matrixCols - 1)) // if not most upper row and not most right
							{
								Geometry gDiagUpRight = (Geometry)this.gridMapMatrix[i-1][j+1].getSquare().getAttribute("element");
								Point centroidPointDiagUpRight = gDiagUpRight.getCentroid();
								line = createLineString(centroidPoint, centroidPointDiagUpRight);
								this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"DIAGONALUPRIGHT"));
							}
							if(i > 0 && j > 0) // if not most upper row and not most left
							{
								Geometry gDiagUpLeft = (Geometry)this.gridMapMatrix[i-1][j-1].getSquare().getAttribute("element");
								Point centroidPointDiagUpLeft = gDiagUpLeft.getCentroid();
								line = createLineString(centroidPoint, centroidPointDiagUpLeft);
								this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"DIAGONALUPLEFT"));
							}
							if(i < (int)(matrixRows - 1) && j < (int) (matrixCols - 1)) // if not most down row and not most right
							{
								Geometry gDiagDownRight = (Geometry)this.gridMapMatrix[i+1][j+1].getSquare().getAttribute("element");
								Point centroidPointDiagDownRight = gDiagDownRight.getCentroid();
								line = createLineString(centroidPoint, centroidPointDiagDownRight);
								this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"DIAGONALDOWNRIGHT"));
							}
							if(i < (int)(matrixRows - 1) && j > 0) // if not most down row and not most left
							{
								Geometry gDiagDownLeft = (Geometry)this.gridMapMatrix[i+1][j-1].getSquare().getAttribute("element");
								Point centroidPointDiagDownLeft = gDiagDownLeft.getCentroid();
								line = createLineString(centroidPoint, centroidPointDiagDownLeft);
								this.gridMapMatrix[i][j].addLineString(new CustomLineString(line, 0,"DIAGONALDOWNLEFT"));
							}
							
							
							//GridSquare[i][j] = feature; // to be modified add new(..,..,..,..);
						}
					}
				
				}catch(IOException e){
					this.result = e.getMessage();
					System.err.println("error IO:" + e.getMessage() );
				} catch (Exception e) {
					this.result = e.getMessage();
					System.err.println("error Exception:" + e.getMessage() );
				}
			
		}

		@SuppressWarnings("unchecked")
		public JSONObject GenerateHeatMap() {
			
			JSONObject heatMapObject = new JSONObject();
			JSONArray heatmapDataArray = new JSONArray();
			Geometry gridSquare, point;
			double max = 0;
			for (int i = 0 ; i < this.gridMapMatrix.length ; i++) {
				for(int j=0 ; j<this.gridMapMatrix[i].length; j++ ){
					//JSONArray heatmapEntry = new JSONArray();
					JSONObject heatMapEntryObject = new JSONObject();
					gridSquare = (Geometry)gridMapMatrix[i][j].getSquare().getAttribute("element");
					if (gridMapMatrix[i][j].getRate() != 0) {
						
	    				point = gridSquare.getCentroid();
	    				
	    				heatMapEntryObject.put("lng", point.getCoordinate().x);
	    				heatMapEntryObject.put("lat", point.getCoordinate().y);
	    				heatMapEntryObject.put("count", gridMapMatrix[i][j].getRate());
	    				if (gridMapMatrix[i][j].getRate() > max) {
	    					max = gridMapMatrix[i][j].getRate();
	    				}
	    				//heatmapEntry.add(point.getCoordinate().x);
	    				//heatmapEntry.add(point.getCoordinate().y);
	    				//heatmapEntry.add(gridMapMatrix[i][j].getRate() * 100);
	    				heatmapDataArray.add(heatMapEntryObject);	
					}
					
				}
			}
			
			
			heatMapObject.put("heatmapDataArray", heatmapDataArray);
			heatMapObject.put("maximumHeat", max);
			
			return heatMapObject;
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

		}
}

package main.java.com.indoor.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RandomGeoJsonPointGenerator {

	final static double minimumLongitude = -417.0;
	final static double maximumLongitude = 305.0;
	final static double minimumLatitude = -72.0;
	final static double maximumLatitude = 82.0;
	final static double VARIANCE = 4.0f;
	
	private static Random fRandom = new Random();
	
	public static String generatePathOfPoints(int pointsNumber, int devNumber) {
		List<double[]> coordinatesArrayList = generateCoordinates(pointsNumber);
		UUID devId = UUID.randomUUID();
		String res = "{\"type\": \"FeatureCollection\",\"features\": [";
		for(int i = 0; i < coordinatesArrayList.size(); i++) {
			res += "{\"type\":\"Feature\",\"properties\":{\"id\":\"marker-ih985is73\",\"title\":\"point_"+ (i+1) +"\",\"description\":\"dev_" + devNumber + "\",\"marker-size\":\"small\",\"marker-color\":\"#fc4353\",\"marker-symbol\":\"circle-stroked\",\"timestamp\":" + (i+1) + "},\"geometry\":{\"coordinates\":["+ coordinatesArrayList.get(i)[0] +","+ coordinatesArrayList.get(i)[1] +"],\"type\":\"Point\"},\"id\":\" " + devId + " \" },";
		}
		res = res.substring(0, res.length()-1);
		res+= "],\"id\": \"jmargieh.p603a4m0\"}";
		return res;
	}
	
	private static List<double[]> generateCoordinates(int pointsNumber) {
		List<double[]> coordinatesArrayList = new ArrayList<double[]>();
		double[] startingCoordinates =  generateRandomStartingCoordinate();
		coordinatesArrayList.add(startingCoordinates);
		for(int i = 1; i < pointsNumber; i++) {
			double [] randomCoordinate = new double[2];
			randomCoordinate[0] = getGaussian(coordinatesArrayList.get(i-1)[0], VARIANCE); // random latitude
			randomCoordinate[1] = getGaussian(coordinatesArrayList.get(i-1)[1], VARIANCE); // random latitude
			coordinatesArrayList.add(randomCoordinate);
		}
		return coordinatesArrayList;
	}
	
	
	private static double[] generateRandomStartingCoordinate() {
		double[] coordinate = new double[2];
		Random r = new Random();
		coordinate[0] = minimumLatitude + (maximumLatitude - minimumLatitude) * r.nextDouble(); // random latitude
		coordinate[1] = minimumLongitude + (maximumLongitude - minimumLongitude) * r.nextDouble(); // random longitude
		return coordinate;	
	}
	
	private static double getGaussian(double aMean, double aVariance){
	    return aMean + fRandom.nextGaussian() * aVariance;
	  }
	
}

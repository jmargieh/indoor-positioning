package main.java.com.indoor.helpers;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

public class GridSquare {
	
	private String id; // id = rowIndex-columnIndex
	private SimpleFeature square;
	private double rate;
	private List<CustomLineString> customLineStringArray;
	private List<SimpleFeature> pointsInSquare;
	private boolean isInObstacle;
	
	public GridSquare(String id, SimpleFeature square, double rate, List<CustomLineString> LineArray){
		this.id = id;
		this.square = square;
		this.rate = rate;
		this.pointsInSquare = new ArrayList<SimpleFeature>();
		this.customLineStringArray = new ArrayList<CustomLineString>();
		this.setIsInObstacle(false);
		for(CustomLineString line : LineArray)
		{
			this.customLineStringArray.add(line);
		}
	}// end c'tor
	
	public GridSquare(String id,SimpleFeature square,double rate) {
		this.id = id;
		this.square = square;
		this.pointsInSquare = new ArrayList<SimpleFeature>();
		this.customLineStringArray = new ArrayList<CustomLineString>();
		this.rate = rate;
		this.setIsInObstacle(false);
	}
	public String getId(){
		return this.id;
	}
	
	public SimpleFeature getSquare(){
		return this.square;
	}
	
	public double getRate(){
		return this.rate;
	}
	
	public List<CustomLineString> getLineStringArray(){
		return this.customLineStringArray;
	}
	
	public void setLineStringArray(List<CustomLineString> list){
		this.customLineStringArray = list;
	}
	
	public void addLineString(CustomLineString line){
		this.customLineStringArray.add(line);
	}
	
	public void setRate(double rate){
		this.rate = rate;
	}
	
	public void addPointToGridSquare(SimpleFeature simplefeaturePoint){
		this.pointsInSquare.add(simplefeaturePoint);
	}
	
	public List<SimpleFeature> getPointsInSquareArrayList(){
		return this.pointsInSquare;
	}

	public boolean getIsInObstacle() {
		return isInObstacle;
	}

	public void setIsInObstacle(boolean isInObstacle) {
		this.isInObstacle = isInObstacle;
	}
	
	public int getRowIndex() {
		return Integer.parseInt(this.id.split("-")[0]);
	}
	
	public int getColumnIndex() {
		return Integer.parseInt(this.id.split("-")[1]);
	}
	
	}


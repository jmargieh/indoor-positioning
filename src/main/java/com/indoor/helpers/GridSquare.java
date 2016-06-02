package main.java.com.indoor.helpers;

import java.util.ArrayList;
import java.util.List;

import main.java.com.indoor.helpers.CustomLineString.Direction;

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
	
	public void increaseGridSquareRateBy(double rate) {
		this.rate += rate;
	}
	
	public int getRowIndex() {
		return Integer.parseInt(this.id.split("-")[0]);
	}
	
	public int getColumnIndex() {
		return Integer.parseInt(this.id.split("-")[1]);
	}
	
	public CustomLineString getCustomLineStringByDirection(Direction direction) {
		CustomLineString cls = null;
		for (int i = 0; i < this.customLineStringArray.size(); i++) {
			if(this.customLineStringArray.get(i).getDirection().compareTo(direction) == 0) {
				cls =  this.customLineStringArray.get(i);
			}
		}
		return cls;
	}
	
	/**
	 * @param direction
	 * @param rate
	 * sets the rate of a lineString according to it's direction
	 */
	public void setCustomLineStringByDirection(Direction direction, double rate) {
		for (int i = 0; i < this.customLineStringArray.size(); i++) {
			if(this.customLineStringArray.get(i).getDirection().compareTo(direction) == 0) {
				this.customLineStringArray.get(i).setRate(rate);
			}
		}
	}
	
	}


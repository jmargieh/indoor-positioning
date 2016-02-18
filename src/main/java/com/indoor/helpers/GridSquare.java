package main.java.com.indoor.helpers;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

public class GridSquare {
	
	private String id;
	private SimpleFeature square;
	private double rate;
	private List<CustomLineString> customLineStringArray;
	
	public GridSquare(String id, SimpleFeature square, double rate, List<CustomLineString> LineArray){
		this.id = id;
		this.square = square;
		this.rate = rate;
		this.customLineStringArray = new ArrayList<CustomLineString>();
		for(CustomLineString line : LineArray)
		{
			this.customLineStringArray.add(line);
		}
	}// end c'tor
	
	public GridSquare(String id,SimpleFeature square,double rate) {
		this.id = id;
		this.square = square;
		this.customLineStringArray = new ArrayList<CustomLineString>();
		this.rate = rate;
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
	
	}


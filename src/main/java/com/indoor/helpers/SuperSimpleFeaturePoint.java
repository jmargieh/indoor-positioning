package main.java.com.indoor.helpers;

import org.opengis.feature.simple.SimpleFeature;

public class SuperSimpleFeaturePoint {

	private SimpleFeature feature;
	private int rowIndex; 
	private int columnIndex;
	
public SuperSimpleFeaturePoint(SimpleFeature feature,int rowIndex, int columnIndex){
	this.feature = feature;
	this.setRowIndex(rowIndex);
	this.setColumnIndex(columnIndex);
}

public SimpleFeature getSimpleFeaturePoint() {
	return this.feature;
}

public int getRowIndex() {
	return rowIndex;
}

public void setRowIndex(int rowIndex) {
	this.rowIndex = rowIndex;
}

public int getColumnIndex() {
	return columnIndex;
}

public void setColumnIndex(int columnIndex) {
	this.columnIndex = columnIndex;
}

	
}

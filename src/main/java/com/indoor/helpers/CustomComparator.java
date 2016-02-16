package main.java.com.indoor.helpers;

import java.util.Comparator;

import org.opengis.feature.simple.SimpleFeature;

public class CustomComparator implements Comparator<SimpleFeature> {
    @Override
    public int compare(SimpleFeature s1, SimpleFeature s2) {
    	int timeStamp1 = Integer.parseInt(s1.getAttribute("timestamp").toString());
    	int timeStamp2 = Integer.parseInt(s2.getAttribute("timestamp").toString());
        return timeStamp1 - timeStamp2;
    }
}

package main.java.com.indoor.helpers;
import com.vividsolutions.jts.geom.Geometry;


public class CustomLineString {
	
	private Geometry line;
	private double rate;
	public enum Direction {
	    UP,
	    RIGHT,
	    DOWN,
	    LEFT,
	    DIAGONALUPLEFT,
	    DIAGONALUPRIGHT,
	    DIAGONALDOWNLEFT,
	    DIAGONALDOWNRIGHT
	}
	private Direction direction;
	
	public CustomLineString(Geometry line, double rate, String direction) {
		this.setLine(line);
		this.setRate(rate);
		
		switch (direction) {
		case "UP":
			this.setDirection(Direction.UP);
			break;
 
		case "RIGHT":
			this.setDirection(Direction.RIGHT);
			break;
 
		case "DOWN":
			this.setDirection(Direction.DOWN);
			break;
		case "LEFT":
			this.setDirection(Direction.LEFT);
			break;
		case "DIAGONALUPLEFT":
			this.setDirection(Direction.DIAGONALUPLEFT);
			break;
		case "DIAGONALUPRIGHT":
			this.setDirection(Direction.DIAGONALUPRIGHT);
			break;
		case "DIAGONALDOWNLEFT":
			this.setDirection(Direction.DIAGONALDOWNLEFT);
			break;
		case "DIAGONALDOWNRIGHT":
			this.setDirection(Direction.DIAGONALDOWNRIGHT);
			break;
 
		default:
			System.err.println("Something's wrong with CustomLineString Direction");
			break;
		}
	}


	public Geometry getLine() {
		return this.line;
	}


	public void setLine(Geometry line) {
		this.line = line;
	}


	public double getRate() {
		return rate;
	}


	public void setRate(double rate) {
		this.rate = rate;
	}

	public void increaseRateBy(double rate) {
		this.rate += rate;
	}

	public Direction getDirection() {
		return this.direction;
	}


	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	
	

}

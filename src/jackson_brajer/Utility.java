package jackson_brajer;

import java.awt.geom.Point2D;

public class Utility {
	
	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public static double randomBetween(double min, double max) {
		return min + Math.random() * (max - min);
	}
	
	public static double calcAngle(Point2D.Double p1, Point2D.Double p2) {
		return Math.atan2(p2.x - p1.x, p2.y - p1.y);
	}


}

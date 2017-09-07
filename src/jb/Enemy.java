package jb;

import java.awt.geom.Point2D;

public class Enemy {
	public Point2D.Double coord = new Point2D.Double(0, 0);
	public Point2D.Double lastCoord;
	public boolean alive;
	public double energy;
	public double heading;
}

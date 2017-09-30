package jackson_brajer;

import java.awt.geom.Point2D;

public class Robot extends Point2D.Double {

	public long scanTime;
	public boolean alive = true;
	public double energy;
	public String name;
	public double gunHeadingRadians;
	public double bearingRadians;
	public double velocity;
	public double heading;
	public double lastHeading;
	public double shootableScore;	

}

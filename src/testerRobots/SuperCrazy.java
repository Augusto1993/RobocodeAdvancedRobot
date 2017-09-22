package testerRobots;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
 
/**
 * SuperCrazy - a sample robot by Chase
 * <p/>
 * This robot moves around in a crazy pattern
 * and it fires randomly within the escape angle of the enemy
 */
public class SuperCrazy extends AdvancedRobot {

		static Point2D.Double[] enemyPoints = new Point2D.Double[14];
	int count;
	double oldEnemyHeading = 0;

	static double turn = 2;
	int turnDir = 1;
	int moveDir = 1;

	double oldEnergy = 100;
	int wallcount = 0;

	/* How many times we have decided to not change direction. */
	public int sameDirectionCounter = 0;
 
	/* How long we should continue to move in the current direction */
	public long moveTime = 1;
 
	/* The direction we are moving in */
	public static int moveDirection = 1;
 
	/* The speed of the last bullet that hit us, used in determining how far to move before deciding to change direction again. */
	public static double lastBulletSpeed = 15.0;
 
	public double wallStick = 120;
 
	/**
	 * run: SuperCrazy's main run function
	 */
	public void run() {
 
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
 
		/* Loop forever */
 
		/* Simple Radar Code */
		while (true) {
			if (getRadarTurnRemaining() == 0.0)
		            	setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	        	execute();
		}
	}
 
	/**
	 * onScannedRobot:  Fire!
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
	
			/* Change the wall stick distance, to make us even more unpredictable */
			wallStick = 120 + Math.random()*40;
 
		double bulletPower = Math.min(3.0, getEnergy());
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		double enemyVelocity = e.getVelocity();
		oldEnemyHeading = enemyHeading;

		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while ((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			enemyHeading += enemyHeadingChange;
			if (predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0) {

				predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		fire(3);
 
		double absBearing = e.getBearingRadians() + getHeadingRadians();
		double distance = e.getDistance() + (Math.random()-0.5)*5.0;
 

		/* Movement */
		if(--moveTime <= 0) {
			distance = Math.max(distance, 100 + Math.random()*50) * 1.25;
			moveTime = 50 + (long)(distance / lastBulletSpeed);
 
			++sameDirectionCounter;
 
			/* Determine if we should change direction */
			if(Math.random() < 0.5 || sameDirectionCounter > 16) {
				moveDirection = -moveDirection;
				sameDirectionCounter = 0;
			}
		}
 
 
		/* Move perpendicular to our enemy, based on our movement direction */
		double goalDirection = absBearing-Math.PI/2.0*moveDirection;
 
		/* This is too clean for crazy! Add some randomness. */
		goalDirection += (Math.random()-0.5) * (Math.random()*2.0 + 1.0);
 
		/* Smooth around the walls, if we smooth too much, reverse direction! */
		double x = getX();
		double y = getY();
		double smooth = 0;
 
		/* Calculate the smoothing we would end up doing if we actually smoothed walls. */
		Rectangle2D fieldRect = new Rectangle2D.Double(18, 18, getBattleFieldWidth()-36, getBattleFieldHeight()-36);
 
		while (!fieldRect.contains(x+Math.sin(goalDirection)*wallStick, y+ Math.cos(goalDirection)*wallStick)) {
			/* turn a little toward enemy and try again */
			goalDirection += moveDirection*0.1;
			smooth += 0.1;
		}
 
		/* If we would have smoothed to much, then reverse direction. */
		/* Add && sameDirectionCounter != 0 check to make this smarter */
		if(smooth > 0.5 + Math.random()*0.125) {
			moveDirection = -moveDirection;
			sameDirectionCounter = 0;
		}
 
		double turn = Utils.normalRelativeAngle(goalDirection - getHeadingRadians());
 
		/* Adjust so we drive backwards if the turn is less to go backwards */
		if (Math.abs(turn) > Math.PI/2) {
			turn = Utils.normalRelativeAngle(turn + Math.PI);
			setBack(100);
		} else {
			setAhead(100);
		}
 
		setTurnRightRadians(turn);
		}
			public void onHitWall(HitWallEvent e) {
				System.out.println("wall");
				wallcount++;
				System.out.println(wallcount);
			}
}
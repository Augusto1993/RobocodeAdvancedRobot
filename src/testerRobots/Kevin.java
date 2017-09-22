package testerRobots;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;

import robocode.ScannedRobotEvent;
import robocode.TurnCompleteCondition;
import robocode.util.Utils;
//import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * HeadSmasher - a robot by (your name here)
 */
public class Kevin extends AdvancedRobot {
	private byte moveDirection = 1;
	boolean movingForward;
	double previousEnergy = 100;
	int movementDirection = 1;
	int gunDirection = 1;

	public void run() {
		// robot is repeatedly scanning
		while (true) {
			setTurnGunRight(99999);
			// crazy movement code --> https://github.com/robo-code/robocode/blob/master/robocode.samples/src/main/java/sample/Crazy.java
			// set to move ahead 40000
			setAhead(40000*moveDirection);
			movingForward = true;
			setTurnRight(90);
			// At this point, we have indicated to the game that *when we do something*,
			// we will want to move ahead and turn right.  That's what "set" means.
			// It is important to realize we have not done anything yet!
			// In order to actually move, we'll want to call a method that
			// takes real time, such as waitFor.
			// waitFor actually starts the action -- we start moving and turning.
			// It will not return until we have finished turning.
			waitFor(new TurnCompleteCondition(this));
			// Note:  We are still moving ahead now, but the turn is complete.
			// Now we'll turn the other way...
			setTurnLeft(180);
			// ... and wait for the turn to finish ...
			waitFor(new TurnCompleteCondition(this));
			// ... then the other way ...
			setTurnRight(180);
			// .. and wait for that turn to finish.
			waitFor(new TurnCompleteCondition(this));
			// then back to the top to do it all again
		}
	}

	
	public void onHitWall(HitWallEvent e) {
		// bounce off
		// reverseDirection();
		moveDirection *= -1;
	}

	/**
	 * run: HeadSmasher's default behavior
	 */

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// strafe code --> http://mark.random-article.com/weber/java/robocode/lesson5.html
			// always square off against our enemy
	setTurnRight(e.getBearing() + 90);
	// strafe by changing direction every 20 ticks
	if (getTime() % 20 == 0) {
		moveDirection *= -1;
		setAhead(150 * moveDirection);
	}
		// linear targeting code --> http://robowiki.net/wiki/Linear_Targeting
		double bulletPower = Math.min(3.0, getEnergy());
		double myX = getX();
		double myY = getY();
		// gets the enemy's absolute bearing by adding your heading by the enemy's bearing
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		// gets the enemy's x coordinate by adding your x value to the distance between you and the enemy and multiplying by the sin of the enemy's absolute bearing
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		// gets the enemy's y coordinate through the same means as x except with cos instead of sin
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		// gets enemy heading and speed
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();

		// variable to record the change in time
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		// sets the initial values of the predicted location to the current location
		double predictedX = enemyX, predictedY = enemyY;
		// loop while the time * the bullet speed is less than the distance between me and the enemy
		while ((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)) {
			// predicted location = the position of the enemy * the speed
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			// checks to make sure the enemy robot scanned is still in the battlefield
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
		fire(bulletPower);
		// when a robot is scanned move at 45 degree angles towards the opponent
		setTurnRight(e.getBearing() + 90 - 30 * movementDirection);
		// if calculates the change in energy of the scanned robot
		double changeInEnergy = previousEnergy - e.getEnergy();
		// if the change in energy is in parameters of a bullet shot then dodge
		if (changeInEnergy > 0 && changeInEnergy <= 3) {
			movementDirection = -movementDirection;
			setAhead((e.getDistance() / 4 + 25) * movementDirection);
		}
		// turn the gun and sweep the opposite way to scan the enemy robot
		gunDirection = -gunDirection;
		setTurnGunRight(99999 * gunDirection);

		previousEnergy = e.getEnergy();

	}

	public void onHitByBullet(HitByBulletEvent e) {

	}
}
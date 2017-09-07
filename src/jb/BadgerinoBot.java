package jb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class BadgerinoBot extends AdvancedRobot {

	static final int PREDICTION_POINTS = 100;
	
	HashMap<String, Enemy> enemies = new HashMap<>();
	Point2D.Double myPos = new Point2D.Double(0, 0);
	Point2D.Double targetPoint = new Point2D.Double(0, 0);
	Point2D.Double lastPos = new Point2D.Double(0, 0);
	Double energy;
	Enemy targetBot;

	int idleTime = 0;

	List<Point2D.Double> possibleLocations = new ArrayList<>();

	public void run() {
		setColors(Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		
		myPos.x = getX();
		myPos.y = getY();
		
		targetPoint.x = myPos.x;
		targetPoint.y = myPos.y;
		
		lastPos.x = myPos.x;
		lastPos.y = myPos.y;
		
		energy = getEnergy();
		lastPos = new Point2D.Double(getX(), getY());
		
		updateListLocations(PREDICTION_POINTS);
		
		targetBot = new Enemy();
		while (true) {
			myPos.x = getX();
			myPos.y = getY();
			energy = getEnergy();
			if (getTime() > 9)
				if (targetBot.alive) {
					movement();
					shooting();
				}
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// Add enemy to the map of other enemies
		Enemy en = enemies.get(e.getName());

		// Not an enemy with that name in the hashmap
		if (en == null) {
			en = new Enemy();
			enemies.put(e.getName(), en);
		}
		en.lastCoord = en.coord;
		en.energy = e.getEnergy();
		en.alive = true;
		en.coord = new Point2D.Double(myPos.x + e.getDistance() * Math.sin(getHeadingRadians() + e.getBearingRadians()),
				myPos.y + e.getDistance() * Math.cos(getHeadingRadians() + e.getBearingRadians()));

		// Gotta kill those ram fires
		// If the target I was shooting at died switch to a new one or if a new
		// challenger has appeared
		if (!targetBot.alive || e.getDistance() < myPos.distance(targetBot.coord))
			targetBot = en;

		// LOGIC NEEDED FOR 1v1 SUPER SAYAN MODE ACTIVATE
		if (getOthers() == 1) {
			// Nano Bot Lock - Very Simple
			setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		}
	}

	public void onRobotDeath(RobotDeathEvent event) {
		enemies.get(event.getName()).alive = false;
	}

	public double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public double randomBetween(double min, double max) {
		double randomNum = min + (Math.random() * ((max - min) + 1));
		return randomNum;
	}

	public void shooting() {
		//TODO Guess Factor Targeting
		//Calculate point A = Full Speed Forward for the time it would take my bullet to hit it
		//Calculate point B = Full Speed Backwards for the time it would take my bullet to hit it
		//Calculate point C = Standing Still
		
		Point2D.Double pointA = targetBot.coord;
		Point2D.Double pointB = targetBot.coord;
		
		double distanceToTarget = myPos.distance(targetBot.coord);
		double angle = Utils.normalRelativeAngle(calcAngle(targetBot.coord, myPos) - getGunHeadingRadians());
		if (getGunTurnRemaining() == 0 && getEnergy() > 1 && getGunHeat() == 0) {
			double power = Math.min(Math.min(getEnergy() / 6d, 1300d / distanceToTarget), targetBot.energy / 3d);
			setFire(power);
		}
		setTurnGunRightRadians(angle);
	}

	public void movement() {
		if (targetPoint.distance(myPos) < 15 || idleTime > 25) {
			idleTime = 0;
			lastPos = myPos;
			updateListLocations(PREDICTION_POINTS);
			// WE ARE HERE NEW POINT
			Point2D.Double lowRiskP = new Point2D.Double(0, 0);
			double risk = 1;
			for (Point2D.Double p : possibleLocations) {
				if (evaluatePoint(p) <= risk) {
					risk = evaluatePoint(p);
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
		} else {
			idleTime++;
			// GO TO POINT
			double angle = Math.atan2(targetPoint.x - myPos.x, targetPoint.y - myPos.y) - getHeadingRadians();
			double direction = 1;

			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}

			setAhead(targetPoint.distance(myPos) * direction);
			angle = Utils.normalRelativeAngle(angle);
			setTurnRightRadians(angle);
			setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);
		}
	}

	public void updateListLocations(int n) {
		possibleLocations.clear();
		final int radius = 175;
		// Create x points in a radius pixel radius around the bot
		for (int i = 0; i < n; i++) {
			double randXMod = randomBetween(-radius, radius);
			double yRange = Math.sqrt(radius * radius - randXMod * randXMod);
			double randYMod = randomBetween(-yRange, yRange);
			double y = clamp(myPos.y + randYMod, 40, getBattleFieldHeight() - 40);
			double x = clamp(myPos.x + randXMod, 40, getBattleFieldWidth() - 40);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}

	public double evaluatePoint(Point2D.Double p) {
		// Retuns 0 - 1
		// 0 Being fully safe and 1 being suicide

		// FACTORS:
		// Number of robots in radius
		// Total Energy of robots in radius
		// Closeness to a wall
		// Distance from last position
		// Distance to closest enemy

		final int radiusForBots = 250;

		int botsInR = 0;
		double energyOfRobotsInR = 0;
		double distToSide, distToCeiling;
		double distanceFromLastPos = p.distance(lastPos);
		double distanceFromMid = p.distance(new Point2D.Double(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2));
		double distanceToEnemy = 0;

		for (Enemy e : enemies.values()) {
			distanceToEnemy += p.distance(e.coord);
		}
		if (distanceToEnemy == 0) {
			distanceToEnemy = Double.MAX_VALUE;
		}

		distToSide = Math.min(p.x, Math.abs(p.x - getBattleFieldWidth()));
		distToCeiling = Math.min(p.y, Math.abs(p.y - getBattleFieldHeight()));

		for (Enemy e : enemies.values()) {
			if (e.alive && p.distance(e.coord) <= radiusForBots) {
				botsInR++;
				energyOfRobotsInR += e.energy;
			}
		}

		double distFromLastScare = (80 / distanceFromLastPos) / 2;
		double botsScare = 0;
		if (botsInR != 0)
			botsScare = (energyOfRobotsInR / botsInR) / botsInR;
		double distFromWallScare = (3750 / (distToSide + distToCeiling)) / 5;
		distanceFromMid = (3750 / distanceFromMid) / 10;
		botsScare /= 75;

		distanceToEnemy = getBattleFieldWidth() / distanceToEnemy;

		botsScare *= .3;
		distFromLastScare *= 0.3;
		distanceToEnemy *= .10;
		distFromWallScare *= .15;
		distanceFromMid *= .15;
		double finalEval = botsScare + distFromLastScare + distanceToEnemy + distFromWallScare + distanceFromMid;

		System.out.println("-------------------");
		System.out.println("distFromLastScare: " + distFromLastScare);
		System.out.println("botsScare: " + botsScare);
		System.out.println("distanceToEnemy: " + distanceToEnemy);
		System.out.println("distFromWallScare: " + distFromWallScare);
		System.out.println("distanceFromMid: " + distanceFromMid);
		System.out.println("finalEval: " + finalEval);
		System.out.println("-------------------");

		return clamp(finalEval, 0, 1);
	}

	private static double calcAngle(Point2D.Double p2, Point2D.Double p1) {
		return Math.atan2(p2.x - p1.x, p2.y - p1.y);
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.BLUE);
		for (Point2D.Double p : possibleLocations) {
			double cChange = evaluatePoint(p);
			g.setColor(new Color((float) (1 * cChange), 0f, (float) (1 * (1 - cChange)), 1f));
			g.drawOval((int) p.x, (int) p.y, 5, 5);
		}
		for (Enemy e : enemies.values()) {
			if (!e.alive)
				g.setColor(new Color(0, 0, 0, 0));
			else
				g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
			g.drawOval((int) e.coord.x - 250, (int) e.coord.y - 250, 250 * 2, 250 * 2);

		}
		g.fillRect((int) targetPoint.x - 20, (int) targetPoint.y - 20, 40, 40);
	}
}
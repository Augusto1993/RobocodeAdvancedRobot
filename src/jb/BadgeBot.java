package jb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import robocode.AdvancedRobot;
import robocode.DeathEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

public class BadgeBot extends AdvancedRobot {

	static final int PREDICTION_POINTS = 150;
	
	Hashtable<String, Enemy> enemies = new Hashtable<>();
	Point2D.Double myPos = new Point2D.Double(0, 0);
	Point2D.Double targetPoint = new Point2D.Double(0, 0);
	Point2D.Double lastPos = new Point2D.Double(0, 0);
	Double energy;
	Enemy targetBot;

	int idleTime = 0;

	List<Point2D.Double> possibleLocations = new ArrayList<>();

	public void run() {
		if (finishes == null)
			finishes = new int[getOthers()+1];
		
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
		en.coord = new Point2D.Double(myPos.x + e.getDistance() * Math.sin(getHeadingRadians() + e.getBearingRadians()), myPos.y + e.getDistance() * Math.cos(getHeadingRadians() + e.getBearingRadians()));
		en.heading = e.getHeadingRadians();

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
			double risk = Double.MAX_VALUE;
			for (Point2D.Double p : possibleLocations) {
				if (evaluatePoint(p) <= risk) {
					risk = evaluatePoint(p);
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
			out.println(targetPoint);
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
		}
	}

	public void updateListLocations(int n) {
		possibleLocations.clear();
		final int radius = 125;
		// Create x points in a radius pixel radius around the bot
		for (int i = 0; i < n; i++) {
			double randXMod = randomBetween(-radius, radius);
			double yRange = Math.sqrt(radius * radius - randXMod * randXMod);
			double randYMod = randomBetween(-yRange, yRange);
			double y = clamp(myPos.y + randYMod, 75, getBattleFieldHeight() - 75);
			double x = clamp(myPos.x + randXMod, 75, getBattleFieldWidth() - 75);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}

	public double evaluatePoint(Point2D.Double p) {
		// FACTORS:
		// Number of robots in radius
		// Total Energy of robots in radius
		// Closeness to a wall
		// Distance from last position
		// Distance to closest enemy
//		final int radiusForBots = 250;
//
//		int botsInR = 0;
//		double energyOfRobotsInR = 0;
//		double distToSide, distToCeiling;
//		double distanceFromLastPos = p.distance(lastPos);
//		double distanceFromMid = p.distance(new Point2D.Double(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2));
//		double distanceToEnemy = 0;
//
//		for (Enemy e : enemies.values()) {
//			distanceToEnemy += p.distance(e.coord);
//		}
//		if (distanceToEnemy == 0) {
//			distanceToEnemy = Double.MAX_VALUE;
//		}
//
//		distToSide = Math.min(p.x, Math.abs(p.x - getBattleFieldWidth()));
//		distToCeiling = Math.min(p.y, Math.abs(p.y - getBattleFieldHeight()));
//
//		for (Enemy e : enemies.values()) {
//			if (e.alive && p.distance(e.coord) <= radiusForBots) {
//				botsInR++;
//				energyOfRobotsInR += e.energy;
//			}
//		}
//
//		double distFromLastScare = (80 / distanceFromLastPos) / 2;
//		double botsScare = 0;
//		if (botsInR != 0)
//			botsScare = (energyOfRobotsInR / botsInR) / botsInR;
//		double distFromWallScare = (3750 / (distToSide + distToCeiling)) / 5;
//		distanceFromMid = (3750 / distanceFromMid) / 10;
//		botsScare /= 75;
//
//		distanceToEnemy = getBattleFieldWidth() / distanceToEnemy;
//		
//		double distanceToCorner = Math.min(Math.min(myPos.distance(0,0),myPos.distance(getBattleFieldWidth(), getBattleFieldHeight())),Math.min(myPos.distance(0, getBattleFieldHeight()),myPos.distance(getBattleFieldWidth(), 0)));
//		
//		botsScare *= .30;
//		distFromLastScare *= 0.30;
//		distanceToEnemy *= .80;
//		distFromWallScare *= .1;
//		distanceFromMid *= 0.12;
//		distanceToCorner *= -0.001;
//		
//		double finalEval = botsScare + distFromLastScare + distanceToEnemy + distanceFromMid + distanceToCorner;
////		System.out.println("-------------------");
////		System.out.println("distFromLastScare: " + distFromLastScare);
////		System.out.println("botsScare: " + botsScare);
////		System.out.println("distanceToEnemy: " + distanceToEnemy);
////		System.out.println("distFromWallScare: " + distFromWallScare);
////		System.out.println("distanceFromMid: " + distanceFromMid);
////		System.out.println("distanceToCorner: " + distanceToCorner);
////		System.out.println("finalEval: " + finalEval);
////		System.out.println("-------------------");
//		
//		return finalEval;
		
		Enumeration _enum = enemies.elements();
		double eval = 0;
		while(_enum.hasMoreElements()){
			Enemy en = (Enemy)_enum.nextElement();
			double radiansToPointFromEnemy = calcAngle(p, en.coord);
			if(p.x > en.coord.x && p.y > en.coord.y){
				radiansToPointFromEnemy += 0;
			} else if(p.x > en.coord.x && p.y < en.coord.y){
				radiansToPointFromEnemy += 90;
			} else if(p.x < en.coord.x && p.y < en.coord.y){
				radiansToPointFromEnemy += 180;
			} else if(p.x < en.coord.x && p.y > en.coord.y){
				radiansToPointFromEnemy += 270;
			} else {
				radiansToPointFromEnemy += 0;
			}
			
			eval += (en.energy/getEnergy()) * (1/p.distanceSq(myPos)) * (180/Math.abs(radiansToPointFromEnemy - en.heading));
		}
		return eval;
	}

	private static double calcAngle(Point2D.Double p2, Point2D.Double p1) {
		return Math.atan2(p2.x - p1.x, p2.y - p1.y);
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.BLUE);
		for (Point2D.Double p : possibleLocations) {
			double cChange = clamp(evaluatePoint(p),0,1);
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
	
	//global declarations:
	static int[] finishes;

	//beginning of run():
	
	//somewhere:
	public void onWin(WinEvent e){onDeath(null);}
	public void onDeath(DeathEvent e)
	{
		finishes[getOthers()]++;
		for (int i=0; i<finishes.length; i++)
			out.print(finishes[i] + " ");
		out.println();
	}
}
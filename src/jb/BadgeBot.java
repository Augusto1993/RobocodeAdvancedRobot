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

	// Static variables or objects in robocode keep their data from round to round

	static final int PREDICTION_POINTS = 150;

	static int[] finishes;
	Hashtable<String, Robot> enemies = new Hashtable<>();
	
	Robot me = new Robot();
	Robot targetBot;
	
	List<Point2D.Double> possibleLocations = new ArrayList<>();
	Point2D.Double targetPoint = new Point2D.Double(0, 0);

	int idleTime = 0;	

	public void run() {
		if (finishes == null)
			finishes = new int[getOthers() + 1];

		setColors(Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		me.x = getX();
		me.y = getY();
		me.energy = getEnergy();

		targetPoint.x = me.x;
		targetPoint.y = me.y;

		updateListLocations(PREDICTION_POINTS);

		targetBot = new Robot();

		while (true) {	
			me.x = getX();
			me.y = getY();
			me.energy = getEnergy();
			me.gunHeadingRadians = getGunHeadingRadians();
			if (getTime() > 9) {
				if (targetBot.alive) {
					movement();
					shooting();
				}
			}
			Enumeration<Robot> enemiesEnum = enemies.elements();
			while (enemiesEnum.hasMoreElements()) {
				Robot r = enemiesEnum.nextElement();
				if (getTime() - r.scanTime > 20) {
					enemies.remove(r.name);
				}
			}
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// Add enemy to the map of other enemies
		Robot en = enemies.get(e.getName());

		// Not an enemy with that name in the hashmap
		if (en == null) {
			en = new Robot();
			enemies.put(e.getName(), en);
		}
		en.name = e.getName();
		en.energy = e.getEnergy();
		en.alive = true;
		en.scanTime = getTime();
		en.velocity = e.getVelocity();
		en.setLocation(
				new Point2D.Double(me.x + e.getDistance() * Math.sin(getHeadingRadians() + e.getBearingRadians()),
						me.y + e.getDistance() * Math.cos(getHeadingRadians() + e.getBearingRadians())));
		en.heading = e.getHeadingRadians();
		en.bearingRadians = e.getBearingRadians();

		// Gotta kill those ram fires
		// If the target I was shooting at died switch to a new one or if a new
		// challenger has appeared 10% closer
		if (!targetBot.alive || e.getDistance() * 0.9 < me.distance(targetBot))
			targetBot = en;

		// LOGIC NEEDED FOR 1v1 SUPER SAYAN MODE ACTIVATE
		if (getOthers() == 1) {
			// Nano Bot Lock - Very Simple
			setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		}
	}

	public void onRobotDeath(RobotDeathEvent event) {
		if (enemies.containsKey(event.getName())) {
			enemies.get(event.getName()).alive = false;
		}
	}

	public void shooting() {
		double POWER = Math.min(targetBot.energy/3d, 1000d/targetBot.distance(me));
		double angle = Utils.normalRelativeAngle(Utility.calcAngle(targetBot, me) - getGunHeadingRadians());
		if (getGunTurnRemaining() == 0 && getEnergy() > 6.10 && getGunHeat() == 0) {
			setFire(POWER);
		}
		setTurnGunRightRadians(angle);
	}

	public void movement() {
		if (targetPoint.distance(me) < 15 || idleTime > 25) {
			idleTime = 0;
			updateListLocations(PREDICTION_POINTS);
			// WE ARE HERE NEW POINT
			Point2D.Double lowRiskP = null;
			double risk = Double.MAX_VALUE;
			for (Point2D.Double p : possibleLocations) {
				if (evaluatePoint(p) <= risk || lowRiskP == null) {
					risk = evaluatePoint(p);
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
		} else {
			idleTime++;
			// GO TO POINT
			double angle = Math.atan2(targetPoint.x - me.x, targetPoint.y - me.y) - getHeadingRadians();
			double direction = 1;

			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}

			setAhead(targetPoint.distance(me) * direction);
			angle = Utils.normalRelativeAngle(angle);
			setTurnRightRadians(angle);
		}
	}

	public void updateListLocations(int n) {
		possibleLocations.clear();
		final int radius = 125;
		// Create x points in a radius pixel radius around the bot
		for (int i = 0; i < n; i++) {
			double randXMod = Utility.randomBetween(-radius, radius);
			double yRange = Math.sqrt(radius * radius - randXMod * randXMod);
			double randYMod = Utility.randomBetween(-yRange, yRange);
			double y = Utility.clamp(me.y + randYMod, 75, getBattleFieldHeight() - 75);
			double x = Utility.clamp(me.x + randXMod, 75, getBattleFieldWidth() - 75);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}

	public double evaluatePoint(Point2D.Double p) {
		Enumeration<Robot> _enum = enemies.elements();
		// You don't want to stay in one spot. Antigrav from starting point as
		// init value to enhance movement.
		double eval = Utility.randomBetween(0, 0.075) / p.distanceSq(me);
		while (_enum.hasMoreElements()) {
			Robot en = _enum.nextElement();
			eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
		}
		return eval;
	}

	public double[] normalizeRisk(double[] arr) {
		double maxRisk = Double.MIN_VALUE;
		for (int i = 0; i < arr.length; i++) {
			maxRisk = arr[i] > maxRisk ? arr[i] : maxRisk;
		}
		for (int i = 0; i < arr.length; i++) {
			arr[i] /= maxRisk;
		}
		return arr;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.BLUE);
		double[] risks = new double[possibleLocations.size()];
		for (int i = 0; i < risks.length; i++) {
			risks[i] = evaluatePoint(possibleLocations.get(i));
		}
		risks = normalizeRisk(risks);
		for (int i = 0; i < risks.length; i++) {
			Point2D.Double p = possibleLocations.get(i);
			double cChange = risks[i];
			g.setColor(new Color((float) (1 * cChange), 0f, (float) (1 * (1 - cChange)), 1f));
			g.drawOval((int) p.x, (int) p.y, 5, 5);
		}
		for (Robot e : enemies.values()) {
			if (!e.alive)
				g.setColor(new Color(0, 0, 0, 0));
			else
				g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
			g.drawRect((int) (e.x - 25), (int) (e.y - 25), 50, 50);
		}
		g.fillRect((int) targetPoint.x - 20, (int) targetPoint.y - 20, 40, 40);
	}

	public void onWin(WinEvent e) {
		onDeath(null);
	}

	public void onDeath(DeathEvent e) {
		finishes[getOthers()]++;
		for (int i = 0; i < finishes.length; i++)
			out.print(finishes[i] + " ");
		out.println();
	}
}
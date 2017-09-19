package jb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import robocode.AdvancedRobot;
import robocode.DeathEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

public class BadgeBot extends AdvancedRobot {

	// Static variables or objects in robocode keep their data from round to
	// round

	static final int PREDICTION_POINTS = 150;

	static int[] finishes;
	HashMap<String, Robot> enemies = new HashMap<>();

	Robot me = new Robot();
	Robot targetBot;

	List<Point2D.Double> possibleLocations = new ArrayList<>();
	Point2D.Double targetPoint = new Point2D.Double(60, 60);

	int idleTime = 30;

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
		targetBot.alive = false;

		while (true) {
			me.x = getX();
			me.y = getY();
			me.energy = getEnergy();
			me.gunHeadingRadians = getGunHeadingRadians();

			// If the robot isn't scanned in 20 turns get rid of it because all
			// the data is old and outdated
			Iterator<Robot> enemiesIter = enemies.values().iterator();
				while (enemiesIter.hasNext()) {
					Robot r = enemiesIter.next();
					if (getTime() - r.scanTime > 20) {
						//If the information is not updated lets just assume its dead so we don't shoot at it
						r.alive = false;
						if(r.name.equals(targetBot.name))
							targetBot.alive = false;
					}
				}			
			
			// Once the robot scans once and sees other robots start moving and
			// shooting
			if (getTime() > 9) {
				movement();
				if (targetBot.alive)
					shooting();
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
		// Setting/Updating enemy variables
		en.lastHeading = en.heading;
		en.name = e.getName();
		en.energy = e.getEnergy();
		en.alive = true;
		en.scanTime = getTime();
		en.velocity = e.getVelocity();
		en.bearingRadians = e.getBearingRadians();
		en.setLocation(
				new Point2D.Double(me.x + e.getDistance() * Math.sin(getHeadingRadians() + en.bearingRadians),
						me.y + e.getDistance() * Math.cos(getHeadingRadians() + en.bearingRadians)));
		en.heading = e.getHeadingRadians();
		en.shootableScore = en.energy < 25 ? en.energy < 5 ? en.distance(me) * 0.1 : en.distance(me) * 0.75 : en.distance(me);
		
		// If the target I was shooting at died switch to a new one or if a new
		// challenger has a lower shootableScore
		if (!targetBot.alive || en.shootableScore < targetBot.shootableScore)
			targetBot = en;
		// LOGIC NEEDED FOR 1v1 SUPER SAYAN MODE ACTIVATE
		if (getOthers() == 1) {
			// Nano Bot Lock - Very Simple
			setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		}
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		// If a robot is dead we need to know
		if (enemies.containsKey(event.getName())) {
			enemies.get(event.getName()).alive = false;
		}
		if(event.getName().equals(targetBot.name))
			targetBot.alive = false;
	}

	public void shooting() {
		if(targetBot != null && targetBot.alive){
			// It works I guess
			double dist = me.distance(targetBot);
			double power = ( dist > 850 ? 0.1 : (dist > 700 ? 0.5 : (dist > 250 ? 2.0 : 3.0)));
			power = Math.min( me.energy/4d, Math.min(targetBot.energy/3d, power));
			// Heads on Targeting
//			double angle = Utils.normalRelativeAngle(Utility.calcAngle(me, targetBot) - getGunHeadingRadians());
//			// If we are ready to fire the gun, FIRE
//			if (getGunTurnRemaining() == 0 && me.energy > 5.0 && getGunHeat() == 0) {
//				setFire(power);
//			}
//			// Turn after shooting so that there it no infinite cycle of
//			// getGunTurnRemaining() never being 0
//			setTurnGunRightRadians(angle);
			
			// perform lineair and circular targeting
			long deltahittime;
			Point2D.Double point = new Point2D.Double();
			double head, chead, bspeed;
			double tmpx, tmpy;

			// perform an iteration to find a reasonable accurate expected position
			tmpx = targetBot.getX();
			tmpy = targetBot.getY();
			head = targetBot.heading;
			chead = head - targetBot.lastHeading;
			point.setLocation( tmpx, tmpy);
			deltahittime = 0;
			do {
				tmpx += Math.sin(head) * targetBot.velocity;
				tmpy +=	Math.cos(head) * targetBot.velocity;
				head += chead;
				deltahittime++;
				Rectangle2D.Double fireField = new Rectangle2D.Double(20, 20, getBattleFieldWidth()- 20, getBattleFieldHeight() - 20);
				// if position not in field, adapt
				if (!fireField.contains( tmpx, tmpy)) {
					bspeed = point.distance(me) / deltahittime;
					power = Math.max( Math.min( (20 - bspeed) / 3.0, 3.0), 0.1);
					break;
				}
				point.setLocation( tmpx, tmpy);
			} while ( (int)Math.round( (point.distance( me) - 18) / Rules.getBulletSpeed( power)) > deltahittime);
			point.setLocation( Math.min( getBattleFieldWidth()  - 34, Math.max( 34, tmpx)), Math.min( getBattleFieldHeight() - 34, Math.max( 34, tmpy)));
			
			// If the gun is too hot or not aligned, don't bother trying to fire.
			if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0) && (power > 0.0) && (getEnergy() > 0.1)) {
				// Only fire the gun when it is locked and loaded, do a radarsweep afterwards.
				setFire( power);
			}
			// Turn gun after eventual firing (robocode does it also this way)
			setTurnGunRightRadians( Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2( point.y - me.getY(), point.x - me.getX())) - getGunHeadingRadians()));
		}
	}

	public void movement() {
		if (targetPoint.distance(me) < 15 || idleTime > 25) {
			// Reset idle time, I'm at my location or took too long to get there
			idleTime = 0;
			// Get a new array of points
			updateListLocations(PREDICTION_POINTS);

			// Lowest Risk Point
			Point2D.Double lowRiskP = null;
			// Current Risk Value
			double lowestRisk = Double.MAX_VALUE;
			for (Point2D.Double p : possibleLocations) {
				// Make sure that if lowRiskP is not assigned yet give it a new
				// value no matter what
				double currentRisk = evaluatePoint(p);
				if (currentRisk <= lowestRisk || lowRiskP == null) {
					lowestRisk = currentRisk;
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
		} else {
			// Increase idle time if still not at position
			idleTime++;
			// GO TO POINT
			double angle = Utility.calcAngle(me, targetPoint) - getHeadingRadians();
			double direction = 1;
			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction *= -1;
			}
			// If Math.cos(angle) is negative its faster to go backwards and
			// turn than going forwards and turn much more
			setMaxVelocity( 10 - (4 * Math.abs(getTurnRemainingRadians())));
			setAhead(me.distance(targetPoint) * direction);
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
		// You don't want to stay in one spot. Antigrav from starting point as
		// init value to enhance movement.
		double eval = Utility.randomBetween(1, 1.5) / p.distanceSq(me);
		//PRESET ANTIGRAV POINTS
		//If its a 1v1 the center is fine. You can use getOthers to see if its a 1v1.
		eval += (getOthers() > 6 ? 3 : (getOthers()-3))  / p.distanceSq(getBattleFieldWidth()/2, getBattleFieldHeight()/2);
		double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 1 : -0.025 : -0.05;
		eval += cornerFactor / p.distanceSq(0, 0);
		eval += cornerFactor / p.distanceSq(getBattleFieldWidth(), 0);
		eval += cornerFactor / p.distanceSq(0, getBattleFieldHeight());
		eval += cornerFactor / p.distanceSq(getBattleFieldWidth(), getBattleFieldHeight());
		
		if(targetBot.alive){
			double botangle = Utils.normalRelativeAngle( Utility.calcAngle(p, targetBot) - Utility.calcAngle(me, p));
			Iterator<Robot> enemiesIter = enemies.values().iterator();
			while (enemiesIter.hasNext()) {
				Robot en = enemiesIter.next();
				//(1 / p.distanceSq(en)) AntiGrav stuff
				//(en.energy / me.energy) How dangerous a robot it
				//(1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2) Better to move perpendicular to the target bot
				//(1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians())) If my heading is pointing towards the enemy its bad
				eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2) * (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
			}
			return eval;
		} else if(enemies.values().size() >= 1) {
			Iterator<Robot> enemiesIter = enemies.values().iterator();
			while (enemiesIter.hasNext()) {
				Robot en = enemiesIter.next();
				eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
			}
			return eval;
		} else {
			eval += (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
			return eval;
		}
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
			double cChange = Utility.clamp(risks[i],0,1);
			g.setColor(new Color((float) (1 * cChange), 0f, (float) (1 * (1 - cChange)), 1f));
			g.drawOval((int) p.x, (int) p.y, 5, 5);
		}
		for (Robot e : enemies.values()) {
			if (!e.alive)
				g.setColor(new Color(0, 0, 0, 0));
			else
				g.setColor(new Color(255,50,0,255/2));
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
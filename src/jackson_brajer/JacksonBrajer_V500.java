package jackson_brajer;

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

public class JacksonBrajer_V500 extends AdvancedRobot {

    // Static variables or objects in robocode keep their data from round to round
    static final int PREDICTION_POINTS = 150;

    //Keep track of each finish to see how well my robot does FROM ** http://robowiki.net/wiki/Melee_Strategy **
    static int[] finishes;
    //Keep track of all my enemy and their data for movement and aiming calculations
    HashMap<String, Robot> enemies = new HashMap<>();

    //My own information to limit the get funtion usage and the targetBot to know what I am aiming at after a scan
    Robot me = new Robot();
    Robot targetBot;

    //List of all my possible movement points
    List<Point2D.Double> possibleLocations = new ArrayList<>();
    //The lowest risked point from the possiblePoints
    Point2D.Double targetPoint = new Point2D.Double(60, 60);

    //To limit getBattleFieldHeight and Width calls
    Rectangle2D.Double battleField = new Rectangle2D.Double();

    //Time before I force a new target point
    int idleTime = 30;

    public void run() {
	battleField.height = getBattleFieldHeight();
	battleField.width = getBattleFieldWidth();

	if (finishes == null)
	    finishes = new int[getOthers() + 1];

	//Need to make my robot a nice colour. The better the skins the better the wins
	setColors(new Color(28, 98, 219), new Color(28, 212, 219), new Color(131, 0, 255), new Color(226, 220, 24), new Color(255, 255, 255));
	setAdjustGunForRobotTurn(true);
	setAdjustRadarForGunTurn(true);
	setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

	//Initial variable update
	me.x = getX();
	me.y = getY();
	me.energy = getEnergy();

	targetPoint.x = me.x;
	targetPoint.y = me.y;

	updateListLocations(PREDICTION_POINTS);

	targetBot = new Robot();
	targetBot.alive = false;

	while (true) {
	    //Update my variables every loop call
	    me.lastHeading = me.heading;
	    me.heading = getHeadingRadians();
	    me.x = getX();
	    me.y = getY();
	    me.energy = getEnergy();
	    me.gunHeadingRadians = getGunHeadingRadians();

	    // If the robot isn't scanned in 25 ticks get rid of it because all the data is old and outdated
	    Iterator<Robot> enemiesIter = enemies.values().iterator();
	    while (enemiesIter.hasNext()) {
		Robot r = enemiesIter.next();
		if (getTime() - r.scanTime > 25) {
		    // If the information is not updated lets just assume its dead so we don't shoot at it
		    r.alive = false;
		    if (targetBot.name != null && r.name.equals(targetBot.name))
			targetBot.alive = false;
		}
	    }

	    // Once the robot scans once and sees other robots start moving and shooting
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
	en.bearingRadians = e.getBearingRadians();
	en.setLocation(new Point2D.Double(me.x + e.getDistance() * Math.sin(getHeadingRadians() + en.bearingRadians), me.y + e.getDistance() * Math.cos(getHeadingRadians() + en.bearingRadians)));
	en.lastHeading = en.heading;
	en.name = e.getName();
	en.energy = e.getEnergy();
	en.alive = true;
	en.scanTime = getTime();
	en.velocity = e.getVelocity();
	en.heading = e.getHeadingRadians();
	//Based on robot distance and energy chose my best enemy to shoot at
	en.shootableScore = en.energy < 25 ? en.energy < 5 ? en.energy == 0 ? Double.MIN_VALUE : en.distance(me) * 0.1 : en.distance(me) * 0.75 : en.distance(me);
	
	// LOGIC NEEDED FOR 1v1 SUPER SAYAN MODE ACTIVATE
	if (getOthers() == 1) {
	    // Nano Bot Lock - Very Simple
	    setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
	}

	// If the target I was shooting at died switch to a new one or if a new challenger has a lower shootableScore
	if (!targetBot.alive || en.shootableScore < targetBot.shootableScore)
	    targetBot = en;

    }

    public void onRobotDeath(RobotDeathEvent event) {
	// If a robot is dead we need to know
	if (enemies.containsKey(event.getName())) {
	    enemies.get(event.getName()).alive = false;
	}
	if (event.getName().equals(targetBot.name))
	    targetBot.alive = false;
    }

    public void shooting() {
	if (targetBot != null && targetBot.alive) {
	    double dist = me.distance(targetBot);
	    double power = (dist > 850 ? 0.1 : (dist > 700 ? 0.5 : (dist > 250 ? 2.0 : 3.0)));
	    power = Math.min(me.energy / 4d, Math.min(targetBot.energy / 3d, power));
	    power = Utility.clamp(power, 0.1, 3.0);
	    
	    //Circular targeting which also works as linear targeting due to the heading change being 0 in linear
	    long deltahittime;
	    Point2D.Double shootAt = new Point2D.Double();
	    double head, chead, bspeed;
	    double tmpx, tmpy;

	    /*
	     * FROM GREIZEL Robot on the RoboWiki page
	     */

	    //Setting up variables
	    tmpx = targetBot.getX();
	    tmpy = targetBot.getY();
	    head = targetBot.heading;
	    chead = head - targetBot.lastHeading;
	    shootAt.setLocation(tmpx, tmpy);
	    deltahittime = 0;
	    
	    do {
		//Add to x and y based on the velocity and the heading
		tmpx += Math.sin(head) * targetBot.velocity;
		tmpy += Math.cos(head) * targetBot.velocity;
		//For circular targeting the heading will always change in a theoretical universe by the same change in heading
		head += chead;
		deltahittime++;
		Rectangle2D.Double fireField = new Rectangle2D.Double(18, 18, battleField.width - 36, battleField.height - 36);
		// if position not in field shoot at the current best location
		if (!fireField.contains(tmpx, tmpy)) {
		    //The best bullet speed is the distance over the calculated time
		    bspeed = shootAt.distance(me) / deltahittime;
		    // Clamping the bullet speed to a reasonable amount
		    power = Utility.clamp((20 - bspeed) / 3.0, 0.1, 3.0);
		    break;
		}
		//Change the current location to the current one
		shootAt.setLocation(tmpx, tmpy);
	    } while ((int) Math.round((shootAt.distance(me) - 18) / Rules.getBulletSpeed(power)) > deltahittime); //Repeat until the bullet distance / speed or velocity >= the time taken. A variation of d=v*t.
	    shootAt.setLocation(Utility.clamp(tmpx, 34, getBattleFieldWidth() - 34), Utility.clamp(tmpy, 34, getBattleFieldHeight() - 34));
	    if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0) && (power > 0.0) && (me.energy > 0.1)) {
		// Only fire the gun is ready
		setFire(power);
	    }
	    // Turn gun after firing so that the gun is not in an infinitely not ready to fire loop
	    setTurnGunRightRadians(Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2(shootAt.y - me.getY(), shootAt.x - me.getX())) - getGunHeadingRadians()));
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
	    //If Math.cos(angle) is negative its faster to go backwards and turn than going forwards and turn much more
	    if (Math.cos(angle) < 0) {
		//Math.PI in radians is half so changing the turn by 180
		angle += Math.PI;
		direction *= -1;
	    }
	    //Increase velocity as my remaining amount to turn becomes less
	    setMaxVelocity(10 - (4 * Math.abs(getTurnRemainingRadians())));
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
	    //yRange is dependant on the x current value to create a circle
	    double yRange = Math.sqrt(radius * radius - randXMod * randXMod);
	    double randYMod = Utility.randomBetween(-yRange, yRange);
	    double y = Utility.clamp(me.y + randYMod, 75, battleField.height - 75);
	    double x = Utility.clamp(me.x + randXMod, 75, battleField.width - 75);
	    possibleLocations.add(new Point2D.Double(x, y));
	}
    }

    public double evaluatePoint(Point2D.Double p) {
	// You don't want to stay in one spot. Antigrav from starting point as
	// init value to enhance movement.
	double eval = Utility.randomBetween(0.75, 2) / p.distanceSq(me);
	// PRESET ANTIGRAV POINTS
	// If its a 1v1 the center is fine. You can use getOthers to see if its a 1v1.
	eval += (6 * (getOthers() - 1)) / p.distanceSq(battleField.width / 2, battleField.height / 2);
	double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 0.25 : 0.5 : 1;
	eval += cornerFactor / p.distanceSq(0, 0);
	eval += cornerFactor / p.distanceSq(battleField.width, 0);
	eval += cornerFactor / p.distanceSq(0, battleField.height);
	eval += cornerFactor / p.distanceSq(battleField.width, battleField.height);
	
	if (targetBot.alive) {
	    double botangle = Utils.normalRelativeAngle(Utility.calcAngle(p, targetBot) - Utility.calcAngle(me, p));
	    Iterator<Robot> enemiesIter = enemies.values().iterator();
	    while (enemiesIter.hasNext()) {
		Robot en = enemiesIter.next();
		// (1 / p.distanceSq(en)) AntiGrav stuff
		// (en.energy / me.energy) How dangerous a robot it
		// (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2) Better to move perpendicular to the target bot
		// (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p)))) Worse if the enemy is closer to the point than I am in heading
		eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2) * (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p))));
	    }
	} else if (enemies.values().size() >= 1) {
	    Iterator<Robot> enemiesIter = enemies.values().iterator();
	    while (enemiesIter.hasNext()) {
		Robot en = enemiesIter.next();
		eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p))));
	    }
	} else {
	    eval += (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
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
	    double cChange = Utility.clamp(risks[i], 0, 1);
	    g.setColor(new Color((float) (1 * cChange), 0f, (float) (1 * (1 - cChange)), 1f));
	    g.drawOval((int) p.x, (int) p.y, 5, 5);
	}
	for (Robot e : enemies.values()) {
	    if (!e.alive)
		g.setColor(new Color(0, 0, 0, 0));
	    else
		g.setColor(new Color(255, 50, 0, 255 / 2));
	    
	    g.drawRect((int) (e.x - 25), (int) (e.y - 25), 50, 50);
	}
	g.setColor(new Color(255, 0, 0, 255 / 2));
	g.fillRect((int) (targetPoint.x - 20), (int) (targetPoint.y - 20), 40, 40);
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
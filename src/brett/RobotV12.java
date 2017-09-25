package brett;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class RobotV12 extends AdvancedRobot {
	
	boolean needPoint = true;
	boolean kamikaze = false;
	double oldHealth;
	Point bestPoint = new Point();
	ArrayList<Point> points = new ArrayList<Point>();
	ArrayList<Robot> robots = new ArrayList<Robot>();
	ArrayList<Bullet> bullets = new ArrayList<Bullet>();
	boolean noRobot = true;
	boolean oneVS;
	
	double height;
	double width;
	Rectangle bounds;

	public void run() {
		height = this.getBattleFieldHeight();
		width  = this.getBattleFieldWidth();
		bounds = new Rectangle(50, 50, (int) (width - 100), (int) (height - 100));
		
		bestPoint = new Point((int) getX(), (int) getY());
		
		// Set colors
		setBodyColor(new Color(9, 35, 157));
		setGunColor(new Color(31, 172, 252));
		setRadarColor(new Color(0, 100, 100));
		setBulletColor(new Color(255, 255, 100));
		setScanColor(new Color(255, 200, 200));

		this.setAdjustRadarForGunTurn(true);
		this.setAdjustRadarForRobotTurn(true);
		this.setAdjustGunForRobotTurn(true);
		
		oldHealth = this.getEnergy();
		
		// Loop forever
		do{
			System.out.println("Time " + getTime());
			for(int i = 0; i < bullets.size(); i++){
				bullets.get(i).move();
				System.out.println(getTime() + "  " + i);
				int x = (int) bullets.get(i).cX;
				int y = (int) bullets.get(i).cY;
				
				if(x < -100 || y < -100 || x > 2100 || y > 2100){
					bullets.remove(i);
				}
			}	
			if(bullets.size() > 0 && getTime() % 10 == 0){
				generatePoints();
			}
			if(this.getOthers() == 1){
				oneVS = true;
			}
			if(needPoint){
				needPoint = false;
				generatePoints();
			}
			goTo(bestPoint);
			if(Math.hypot(Math.abs(getX() - bestPoint.x), Math.abs(getY() - bestPoint.y)) < 10){
				needPoint = true;
			}
			noRobot = true;
			scan();
			if(noRobot){
				this.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
				execute();
			}

		}while(true);
	}

	public void generatePoints(){
		points.clear();
		int cx, cy;
		if(kamikaze){
			cx = (int) (robots.get(0).getX() + getX()) / 2;
			cy = (int) (robots.get(0).getY() + getY()) / 2;
		}else{
			cx = (int) getX();
			cy = (int) getY();
		}
		int r = 400;
		int space = 50;
		for(int x = cx - r; x <= cx + r; x += space){
			for(int y = cy - r; y <= cy + r; y += space){
				int radius = Math.abs(x - cx) * Math.abs(x - cx) + Math.abs(y - cy) * Math.abs(y - cy);
				if(radius <= r * r && radius >= r * r / 4){
					points.add(new Point(x, y));
				}	
			}
		}
		calculateDanger();
		
		points.clear();
		cx = bestPoint.x;
		cy = bestPoint.y;
		r = 100;
		space = 7;
		for(int x = cx - r; x <= cx + r; x += space){
			for(int y = cy - r; y <= cy + r; y += space){
				int radius = Math.abs(x - cx) * Math.abs(x - cx) + Math.abs(y - cy) * Math.abs(y - cy);
				if(radius <= r * r){
					points.add(new Point(x, y));
				}	
			}
		}
		calculateDanger();
	}
	
	public void goTo(Point p){
		double dx = p.getX() - this.getX();
		double dy = p.getY() - this.getY();
		boolean reverse = false;
		// Calculate angle to target
		double turn = Math.atan2(dx, dy);
		turn = Utils.normalRelativeAngle(turn - getHeadingRadians());

		if(turn > Math.PI / 2){
			turn -= Math.PI;
			reverse = true;
		}else if(turn < -(Math.PI / 2)){
			turn += Math.PI;
			reverse = true;
		}
		
		this.setTurnRightRadians(turn);
//		this.waitFor(new TurnCompleteCondition(this));
		double distance = Math.sqrt(dx * dx + dy * dy);
		if(reverse){
			setBack(distance);
		}else{
			setAhead(distance);
		}
	}
	
	public void calculateDanger(){
		double lowestDanger = Double.MAX_VALUE;
		int bestPointIndex = 0;
		
		for(int pIndex = 0; pIndex < points.size(); pIndex++){
			Point p = points.get(pIndex);
			
			if(bounds.contains(p) == false){
				points.remove(pIndex);
				pIndex--;
			}else{
				double danger = 0;
				
				for(int i = 0; i < robots.size(); i++){
					double dx = Math.abs(robots.get(i).x - p.x);
					double dy = Math.abs(robots.get(i).y - p.y);
					//TODO fix this
					double distance = dx * dx + dy * dy;
					if(!kamikaze){
						danger += robots.get(i).health * robots.get(i).health / distance;
					}else{
						danger -= robots.get(i).health * robots.get(i).health / distance;
					}
				}
				
				for(int i = 0; i < bullets.size(); i++){
					Bullet b = bullets.get(i);
					double distance = Math.sqrt(b.cX * b.cX + b.cY + b.cY);
					danger += distance / 100;
				}
				
				if(oneVS){
					danger += Math.abs(p.getX() - width / 2) / 200000;
					danger += Math.abs(p.getY() - height / 2) / 200000;
				}
				
				if(danger < lowestDanger){
					lowestDanger = danger;
					bestPointIndex = pIndex;
				}
			}
		}
		
		bestPoint = points.get(bestPointIndex);
	}
	
	public void onPaint(Graphics2D g){
		for(int i = 0; i < points.size(); i++){
			g.fillOval(points.get(i).x, points.get(i).y, 7, 7);
		}
		g.setColor(Color.red);
		g.fillOval(bestPoint.x, bestPoint.y, 15, 15);
		
		for(int i = 0; i < robots.size(); i++){
			g.drawOval(robots.get(i).x - 25, robots.get(i).y - 25, 50, 50);
		}
		g.setColor(Color.green);
		for(int i = 0; i < bullets.size(); i++){
			g.drawOval(bullets.get(i).getX() - 10, bullets.get(i).getY() - 10, 20, 20);
		}
		
		g.drawOval((int) getX() - 200, (int) (getY() - 200), 400, 400);
	}
	
	//////////////////////SCANNED ROBOT EVENT//////////////////////////
	double oldEnemyHeading = 0;
	public void onScannedRobot(ScannedRobotEvent e) {
		noRobot = false;
		double dis = e.getDistance();
		double health = e.getEnergy();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		//Used on first ScannedRobotEvent to add a robot
		if(robots.size() == 0){
			robots.add(new Robot(e.getName()));
		}
		
		//1v1 scenario
		if(oneVS){
			System.out.println("Current time of scan " + getTime());
			Robot r = robots.get(0);
			double radarTurn = absoluteBearing
				// Subtract current radar heading to get turn required
				- getRadarHeadingRadians();
			//Magic turn fixing
			setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));
			
			double healthLoss = r.getHealth() - health;
			if(healthLoss <= 3 && healthLoss >= 0.1 && oldHealth - getEnergy() != 0.1){
				int type = 1;
				if(e.getName().substring(0, 5).equals("sample")){
					type = 0;
				}
				spawnBullets(healthLoss, type);
			}
			oldHealth = getEnergy();
			double power;
			if(health >= 16){
				power = 3;
			}else if(health > 4){
				power = (health + 2) / 6;
			}else{
				power = health / 4;
			}
			power = Math.min(power, getEnergy());
			
			double myX = getX();
			double myY = getY();
			double enemyX = getX() + dis * Math.sin(absoluteBearing);
			double enemyY = getY() + dis * Math.cos(absoluteBearing);
			double enemyHeading = e.getHeadingRadians();
			double enemyHeadingChange = enemyHeading - oldEnemyHeading;
			double enemyVelocity = e.getVelocity();
			oldEnemyHeading = enemyHeading;
			
			double deltaTime = 0;
			double predictedX = enemyX, predictedY = enemyY;
			while((++deltaTime) * (20.0 - 3.0 * power) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
				predictedX += Math.sin(enemyHeading) * enemyVelocity;
				predictedY += Math.cos(enemyHeading) * enemyVelocity;
				
				enemyHeading += enemyHeadingChange;
				if(predictedX < 18.0 || predictedY < 18.0 || predictedX > width - 18.0 || predictedY > height - 18.0){
					predictedX = Math.min(Math.max(18.0, predictedX), width - 18.0);
					predictedY = Math.min(Math.max(18.0, predictedY), height - 18.0);
					break;
				}
			}
			double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
			
			setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
			
//			kamikaze = getEnergy() - health >= 30;
			
			if(dis < 900 && getEnergy() > 4){
				setFire(power);
			}
			
		}else{
			//Determine minimum power to kill
			double power;
			if(health >= 16){
				power = 3;
			}else if(health > 4){
				power = (health + 2) / 6;
			}else{
				power = health / 4;
			}
			
			double myX = getX();
			double myY = getY();
			double enemyX = getX() + dis * Math.sin(absoluteBearing);
			double enemyY = getY() + dis * Math.cos(absoluteBearing);
			double enemyHeading = e.getHeadingRadians();
			double enemyHeadingChange = enemyHeading - oldEnemyHeading;
			double enemyVelocity = e.getVelocity();
			oldEnemyHeading = enemyHeading;
			
			double deltaTime = 0;
			double predictedX = enemyX, predictedY = enemyY;
			while((++deltaTime) * (20.0 - 3.0 * power) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
				predictedX += Math.sin(enemyHeading) * enemyVelocity;
				predictedY += Math.cos(enemyHeading) * enemyVelocity;
				
				enemyHeading += enemyHeadingChange;
				if(predictedX < 18.0 || predictedY < 18.0 || predictedX > width - 18.0 || predictedY > height - 18.0){
					predictedX = Math.min(Math.max(18.0, predictedX), width - 18.0);
					predictedY = Math.min(Math.max(18.0, predictedY), height - 18.0);
					break;
				}
			}
			double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
			
			setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
			if(dis < 700 && getEnergy() >= 4){
				if(getTime() % 20 == 0){
					needPoint = true;
					generatePoints();
				}
				
				//See 1v1 code
				double radarTurn = absoluteBearing - getRadarHeadingRadians();
				setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));
				
				
				setFire(power);
			}else if(health < 30 && getEnergy() > 4){
				//See 1v1 code
				double radarTurn = absoluteBearing - getRadarHeadingRadians();
				setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));
				setFire(power);
			}else{
				noRobot = true;
			}
			
			
		}
		
		//Loop through all robots to update their information
		boolean robotFound = false;
		for(int i = 0; i < robots.size() && !robotFound; i++){
			if(robots.get(i).name == e.getName()){
				robotFound = true;
				robots.get(i).setHealth(health);
				double angle = getHeadingRadians() + e.getBearingRadians();
				double enemyX = getX() + dis * Math.sin(angle);
				double enemyY = getY() + dis * Math.cos(angle);

				robots.get(i).setX((int) enemyX);
				robots.get(i).setY((int) enemyY);
			}else if(i == robots.size() - 1){
				robots.add(new Robot(e.getName()));
				i--;
			}
		}
		
//		if(!(e.getName().substring(0, 5).equals("Fisher")) && !oneVS && e.getDistance() < 500){
//			setFire(power);
//		}
		
	}
	
	public void onRobotDeath(RobotDeathEvent e){
		//Remove dead robots to not have inaccurate info
		String name = e.getName();
		for(int i = 0; i < robots.size(); i++){
			if(name.equals(robots.get(i).name)){
				robots.remove(i);
				break;
			}
		}
		
		//Check if the battle is now 1v1
		if(this.getOthers() == 1){
			oneVS = true;
		}
	}
	
	public void onHitRobot(HitRobotEvent e){
		
	}
	
	public void onHitByBullet(HitByBulletEvent e){
		kamikaze = false;
	}
	
	//Creates bullets to track
	public void spawnBullets(double power, int type){
		generatePoints();
		int rX = robots.get(0).getX();
		int rY = robots.get(0).getY();
		//Target being current location
		if(type == 0){
			bullets.add(new Bullet(rX, rY, getX(), getY(), power));
		}else if(type == 1){
			//target being linear tracking
			double enemyX = getX();
			double enemyY = getY();
			double myX = rX;
			double myY = rY;
			double enemyHeading = getHeadingRadians();
			double enemyVelocity = getVelocity();
			
			double deltaTime = 0;
			double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
			double predictedX = enemyX, predictedY = enemyY;
			while((++deltaTime) * (20.0 - 3.0 * power) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){
				predictedX += Math.sin(enemyHeading) * enemyVelocity;
				predictedY += Math.cos(enemyHeading) * enemyVelocity;
				
				if(predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0 || predictedY > battleFieldHeight - 18.0){
					predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);
					predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
					break;
				}
			}
			bullets.add(new Bullet(rX, rY, predictedX, predictedY, power));
		}else if(type == 2){
			
		}
		//TODO target being circular tracking
		//Is it worth it? Movement is already pretty linear
	}
}

package testerRobots.brett;

public class Bullet {

	//current x/y
	double cX;
	double cY;
	//target x/y
	int tX;
	int tY;
	
	//up & side velocity
	double upV, sideV;
	
	public Bullet(int x, int y, double targetX, double targetY, double power){
		cX = x;
		cY = y;
		tX = (int) targetX;
		tY = (int) targetY;
		double speed = 20 - 3 * power;
		double distance = Math.sqrt(Math.abs(x - targetX) * Math.abs(x - targetX) + Math.abs(y - targetY) * Math.abs(y - targetY));
		double ticks = distance / speed;
		upV = (y - targetY) / ticks;
		sideV = (x - targetX) / ticks;
		move();
	}
	
	public int getX(){
		return (int) cX;
	}
	
	public int getY(){
		return (int) cY;
	}
	
	public void move(){
		cX -= sideV;
		cY -= upV;
	}
	
//	public void moveBullet(int time){
//		cX -= sideV;
//		cY -= upV;
//	}
}

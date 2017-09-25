package testerRobots.brett;

public class Robot {

	public int x, y;
	public String name;
	public double health;
	public double bearing;
	//TODO add distance
	
	public Robot(String robotName){
		name = robotName;
	}
	
	public void setX(int X){
		x = X;
	}
	
	public void setY(int Y){
		y = Y;
	}
	
	public void setHealth(double energy){
		health = energy;
	}
	
	public void setBearing(double b){
		bearing = b;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public double getHealth(){
		return health;
	}
	
//	public double getBearing(){
//		return bearing;
//	}
	
}

package legorvegenine;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Region 
{
	String name;
	Location lowCorner;
	Location highCorner;
	Location startPosition;
	
	public Region(String name, World world, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3)
	{
		this.name = name;
		
		double lowerX = Math.min(x1, x2);
		double lowerY = Math.min(y1, y2);
		double lowerZ = Math.min(z1, z2);
		lowCorner = new Location(world, lowerX, lowerY, lowerZ);
		
		double higherX = Math.max(x1, x2);
		double higherY = Math.max(y1, y2);
		double higherZ = Math.max(z1, z2);
		highCorner = new Location(world, higherX, higherY, higherZ);
		
		startPosition = new Location(world, x3, y3, z3);
	}
	
	public boolean containsPlayer(Player p)
	{
		Location pLoc = p.getLocation();
		
		if(pLoc.getX() < lowCorner.getX() || highCorner.getX() < pLoc.getX())
			return false;
		if(pLoc.getY() < lowCorner.getY() || highCorner.getY() < pLoc.getY())
			return false;
		if(pLoc.getZ() < lowCorner.getZ() || highCorner.getZ() < pLoc.getZ())
			return false;
		
		return true;
	}
	
	public String[] info()
	{
		String[] result = new String[5];
		
		result[0] = "Name: " + name + "\n";
		result[1] = "World: " + startPosition.getWorld().getName();
		result[2] = "Low Corner: (" + lowCorner.getX() + ", " + lowCorner.getY() + ", " + lowCorner.getZ() + ")";
		result[3] = "High Corner: (" + highCorner.getX() + ", " + highCorner.getY() + ", " + highCorner.getZ() + ")";
		result[4] = "Starting Position: (" + startPosition.getX() + ", " + startPosition.getY() + ", " + startPosition.getZ() + ")";
		
		return result;
	}
	
}

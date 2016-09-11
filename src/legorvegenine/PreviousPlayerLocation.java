package legorvegenine;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PreviousPlayerLocation 
{
	Player p = null;
	Location loc = null;
	
	public PreviousPlayerLocation(Player p, Location loc)
	{
		this.p = p;
		this.loc = loc;
	}
	
	public boolean correctPlayer(Player p)
	{
		return this.p.equals(p);
	}
	
	public void returnPlayer()
	{
		p.teleport(loc);
	}
}

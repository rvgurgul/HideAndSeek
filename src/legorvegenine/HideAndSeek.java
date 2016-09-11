package legorvegenine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus; 

//TO-DO:
//Prevent leaving the region
//HIDERS.GET(0) ENDGAME THINGEE
//Change tellGroup to auto add Golden [H&S]
//Hiding player names
//Cosmetic features

public class HideAndSeek extends JavaPlugin implements Listener
{
	ArrayList<Player> queue = new ArrayList<Player>();
	ArrayList<Player> hiders = new ArrayList<Player>();
	ArrayList<Player> seekers = new ArrayList<Player>();
	
	Team hidersTeam, seekersTeam;
	
	ArrayList<PreviousPlayerLocation> locations = new ArrayList<PreviousPlayerLocation>();

	ArrayList<Region> regions = new ArrayList<Region>();
	List<String> regionNames = new ArrayList<String>();
	Region currentMap = null;
	
	boolean inProgress = false;
	boolean seekerCanTag = true;
	
	int timerCount = 0;
	int currentTask = -1;
	
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		
		regionNames = getConfig().getStringList("regionNames");
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		loadRegions();
		
		Scoreboard board = getServer().getScoreboardManager().getNewScoreboard();
		
		Team hidersTeam = board.registerNewTeam("hiders");
		hidersTeam.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OWN_TEAM);
		
		Team seekersTeam = board.registerNewTeam("seekers");
		seekersTeam.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.ALWAYS);
	}
	
	@Override
	public void onDisable()
	{
		saveRegions();
		saveConfig();
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (!(sender instanceof Player)) {return false;}
		Player p = (Player)sender;
		
		if (cmd.getName().equalsIgnoreCase("hideandseek"))
		{
			if (args.length > 0 && args[0].equalsIgnoreCase("start"))
			{
				if (!queue.contains(p))
				{
					p.sendMessage(ChatColor.RED + "You cannot start a game of Hide and Seek if you are not in queue.");
					p.sendMessage(ChatColor.RED + "Type: " + ChatColor.WHITE + "\"/hideandseek join\"" + ChatColor.RED + " to join the queue.");
					return true;
				}
				if (allPlayers().size() > 1) //SET TO 0 FOR TESTING
				{
					p.sendMessage(ChatColor.GREEN + "You have started the game of Hide & Seek.");
					pregameCountdown();
				}
				else
					p.sendMessage(ChatColor.RED + "There are not enough players in the queue to start a game of Hide and Seek!");
				return true;
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase("join"))
			{
				if(args.length > 1 && currentMap == null)
				{
					for(Region r : regions)
					{
						if(r.name.equalsIgnoreCase(args[1]))
						{
							currentMap = r;
						}
					}
				}
				
				if (hiders.contains(p) || seekers.contains(p) && inProgress)
				{
					p.sendMessage(ChatColor.RED + "You are already in the game of Hide & Seek.");
				}
				else if (queue.contains(p))
				{
					p.sendMessage(ChatColor.YELLOW + "You are already in the queue for the next game.");
					p.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.WHITE + "\"/hideandseek leave\"" + ChatColor.YELLOW + " to leave the queue.");
				}
				else
				{
					
					if(currentMap != null)
					{
						locations.add(new PreviousPlayerLocation(p, p.getLocation()));
						p.teleport(currentMap.startPosition);
					}
					
					if (inProgress)
					{
						p.sendMessage(ChatColor.YELLOW + "There is currently a game in progress.");
						p.sendMessage(ChatColor.YELLOW + "You have been added to the queue for next game.");
					}
					else
					{
						p.sendMessage(ChatColor.GREEN + "You have joined the queue for a game of Hide & Seek.");
						p.sendMessage(ChatColor.GREEN + "Type " + ChatColor.WHITE + "\"/hideandseek start\"" + ChatColor.GREEN + " to start the game.");
						
						tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + p.getName() + " has joined the queue.");
					}
					queue.add(p);
				}
				return true;
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase("leave"))
			{
				if (inProgress)
				{
					if (hiders.contains(p))
						hiders.remove(p);
					else if (seekers.contains(p))
						seekers.remove(p);
					else
						return true;

					p.sendMessage(ChatColor.GREEN + "You have left the game of Hide & Seek!");
					
					tellGroup(allPlayers(), ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + p.getName() + " has left the game of Hide & Seek.");
					
					if(allPlayers().size() < 2 || seekers.size() == 0)
						endGame();
				}
				else if (queue.contains(p))
				{
					queue.remove(p);
					
					for(int i = 0; i < locations.size(); i++)
					{
						if(locations.get(i).correctPlayer(p))
						{
							p.teleport(locations.get(i).loc);
							locations.remove(i);
						}
					}
					
					p.sendMessage(ChatColor.YELLOW + "You have been removed from the queue for next game.");
				}
				else
					p.sendMessage(ChatColor.RED + "You were not in the queue.");
				return true;
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase("status"))
			{
				if(!inProgress)
					p.sendMessage(ChatColor.RED + "There is currently no game of Hide & Seek.");
				else
				{
					p.sendMessage(ChatColor.GREEN + "There is currently a game of Hide & Seek:");
					p.sendMessage(ChatColor.GREEN + "Map: " + ChatColor.WHITE + currentMap.name);
					
					String hiderList = ChatColor.GREEN + "Hiders: " + ChatColor.WHITE;
					for(Player hider : hiders)
						hiderList += hider.getName();
					p.sendMessage(hiderList);
					
					String seekerList = ChatColor.GREEN + "Seekers: " + ChatColor.WHITE;
					for(Player seeker : seekers)
						seekerList += seeker.getName();
					p.sendMessage(seekerList);
				}
				return true;
			}
			
			if(p.hasPermission("canDefineNewRegions"))
			{
				if(args.length > 0 && args[0].equalsIgnoreCase("region"))
				{
					if(args.length > 2 && args[1].equalsIgnoreCase("remove"))
					{
						if(removeRegion(args[2]))
							p.sendMessage(ChatColor.GREEN + "Region \"" + args[2] + "\" was deleted.");
						else
							p.sendMessage(ChatColor.RED + "Region \"" + args[2] + "\" was not deleted.");
					}
					else if(args.length > 11 && args[1].equalsIgnoreCase("add"))
					//hideandseek region add name x1 y1 z1 x2 y2 z2 x3 y3 z3
					//			0	   1   2    3  4  5  6  7  8  9  10 11
					{
						for(String s : regionNames)
						{
							if(s.equalsIgnoreCase(args[2]))
							{
								p.sendMessage(ChatColor.RED + "A region with the name \"" + args[2] + "\" already exists.");
								return true;
							}
						}
						
						String name = args[2];
						World world = p.getWorld();
						double x1 = 0.0, y1 = 0.0, z1 = 0.0;
						double x2 = 0.0, y2 = 0.0, z2 = 0.0;
						double x3 = 0.0, y3 = 0.0, z3 = 0.0;
						
						try
						{
							x1 = Double.valueOf(args[3]);
							y1 = Double.valueOf(args[4]);
							z1 = Double.valueOf(args[5]);

							x2 = Double.valueOf(args[6]);
							y2 = Double.valueOf(args[7]);
							z2 = Double.valueOf(args[8]);
							
							x3 = Double.valueOf(args[9]);
							y3 = Double.valueOf(args[10]);
							z3 = Double.valueOf(args[11]);
						}
						catch (NumberFormatException e)
						{
							p.sendMessage(ChatColor.RED + "Invalid Arguments");
							p.sendMessage(ChatColor.RED + "/hideandseek region add name x y z x y z x y z");
							return true;
						}
						
						regionNames.add(name);
						regions.add(new Region(name, world, x1, y1, z1, x2, y2, z2, x3, y3, z3));
						
						saveRegions();
						
						p.sendMessage(ChatColor.GREEN + "Successfully added new region \"" + name + "\"!");
					}
					else if(args.length > 1 && args[1].equalsIgnoreCase("list"))
					{
						p.sendMessage(ChatColor.GREEN + "List of all " + regions.size() + " regions: ");
						p.sendMessage(ChatColor.GREEN + "-----");
						for(Region r : regions)
						{
							p.sendMessage(r.name);
						}
						p.sendMessage(ChatColor.GREEN + "-----");
					}
					else if(args.length > 2 && args[1].equalsIgnoreCase("info"))
					{
						for(String s : regionNames)
						{
							if(s.equalsIgnoreCase(args[2]))
							{
								p.sendMessage(ChatColor.GREEN + "Info for region \"" + args[2] + "\"");
								for(Region r : regions)
								{
									if(r.name.equalsIgnoreCase(args[2]))
									{
										for(String info : r.info())
											p.sendMessage(info);
										break;
									}
								}
								return true;
							}
						}
						
						p.sendMessage(ChatColor.RED + "No info for region \"" + args[2] + "\" found");
					}
					return true; 
				}
			}
			
			p.sendMessage("Correct Usage for /hideandseek:");
			p.sendMessage(ChatColor.GREEN + "/hideandseek start");
			p.sendMessage(ChatColor.GREEN + "/hideandseek join [map]");
			p.sendMessage(ChatColor.GREEN + "/hideandseek status");
			p.sendMessage(ChatColor.GREEN + "/hideandseek leave");
			if(p.hasPermission("canDefineNewRegions"))
			{
				p.sendMessage(ChatColor.GREEN + "/hideandseek region add name x y z x y z x y z");
				p.sendMessage(ChatColor.GREEN + "/hideandseek region remove name");
				p.sendMessage(ChatColor.GREEN + "/hideandseek region list");
			}
			
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerTag(EntityDamageByEntityEvent e)
	{
		Player h = null;
		Player s = null;
		
		if ((e.getEntity() instanceof Player)) h = (Player)e.getEntity();
		if ((e.getDamager() instanceof Player)) s = (Player)e.getDamager();
		
		if(!seekerCanTag)
		{
			e.setCancelled(true);
			if(s != null && seekers.contains(s)) s.sendMessage(ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + "You cannot tag players right now.");
		}
		else if(h != null && hiders.contains(h) && s != null && seekers.contains(s))
		{
			e.setCancelled(true);
			
			hiders.remove(h);
			seekers.add(h);
			
			h.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15*60*20, 0, true));
			
			tellGroup(hiders, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + h.getName() + " was found by " + s.getName() + ".");
			tellGroup(hiders, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + s.getName() + " has found " + h.getName() + ".");
			
			if(hiders.size() == 1)
			{
				//Get the last hider, not hiders(0) b/c its null in the case of a 1v1.
				tellGroup(allPlayers(), ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + "The last remaining hider was " + hiders.get(0).getName());
				endGame();
			}
		}
	}
	
//	@EventHandler
//	public void onPlayerMovement(PlayerMoveEvent e)
//	{
//		if(!inProgress) return;
//		
//		Player p = e.getPlayer();
//		
//		if((hiders.contains(p) || seekers.contains(p)) && !currentMap.containsPlayer(p))
//		{
//			p.sendMessage(ChatColor.GOLD + "[H&S]" + ChatColor.RED + "You cannot exit the region!");
//		}
//	}
	
	public void startGame()
	{
		if(currentMap == null)
		{
			Random RNGesus = new Random();
			currentMap = regions.get(RNGesus.nextInt(regions.size()));
		}
		
		inProgress = true;
		seekerCanTag = false;
		
		tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + "The game of Hide & Seek on " + currentMap.name + " has begun!");
		
		hiders.addAll(queue);
		queue.clear();
		
		Random RNGesus = new Random();
		Player seeker = hiders.remove(RNGesus.nextInt(hiders.size()));
		seekers.add(seeker);
		
		tellGroup(hiders, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + seeker.getName() + " has randomly been chosen as the seeker!");
		tellGroup(seekers, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + "You have been chosen as the first seeker.");
		
		seeker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15*60*20, 0, true));
		seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 31*20, 0, true));
		seeker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30*20, 127, true));
		seeker.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 30*20, 128, true));
		
		for(Player p : allPlayers())
		{
			for(PreviousPlayerLocation loc : locations)
			{
				if(loc.correctPlayer(p))
					break;
			}
			locations.add(new PreviousPlayerLocation(p, p.getLocation()));
			
			p.teleport(currentMap.startPosition);
			showScoreboardToPlayer(p);
		}
		
		seekerBlindCountdown();
	}
	
	public void endGame()
	{
		getServer().getScheduler().cancelAllTasks();
		
		inProgress = false;
		currentMap = null;
		
		for(Player p : seekers)
			p.removePotionEffect(PotionEffectType.GLOWING);
		
		queue.addAll(hiders);
		queue.addAll(seekers);
		hiders.clear();
		seekers.clear();
		
		for(Player p : queue)
			p.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());

		tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + "The game of Hide & Seek has ended!" );
		tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + "You have been added back into the queue.");
		tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.YELLOW + "Type " + ChatColor.WHITE + "\"/hideandseek leave\"" + ChatColor.YELLOW + " to leave the queue.");
		
		while(locations.size() > 0)
		{
			locations.get(0).returnPlayer();
			locations.remove(0);
		}
	}
	
	public void tellGroup(ArrayList<Player> group, String message)
	{
		for(Player p : group)
			p.sendMessage(message);
	}
	
	public ArrayList<Player> allPlayers()
	{
		ArrayList<Player> all = new ArrayList<Player>();
		all.addAll(queue);
		all.addAll(hiders);
		all.addAll(seekers);
		return all;
	}
	
	public void showScoreboardToPlayer(Player p)
	{
		Scoreboard board = getServer().getScoreboardManager().getNewScoreboard();
		
		Objective objective = board.registerNewObjective("hideAndSeek", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.GOLD + "Hide & Seek");
		
		Score timeScore = objective.getScore(ChatColor.YELLOW + "Time: ");
		timeScore.setScore(600);
		
		Score seekerScore = objective.getScore(ChatColor.YELLOW + "Seekers: ");
		seekerScore.setScore(seekers.size());
		
		Score hiderScore = objective.getScore(ChatColor.YELLOW + "Hiders: ");
		hiderScore.setScore(hiders.size());
		
		p.setScoreboard(board);
	}
	
	public void setScoreboardValueForPlayer(Player p, int value)
	{
		Scoreboard board = p.getScoreboard();
		
		Objective objective = board.getObjective("hideAndSeek");
		objective.setDisplayName(ChatColor.GOLD + "Hide & Seek");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		Score score = objective.getScore(ChatColor.YELLOW + "Time: ");
		score.setScore(value);

		Score seekerScore = objective.getScore(ChatColor.YELLOW + "Seekers: ");
		seekerScore.setScore(seekers.size());
		
		Score hiderScore = objective.getScore(ChatColor.YELLOW + "Hiders: ");
		hiderScore.setScore(hiders.size());
	}
	
	public void saveRegions()
	{
		getConfig().set("regionNames", regionNames);
		
		for(Region r : regions)
		{
			String rName = r.name;
			
			getConfig().set(rName + "X1", r.lowCorner.getX());
			getConfig().set(rName + "Y1", r.lowCorner.getY());
			getConfig().set(rName + "Z1", r.lowCorner.getZ());
			
			getConfig().set(rName + "X2", r.highCorner.getX());
			getConfig().set(rName + "Y2", r.highCorner.getY());
			getConfig().set(rName + "Z2", r.highCorner.getZ());
			
			getConfig().set(rName + "X3", r.startPosition.getX());
			getConfig().set(rName + "Y3", r.startPosition.getY());
			getConfig().set(rName + "Z3", r.startPosition.getZ());
			
			int worldIndex = getServer().getWorlds().indexOf(r.startPosition.getWorld());
			getConfig().set(rName + "worldIndex", worldIndex);
		}
		
		saveConfig();
	}
	
	public void loadRegions()
	{
		regionNames = getConfig().getStringList("regionNames");
		for(String s : regionNames)
		{
			double x1 = getConfig().getDouble(s + "X1");
			double y1 = getConfig().getDouble(s + "Y1");
			double z1 = getConfig().getDouble(s + "Z1");
			
			double x2 = getConfig().getDouble(s + "X2");
			double y2 = getConfig().getDouble(s + "Y2");
			double z2 = getConfig().getDouble(s + "Z2");
			
			double x3 = getConfig().getDouble(s + "X3");
			double y3 = getConfig().getDouble(s + "Y3");
			double z3 = getConfig().getDouble(s + "Z3");
			
			int worldIndex = getConfig().getInt(s + "World");
			World world = getServer().getWorlds().get(worldIndex);
			
			regions.add(new Region(s, world, x1, y1, z1, x2, y2, z2, x3, y3, z3));
		}
	}
	
	public boolean removeRegion(String name)
	{
		for(String s : regionNames)
		{
			if (s.equalsIgnoreCase(name))
			{
				regionNames.remove(s);
				getConfig().set("regionNames", regionNames);
				
				for(int i = 0; i < regions.size(); i++)
				{
					if (regions.get(i).name.equalsIgnoreCase(name))
						regions.remove(i);
				}
				
				getConfig().set(name + "X1", null);
				getConfig().set(name + "Y1", null);
				getConfig().set(name + "Z1", null);

				getConfig().set(name + "X2", null);
				getConfig().set(name + "Y2", null);
				getConfig().set(name + "Z2", null);

				getConfig().set(name + "X3", null);
				getConfig().set(name + "Y3", null);
				getConfig().set(name + "Z3", null);
				
				getConfig().set(name + "worldIndex", null);
				
				saveRegions();
				return true;
			}
		}
		return false;
	}
	
	private void pregameCountdown()
	{
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			private int seconds = 20;
			
			@Override
			public void run() 
			{
				if(seconds % 5 == 0 || seconds <= 5)
					tellGroup(queue, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + "The game of Hide & Seek will begin in " + seconds + " seconds.");
				
				seconds--;
				
				if(seconds == 0)
				{
					startGame();
					return;
				}
			}
		}, 0, 20);
	}
	
	private void seekerBlindCountdown()
	{
		currentTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			private int seconds = 30;
			private String pName = seekers.get(0).getName();
			
			@Override
			public void run() 
			{
				if(seconds % 5 == 0 || seconds <= 5)
				{
					tellGroup(hiders, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + pName + " will be released in " + seconds + " seconds.");
					tellGroup(seekers, ChatColor.GOLD + "[H&S]" + ChatColor.WHITE + "You will be released in " + seconds + " seconds.");
				}
				
				seconds--;
				
				if(seconds == 0)
				{
					getServer().getScheduler().cancelTask(currentTask);
					mainGameTimer();
				}
			}
		}, 0, 20);
	}
	
	private void mainGameTimer()
	{
		seekerCanTag = true;
		currentTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			private int seconds = 600;
			//private int seconds = 10; //TESTING TIME
			
			@Override
			public void run() 
			{
				for(Player p : allPlayers())
					setScoreboardValueForPlayer(p, seconds);
				
				seconds--;
				
				if(seconds < 0)
				{
					getServer().getScheduler().cancelTask(currentTask);
					endGame();
				}
			}
		}, 0, 20);
	}
}

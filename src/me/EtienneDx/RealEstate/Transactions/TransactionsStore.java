package me.EtienneDx.RealEstate.Transactions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class TransactionsStore
{
    public final String dataFilePath = RealEstate.pluginDirPath + "transactions.data";
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();

    public HashMap<String, ClaimSell> claimSell;
    public HashMap<String, ClaimRent> claimRent;
    public HashMap<String, ClaimLease> claimLease;
    
    public TransactionsStore()
    {
    	loadData();
    	new BukkitRunnable()
    	{
			
			@Override
			public void run()
			{
				Iterator<ClaimRent> ite = claimRent.values().iterator();
				while(ite.hasNext())
				{
					if(ite.next().update())
						ite.remove();
				}

				Iterator<ClaimLease> it = claimLease.values().iterator();
				while(it.hasNext())
				{
					if(it.next().update())
						it.remove();
				}
				saveData();
			}
		}.runTaskTimer(RealEstate.instance, 1200L, 1200L);// run every 60 seconds
    }
    
    public void loadData()
    {
    	claimSell = new HashMap<>();
    	claimRent = new HashMap<>();
    	claimLease = new HashMap<>();
    	
    	File file = new File(this.dataFilePath);
    	
    	if(file.exists())
    	{
	    	FileConfiguration config = YamlConfiguration.loadConfiguration(file);
	    	try {
				RealEstate.instance.addLogEntry(new String(Files.readAllBytes(FileSystems.getDefault().getPath(this.dataFilePath))));
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	ConfigurationSection sell = config.getConfigurationSection("Sell");
	    	ConfigurationSection rent = config.getConfigurationSection("Rent");
	    	ConfigurationSection lease = config.getConfigurationSection("Lease");
	    	if(sell != null)
	    	{
	    		RealEstate.instance.addLogEntry(sell.toString());
	    		RealEstate.instance.addLogEntry(sell.getKeys(false).size() + "");
		    	for(String key : sell.getKeys(false))
				{
					ClaimSell cs = (ClaimSell)sell.get(key);
					claimSell.put(key, cs);
				}
	    	}
	    	if(rent != null)
	    	{
				for(String key : rent.getKeys(false))
				{
					ClaimRent cr = (ClaimRent)rent.get(key);
					claimRent.put(key, cr);
				}
			}
	    	if(lease != null)
	    	{
				for(String key : lease.getKeys(false))
				{
					ClaimLease cl = (ClaimLease)lease.get(key);
					claimLease.put(key, cl);
		    	}
			}
    	}
    }
    
    public void saveData()
    {
    	YamlConfiguration config = new YamlConfiguration();
        for (ClaimSell cs : claimSell.values())
            config.set("Sell." + cs.claimId, cs);
        for (ClaimRent cr : claimRent.values())
            config.set("Rent." + cr.claimId, cr);
        for (ClaimLease cl : claimLease.values())
            config.set("Lease." + cl.claimId, cl);
        try
        {
			config.save(new File(this.dataFilePath));
		}
        catch (IOException e)
        {
			RealEstate.instance.log.info("Unable to write to the data file at \"" + this.dataFilePath + "\"");
		}
    }
	
	public boolean anyTransaction(Claim claim)
	{
		return claim != null && 
				(claimSell.containsKey(claim.getID().toString()) || 
						claimRent.containsKey(claim.getID().toString()) || 
						claimLease.containsKey(claim.getID().toString()));
	}

	public Transaction getTransaction(Claim claim)
	{
		if(claimSell.containsKey(claim.getID().toString()))
			return claimSell.get(claim.getID().toString());
		if(claimRent.containsKey(claim.getID().toString()))
			return claimRent.get(claim.getID().toString());
		if(claimLease.containsKey(claim.getID().toString()))
			return claimLease.get(claim.getID().toString());
		return null;
	}

	public void cancelTransaction(Claim claim)
	{
		if(anyTransaction(claim))
		{
			Transaction tr = getTransaction(claim);
			cancelTransaction(tr);
		}
		saveData();
	}

	public void cancelTransaction(Transaction tr)
	{
		if(tr.getHolder() != null)
			tr.getHolder().breakNaturally();
		if(tr instanceof ClaimSell)
		{
			claimSell.remove(String.valueOf(((ClaimSell) tr).claimId));
		}
		if(tr instanceof ClaimRent)
		{
			claimRent.remove(String.valueOf(((ClaimRent) tr).claimId));
		}
		if(tr instanceof ClaimLease)
		{
			claimLease.remove(String.valueOf(((ClaimLease) tr).claimId));
		}
		saveData();
	}
	public void kickTransaction (Transaction tr)
	{
                if(tr instanceof ClaimSell)
                {
                        tr.update();
                }
                if(tr instanceof ClaimRent)
                {
                        tr.update();
                }
                if(tr instanceof ClaimLease)
                {
                        tr.update();
                }

	
	}	
	public boolean canCancelTransaction(Transaction tr)
	{
		return tr instanceof ClaimSell || (tr instanceof ClaimRent && ((ClaimRent)tr).buyer == null) || 
				(tr instanceof ClaimLease && ((ClaimLease)tr).buyer == null);
	}

	public void sell(Claim claim, Player player, double price, Location sign)
	{
		ClaimSell cs = new ClaimSell(claim, claim.isAdminClaim() ? null : player, price, sign);
		claimSell.put(claim.getID().toString(), cs);
		cs.update();
		saveData();
		
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.parent == null ? "claim" : "subclaim") + " for sale at " +
                "[" + claim.getGreaterBoundaryCorner().getWorld() + ", " +
                "X: " + claim.getGreaterBoundaryCorner().getBlockX() + ", " +
                "Y: " + claim.getGreaterBoundaryCorner().getBlockY() + ", " +
                "Z: " + claim.getGreaterBoundaryCorner().getBlockZ() + "] " +
                "Price: " + price + " " + RealEstate.econ.currencyNamePlural());
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.parent == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;

		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedSell,
					claimPrefix,
					claimTypeDisplay,
					RealEstate.econ.format(price));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedSellBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getDisplayName(),
							claimPrefix,
							claimTypeDisplay,
							RealEstate.econ.format(price));
				}
			}
		}
	}

	public void rent(Claim claim, Player player, double price, Location sign, int duration, int rentPeriods, boolean buildTrust)
	{
		
		ClaimRent cr = new ClaimRent(claim, claim.isAdminClaim() ? null : player, price, sign, duration, rentPeriods, buildTrust);
		claimRent.put(claim.getID().toString(), cr);
		cr.update();
		saveData();
		
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.parent == null ? "claim" : "subclaim") + " for" + (buildTrust ? "" : " container") + " rent at " +
				"[" + claim.getLesserBoundaryCorner().getWorld() + ", " +
                "X: " + claim.getLesserBoundaryCorner().getBlockX() + ", " +
                "Y: " + claim.getLesserBoundaryCorner().getBlockY() + ", " +
                "Z: " + claim.getLesserBoundaryCorner().getBlockZ() + "] " +
                "Price: " + price + " " + RealEstate.econ.currencyNamePlural());
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.parent == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
		
		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedRent,
					claimPrefix,
					claimTypeDisplay,
					RealEstate.econ.format(price),
					Utils.getTime(duration, null, false));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedRentBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getDisplayName(),
							claimPrefix,
							claimTypeDisplay,
							RealEstate.econ.format(price),
							Utils.getTime(duration, null, false));
				}
			}
		}
	}

	public void lease(Claim claim, Player player, double price, Location sign, int frequency, int paymentsCount)
	{
		ClaimLease cl = new ClaimLease(claim, claim.isAdminClaim() ? null : player, price, sign, frequency, paymentsCount);
		claimLease.put(claim.getID().toString(), cl);
		cl.update();
		saveData();
		
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) + 
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.parent == null ? "claim" : "subclaim") + " for lease at " +
				"[" + claim.getLesserBoundaryCorner().getWorld() + ", " +
                "X: " + claim.getLesserBoundaryCorner().getBlockX() + ", " +
                "Y: " + claim.getLesserBoundaryCorner().getBlockY() + ", " +
                "Z: " + claim.getLesserBoundaryCorner().getBlockZ() + "] " +
                "Payments Count : " + paymentsCount + " " + 
                "Price: " + price + " " + RealEstate.econ.currencyNamePlural());
	
		String claimPrefix = claim.isAdminClaim() ? RealEstate.instance.messages.keywordAdminClaimPrefix :
				RealEstate.instance.messages.keywordClaimPrefix;
		String claimTypeDisplay = claim.parent == null ? RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
		
		if(player != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimCreatedLease,
					claimPrefix,
					claimTypeDisplay,
					RealEstate.econ.format(price),
					paymentsCount + "",
					Utils.getTime(frequency, null, false));
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					Messages.sendMessage(p, RealEstate.instance.messages.msgInfoClaimCreatedLeaseBroadcast,
							player == null ? RealEstate.instance.messages.keywordTheServer : player.getDisplayName(),
							claimPrefix,
							claimTypeDisplay,
							RealEstate.econ.format(price),
							paymentsCount + "",
							Utils.getTime(frequency, null, false));
				}
			}
		}
	}

	public Transaction getTransaction(Player player)
	{
		if(player == null) return null;
		Claim c = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
		return getTransaction(c);
	}
}

package me.EtienneDx.RealEstate.Transactions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.earth2me.essentials.User;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;

import me.EtienneDx.RealEstate.AbandonedItems;
import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatColor;

public class ClaimRent extends BoughtTransaction
{
	LocalDateTime startDate = null;
	int duration;
	public boolean autoRenew = false;
	public boolean buildTrust = true;
	public int periodCount = 0;
	public int maxPeriod;
	
	public ClaimRent(Map<String, Object> map)
	{
		super(map);
		if(map.get("startDate") != null)
			startDate = LocalDateTime.parse((String) map.get("startDate"), DateTimeFormatter.ISO_DATE_TIME);
		duration = (int)map.get("duration");
		autoRenew = (boolean) map.get("autoRenew");
		periodCount = (int) map.get("periodCount");
		maxPeriod = (int) map.get("maxPeriod");
		try {
			buildTrust = (boolean) map.get("buildTrust");
		}
		catch (Exception e) {
			buildTrust = true;
		}
	}
	
	public ClaimRent(Claim claim, Player player, double price, Location sign, int duration, int rentPeriods, boolean buildTrust)
	{
		super(claim, player, price, sign);
		this.duration = duration;
		this.maxPeriod = RealEstate.instance.config.cfgEnableRentPeriod ? rentPeriods : 1;
		this.buildTrust = buildTrust;
		if((claim.isAdminClaim()&&RealEstate.instance.config.RestoreAdminOnly)|| !RealEstate.instance.config.RestoreAdminOnly) //if we are in an admin claim *and* admin only is selected, or if admin only is false
		{
			//if worldedit saving is requested, here's where i think... to do it..
			if(RealEstate.instance.getServer().getPluginManager().getPlugin("WorldEdit")!=null) //is world edit installed?
			{
				if(RealEstate.instance.config.RestoreRentalState)//are we configured to use it?
				{
				
					Location lesser=claim.getLesserBoundaryCorner();
					Location greater=claim.getGreaterBoundaryCorner();
					CuboidRegion region = new CuboidRegion(BlockVector3.at(lesser.getX(), lesser.getWorld().getMinHeight(), lesser.getZ()),BlockVector3.at(greater.getX(),greater.getWorld().getMaxHeight(),greater.getZ()));
					BlockArrayClipboard clipboard = new BlockArrayClipboard (region);
					com.sk89q.worldedit.world.World adaptedworld= BukkitAdapter.adapt(lesser.getWorld());
					EditSession editSession=WorldEdit.getInstance().newEditSession(adaptedworld);
					ForwardExtentCopy CopyArea=new ForwardExtentCopy(editSession,region,clipboard,region.getMinimumPoint());
					CopyArea.setCopyingEntities(false);
					
					
					
					try {
						Operations.complete(CopyArea);
					} catch (WorldEditException e) {
						
						RealEstate.instance.log.info("Failed to copy rental area, WorldEdit gives error: "+e.getMessage());
					}
					
					String schempath = RealEstate.pluginDirPath + "/schematics/"+claim.getID().toString()+".schem";
					File file = new File(schempath);
					try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
						writer.write(clipboard);
					}
					catch (Exception e)
					{
						RealEstate.instance.log.info("Failed to copy rental area, Writing out schematic failed: "+e.getMessage());
					}
					
					
				}
				
			}
		}
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = super.serialize();

		if(startDate != null)
			map.put("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
		map.put("duration", duration);
		map.put("autoRenew",  autoRenew);
		map.put("periodCount", periodCount);
		map.put("maxPeriod", maxPeriod);
		map.put("buildTrust", buildTrust);
		
		return map;
	}

	@Override
	public boolean update()
	{
		if(buyer == null)
		{
			if(sign.getBlock().getState() instanceof Sign)
			{
				Sign s = (Sign) sign.getBlock().getState();
				s.setWaxed(true);
				s.setLine(0, Messages.getMessage(RealEstate.instance.config.cfgSignsHeader, false));
				s.setLine(1, ChatColor.DARK_GREEN + RealEstate.instance.config.cfgReplaceRent);
				//s.setLine(2, owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "SERVER");
				String price_line = "";
				if(RealEstate.instance.config.cfgUseCurrencySymbol)
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price);
					}
					else
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + price;
					}

				}
				else
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = (int)Math.round(price) + " " + RealEstate.econ.currencyNamePlural();
					}
					else
					{
						price_line = price + " " + RealEstate.econ.currencyNamePlural();
					}
				}
				String period = (maxPeriod > 1 ? maxPeriod + "x " : "") + Utils.getTime(duration, null, false);
				if(this.buildTrust) {
					s.setLine(2, price_line);
					s.setLine(3, period);
				} else {
					s.setLine(2, RealEstate.instance.config.cfgContainerRentLine);
					s.setLine(3, price_line + " - " + period);
				}
				s.update(true);
			}
			else
			{
				return true;
			}
		}
		else
		{
			// we want to know how much time has gone by since startDate
			int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
			Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
			if(hours.isNegative() && !hours.isZero())
	        {
	            hours = hours.plusHours(24);
	            days--;
	        }
			if(days >= duration)// we exceeded the time limit!
			{
				payRent();
			}
			else if(sign.getBlock().getState() instanceof Sign)
			{
				Sign s = (Sign) sign.getBlock().getState();
				s.setLine(0, ChatColor.GOLD + RealEstate.instance.config.cfgReplaceOngoingRent); //Changed the header to "[Rented]" so that it won't waste space on the next line and allow the name of the player to show underneath.
				s.setLine(1, Utils.getSignString(Bukkit.getOfflinePlayer(buyer).getName()));//remove "Rented by"
				s.setLine(2, "Time remaining : ");
				
				int daysLeft = duration - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				s.setLine(3, Utils.getTime(daysLeft, timeRemaining, false));
				s.update(true);
			}
		}
		return false;
		
	}
	public void save_inventories(Claim claim,String mybuyer)
	{
		if(!RealEstate.instance.config.SaveInventory) {
			return;
					
		}
		String item_owner=mybuyer;//uuid saved when we started the transaction
		//so now we need to scan through the entire claim, and find any inventory to save X.x
		//get chunks within area, get inventories, check if inventory is within the bounds, then process.
		List<Chunk> chunksToProcess=new ArrayList<Chunk>();
		Location min=claim.getLesserBoundaryCorner();
		Location max=claim.getGreaterBoundaryCorner();
		World world=min.getWorld();
		
		List<Inventory> lifeboat=new ArrayList<Inventory>();
		int invsize=0;
		
		for (double x=min.getX();x<max.getX();x+=16) {
			for(double z=min.getZ();z<max.getZ();z+=16) 
			{
				
			//	chunksToProcess.add(world.getChunkAt((int)x,(int)z));
				//Chunk chunk=world.getChunkAt((int)x,(int)z);
				Location current=new Location(max.getWorld(),x,0,z);
				Chunk chunk=world.getChunkAt(current);
				chunksToProcess.add(chunk);
					
			}
			
		}
		//now we have all the chunks involved, now to find inventories...
		AbandonedItems item_saver=new AbandonedItems(mybuyer);
		for(Chunk chunk:chunksToProcess)
		{
			for(BlockState tileEntity:chunk.getTileEntities()) 
			{
				if (tileEntity.getX()>min.getX()&&tileEntity.getX()<max.getX()) 
				{
					if (tileEntity.getZ()>min.getZ()&&tileEntity.getZ()<max.getZ()) 
					{
						try 
						{
							if(tileEntity.getClass().getMethod("getInventory") != null) 
							{

								Method method= tileEntity.getClass().getMethod("getInventory");

								Inventory found_inventory=(Inventory) method.invoke(tileEntity, null);
								for(ItemStack j : found_inventory.getStorageContents()) 
								{
									if(j !=null) {
								
										item_saver.add_item(j);
									}
									else
									{
										continue;
									}
								}
								//blow away the container after we save everything
								
								Location inventoryloc=tileEntity.getLocation();
								
								world.setType(inventoryloc, Material.AIR);
								
							}
						} 
						catch (Exception e) 
						{
							// TODO Auto-generated catch block
							//RealEstate.instance.log.info("");
							//nothing to do here, we know some of these are not going to have this method, we don't care.

							RealEstate.instance.log.info("exception raised: "+e.getMessage());
						}
					}
				}
			}
		}
		item_saver.save();
		//ok, so now i have an array list of inventories, and how many stacks of items we have.... now what?
		//well bukkit needs a size that is a factor of 9, so next highest factor of 9?
		/*
		 * while((invsize %9)!=0) {
		 * RealEstate.instance.log.info("Inventory size was only "
		 * +invsize+" not divisible by 9, incrementing"); invsize++; //increase invsize
		 * until it is divisible by 9 } //move everything into one inventory Inventory
		 * saved_inventory=Bukkit.createInventory(null, invsize); for (Inventory
		 * passengers:lifeboat) { for(ItemStack j:passengers.getStorageContents()) {
		 * RealEstate.instance.log.info("Moving item stack of "+j.getType());
		 * saved_inventory.addItem(j);
		 * 
		 * } }
		 */
		
		
		
		
	}
	public static void restore_rental(Claim claim)
	{
		
		//if worldedit saving is requested, here's where I load the saved area
		if((claim.isAdminClaim()&&RealEstate.instance.config.RestoreAdminOnly)|| !RealEstate.instance.config.RestoreAdminOnly) //if we are in an admin claim *and* admin only is selected, or if admin only is false
		{
			if(RealEstate.instance.getServer().getPluginManager().getPlugin("WorldEdit")!=null) //is world edit installed?
			{
				if(RealEstate.instance.config.RestoreRentalState)//are we configured to use it?
				{
					//load schematic, paste where we got it.
					String schempath = RealEstate.pluginDirPath + "/schematics/"+claim.getID().toString()+".schem";
					File file = new File(schempath);
					Clipboard clipboard =null;
					ClipboardFormat format= ClipboardFormats.findByFile(file);
					try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
						clipboard = reader.read();
					}
					catch (Exception e) 
					{
						RealEstate.instance.log.info("Failed to import previously saved schematic: "+e.getMessage());
		
						return;
					}
					Location lesser=claim.getLesserBoundaryCorner();
					
					
					com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(lesser.getWorld());
					 
					try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
						
													
						
						BlockType air=new BlockType("minecraft:air");//we want to capture all the air from the saved schematic, to empty out the space that used to be empty
						BlockTypeMask mask= new BlockTypeMask(clipboard,air);//create a mask, specifying we want to keep just the air from the schematic			
						Operation operation = new ClipboardHolder(clipboard)
								.createPaste(editSession)
								.to(BlockVector3.at(lesser.getX(), lesser.getWorld().getMinHeight(), lesser.getZ()))
								.maskSource(mask)//ignore non-air blocks from the schematic.
								.ignoreAirBlocks(false) //well that would be silly, we want the air
				            	.build();
				    	Operations.complete(operation);
					}
					catch(Exception e)
					{
						RealEstate.instance.log.info("Failed to paste initial schematic: "+e.getMessage());
						return;
						
					}
				}
						
			}
		}
	}
	private void unRent(boolean msgBuyer)
	{
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null);
		claim.dropPermission(buyer.toString());
		claim.managers.remove(buyer.toString());
		claim.setSubclaimRestrictions(false);
		GriefPrevention.instance.dataStore.saveClaim(claim);
		if(msgBuyer && Bukkit.getOfflinePlayer(buyer).isOnline() && RealEstate.instance.config.cfgMessageBuyer)
		{
			String location = "[" + sign.getWorld().getName() + ", X: " + sign.getBlockX() + ", Y: " + 
					sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
			String claimType = claim.parent == null ?
					RealEstate.instance.messages.keywordClaim :
					RealEstate.instance.messages.keywordSubclaim;

			Messages.sendMessage(Bukkit.getPlayer(buyer), RealEstate.instance.messages.msgInfoClaimInfoRentCancelled,
					claimType,
					location);
		}
		save_inventories(claim,buyer.toString());
		buyer = null;
		RealEstate.transactionsStore.saveData();
		
		
		ClaimRent.restore_rental(claim);
		update();
	}

	private void payRent()
	{
		if(buyer == null) return;

		OfflinePlayer buyerPlayer = Bukkit.getOfflinePlayer(this.buyer);
		OfflinePlayer seller = owner == null ? null : Bukkit.getOfflinePlayer(owner);
		
		String claimType = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null).parent == null ?
				RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
		String location = "[" + sign.getWorld().getName() + ", X: " + sign.getBlockX() + ", Y: " + 
				sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]";
		
		if((autoRenew || periodCount + 1 < maxPeriod) && Utils.makePayment(owner, this.buyer, price, false, false))
		{
			periodCount = (periodCount + 1) % maxPeriod;
			startDate = LocalDateTime.now();
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.getPlayer(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyer,
						claimType,
						location,
						RealEstate.econ.format(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
        	{
        		User u = RealEstate.ess.getUser(this.buyer);
				u.addMail(Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyer,
						claimType,
						location,
						RealEstate.econ.format(price)));
        	}
			
			if(seller != null)
			{
				if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
				{
					Messages.sendMessage(seller.getPlayer(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.getName(),
							claimType,
							location,
							RealEstate.econ.format(price));
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = RealEstate.ess.getUser(this.owner);
					u.addMail(Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentOwner,
							buyerPlayer.getName(),
							claimType,
							location,
							RealEstate.econ.format(price)));
	        	}
			}
			
		}
		else if (autoRenew)
		{
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				Messages.sendMessage(buyerPlayer.getPlayer(), RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyerCancelled,
						claimType,
						location,
						RealEstate.econ.format(price));
			}
			else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
        	{
        		User u = RealEstate.ess.getUser(this.buyer);
				u.addMail(Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentPaymentBuyerCancelled,
						claimType,
						location,
						RealEstate.econ.format(price)));
        	}
			unRent(false);
			return;
		}
		else
		{
			unRent(true);
			return;
		}
		update();
		RealEstate.transactionsStore.saveData();
	}
	
	@Override
	public boolean tryCancelTransaction(Player p, boolean force)
	{
		if(buyer != null)
		{
			if(p.hasPermission("realestate.admin") && force == true)
			{
				this.unRent(true);
				RealEstate.transactionsStore.cancelTransaction(this);
				return true;
			}
			else
			{
				Claim claim = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null);
				if(p != null) {
					Messages.sendMessage(p, RealEstate.instance.messages.msgErrorCantCancelAlreadyRented,
						claim.parent == null ?
							RealEstate.instance.messages.keywordClaim :
							RealEstate.instance.messages.keywordSubclaim
						);
				}
	            return false;
			}
		}
		else
		{
			RealEstate.transactionsStore.cancelTransaction(this);
			return true;
		}
	}

	@Override
	public void interact(Player player)
	{
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null);// getting by id creates errors for subclaims
		if(claim == null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimDoesNotExist);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		String claimType = claim.parent == null ? "claim" : "subclaim";
		String claimTypeDisplay = claim.parent == null ? 
			RealEstate.instance.messages.keywordClaim : RealEstate.instance.messages.keywordSubclaim;
		
		if (owner != null && owner.equals(player.getUniqueId()))
        {
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyOwner, claimTypeDisplay);
            return;
        }
		if(claim.parent == null && owner != null && !owner.equals(claim.ownerID))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNotRentedByOwner, claimTypeDisplay);
            RealEstate.transactionsStore.cancelTransaction(claim);
            return;
		}
		if(!player.hasPermission("realestate." + claimType + ".rent"))
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoRentPermission, claimTypeDisplay);
            return;
		}
		if(player.getUniqueId().equals(buyer) || buyer != null)
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimAlreadyRented, claimTypeDisplay);
            return;
		}
		
		if(Utils.makePayment(owner, player.getUniqueId(), price, false, true))// if payment succeed
		{
			buyer = player.getUniqueId();
			startDate = LocalDateTime.now();
			autoRenew = false;
			periodCount = 0;
			claim.setPermission(buyer.toString(), buildTrust ? ClaimPermission.Build : ClaimPermission.Inventory);
			claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Manage);
			claim.managers.add(player.getUniqueId().toString());
			claim.setSubclaimRestrictions(true);
			GriefPrevention.instance.dataStore.saveClaim(claim);
			update();
			RealEstate.transactionsStore.saveData();
			
			RealEstate.instance.addLogEntry(
                    "[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.getName() + 
                    " has rented a " + claimType + " at " +
                    "[" + player.getLocation().getWorld() + ", " +
                    "X: " + player.getLocation().getBlockX() + ", " +
                    "Y: " + player.getLocation().getBlockY() + ", " +
                    "Z: " + player.getLocation().getBlockZ() + "] " +
                    "Price: " + price + " " + RealEstate.econ.currencyNamePlural());

			if(owner != null)
			{
				OfflinePlayer seller = Bukkit.getOfflinePlayer(owner);
				String location = "[" + sign.getWorld().getName() + ", " + 
						"X: " + sign.getBlockX() + ", " + 
						"Y: " + sign.getBlockY() + ", " + 
						"Z: " + sign.getBlockZ() + "]";
			
				if(RealEstate.instance.config.cfgMessageOwner && seller.isOnline())
				{
					Messages.sendMessage(seller.getPlayer(), RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.getName(),
						claimTypeDisplay,
						RealEstate.econ.format(price),
						location);
				}
				else if(RealEstate.instance.config.cfgMailOffline && RealEstate.ess != null)
	        	{
	        		User u = RealEstate.ess.getUser(this.owner);
					u.addMail(Messages.getMessage(RealEstate.instance.messages.msgInfoClaimOwnerRented,
						player.getName(),
						claimTypeDisplay,
						RealEstate.econ.format(price),
						location));
	        	}
			}
			
			Messages.sendMessage(player, RealEstate.instance.messages.msgInfoClaimBuyerRented,
				claimTypeDisplay,
				RealEstate.econ.format(price));
			
			destroySign();
		}
	}

	@Override
	public void preview(Player player)
	{
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null);
		if(player.hasPermission("realestate.info"))
		{
			String claimType = claim.parent == null ? "claim" : "subclaim";
			String claimTypeDisplay = claim.parent == null ? 
				RealEstate.instance.messages.keywordClaim :
				RealEstate.instance.messages.keywordSubclaim;
			String msg;
			msg = Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentHeader) + "\n";
			if(buyer == null)
			{
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralRentNoBuyer,
						claimTypeDisplay,
						RealEstate.econ.format(price),
						Utils.getTime(duration, null, true)) + "\n";
				if(maxPeriod > 1)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentMaxPeriod,
							maxPeriod + "") + "\n";
				}

				if(claimType.equalsIgnoreCase("claim"))
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoOwner,
							claim.getOwnerName()) + "\n";
	            }
	            else
	            {
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoMainOwner,
	            			claim.parent.getOwnerName()) + "\n";
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoNote) + "\n";
	            }
			}
			else
			{
				int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
				Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
				if(hours.isNegative() && !hours.isZero())
		        {
		            hours = hours.plusHours(24);
		            days--;
		        }
				int daysLeft = duration - days - 1;// we need to remove the current day
				Duration timeRemaining = Duration.ofHours(24).minus(hours);
				
				msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoGeneralRentBuyer,
						claimTypeDisplay,
						Bukkit.getOfflinePlayer(buyer).getName(),
						RealEstate.econ.format(price),
						Utils.getTime(daysLeft, timeRemaining, true),
						Utils.getTime(duration, null, true)) + "\n";
				
				if(maxPeriod > 1 && maxPeriod - periodCount > 0)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentRemainingPeriods,
							(maxPeriod - periodCount) + "") + "\n";
				}

				if((owner != null && owner.equals(player.getUniqueId()) || buyer.equals(player.getUniqueId())) && RealEstate.instance.config.cfgEnableAutoRenew)
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoRentAutoRenew,
						autoRenew ? 
							RealEstate.instance.messages.keywordEnabled :
							RealEstate.instance.messages.keywordDisabled) + "\n";
				}
				if(claimType.equalsIgnoreCase("claim"))
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoOwner,
							claim.getOwnerName()) + "\n";
				}
				else
				{
					msg += Messages.getMessage(RealEstate.instance.messages.msgInfoClaimInfoMainOwner,
							claim.parent.getOwnerName()) + "\n";
				}
			}
			Messages.sendMessage(player, msg, false);
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgErrorClaimNoInfoPermission);
		}
	}

	@Override
	public void msgInfo(CommandSender cs)
	{
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(sign, false, null);
		String location = "[" + claim.getLesserBoundaryCorner().getWorld().getName() + ", " +
		"X: " + claim.getLesserBoundaryCorner().getBlockX() + ", " +
		"Y: " + claim.getLesserBoundaryCorner().getBlockY() + ", " +
		"Z: " + claim.getLesserBoundaryCorner().getBlockZ() + "]";

		Messages.sendMessage(cs, RealEstate.instance.messages.msgInfoClaimInfoRentOneline,
				claim.getArea() + "",
				location,
				RealEstate.econ.format(price),
				Utils.getTime(duration, Duration.ZERO, false));
	}

}

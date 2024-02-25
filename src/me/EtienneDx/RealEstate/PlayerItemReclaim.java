package me.EtienneDx.RealEstate;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.inventory.Inventory;


public class PlayerItemReclaim implements Listener
{
	void registerEvents()
	{
		PluginManager pm = RealEstate.instance.getServer().getPluginManager();

		pm.registerEvents(this, RealEstate.instance);
	}
	
	public static void reclaimItems(Player player, int page) {
		// Get the items from AbandonedItems
		AbandonedItems player_items=new AbandonedItems(player.getUniqueId().toString());
		
		List<ItemStack> items = player_items.get_items();
		// Create an inventory for the player to hold the items
		int number=items.size();
		if(number==0) 
		{
			Messages.sendMessage(player, "You don't have any items in storage!");
			return;
			
		}
		int roundedNumber=0;
		/*
		if (number > 54) {
			roundedNumber = 54;
		} else {
			roundedNumber = (number + 9 - 1) / 9 * 9;
		}*/
		roundedNumber=(int)(Math.ceil(number / 9.0) * 9);
		if(roundedNumber>54) 
		{
			roundedNumber=54;
			
		}

		Inventory inv = Bukkit.createInventory(null, roundedNumber, "Reclaimed Items");
		if(page>items.size()) 
		{

			page=0;
		}
		// Add the items to the inventory
		
		int i = page;
		while (i < items.size() && i < roundedNumber) {
			inv.setItem(i - page, items.get(i));
			i++;
		}
		// Give the inventory to the player
		player.openInventory(inv);
		
		
		
		
	}
	@EventHandler
	public void OnInventoryCloseEvent(InventoryCloseEvent event) 
	{

		String title=event.getView().getTitle();
		if(title =="Reclaimed Items") 
		{
			Inventory inv =event.getInventory();

			HumanEntity player= event.getPlayer();
			AbandonedItems reclaimed_items=new AbandonedItems(player.getUniqueId().toString());

			List<ItemStack> remaining_items=new ArrayList<ItemStack>();
			for(ItemStack a: inv.getContents()) 
			{

				if(a != null) 
				{
					RealEstate.instance.log.info("Top inventory still had: "+a.getType());
					remaining_items.add(a);
				}
			}
			reclaimed_items.set_items(remaining_items);

		}


	}
}

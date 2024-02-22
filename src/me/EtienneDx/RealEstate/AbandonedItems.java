package me.EtienneDx.RealEstate;


import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.Arrays;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class AbandonedItems extends YamlConfiguration

{
	
	public static List<ItemStack>Saved_Items;
	public static String owner;
	private File file;
	//add items
	//save items
	//load items
	//expunge items
	public AbandonedItems(String item_owner) 
	{
		if(!RealEstate.instance.config.SaveInventory) {
			RealEstate.instance.log.info("Item saving is disabled, but an attempt to call the storage class was made, whoops");
			return;
					
		}
		owner=item_owner;
		Saved_Items=new ArrayList<ItemStack>();
		String path= RealEstate.pluginDirPath + "/Abandoned_inventories/"+item_owner+".inventory";
		file=new File(path);
		if(file.exists())//user has stuff already saved, load it
		{
			load();
		}
	}
	public String getOwner() 
	{
		return owner;
		
	}
	public void add_item(ItemStack addme) 
	{
		int remaining=addme.getAmount();
		RealEstate.instance.log.info("Attempting to add: "+addme.getType()+" of stack amount "+addme.getAmount());
		for(ItemStack a : Saved_Items) 
		{
		
			if(a.getType()==addme.getType()) 
			{
				RealEstate.instance.log.info("Found an existing stack, combining: "+a.getType()+" With existing size of "+a.getAmount());
				int cursize=a.getAmount();
				int maxamount=a.getMaxStackSize();
				if(cursize<maxamount) //room to add more
				{
					RealEstate.instance.log.info("We went over the limit, adding what we can");
					cursize+=addme.getAmount();
					cursize=cursize>maxamount? maxamount:cursize;
					remaining -= maxamount-a.getAmount();
					a.setAmount(cursize);
					
					
				}
			}
			if(remaining >0)//there is atleast one more to put in 
			{
				RealEstate.instance.log.info("Remaining items: "+remaining+" adding remaining stack to saved items");
				addme.setAmount(remaining);//just in case we removed some earlier 
				Saved_Items.add(addme);
				remaining=0;
				
			}
			
			
		}
		if(Saved_Items.size()==0)//first item being put in, heh
		{
			RealEstate.instance.log.info("Adding first item to the save queue");
			Saved_Items.add(addme);
		}
		this.save();
		
	}
    @SuppressWarnings("unchecked")
	public void load() 
	{

    	YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
    	try {
    	Saved_Items=(List<ItemStack>) c.get("inventory");
    	}
    	catch(Exception e) 
    	{
    	//so the file either didn't exist when we tried to load it, or was malformed in some way.	
    		
    	}
    	//Saved_Items = (List<ItemStack>) c.get("inventory");
    	
    	//p.getInventory().setContents(content);
    	//Saved_Items=content;


		
	}
    public void delete_save()
    {
    	
    	file.delete();
    }
	public void save() 
	{
		
		
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		c.set("inventory", Saved_Items);
        try {
			c.save(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			RealEstate.instance.log.info("Failed to save, got exception: "+e.getMessage());
		}
	}
	public List<ItemStack> get_items()
	{
		
		return Saved_Items;
	}
	public static void purge_items(Player player) 
	{
		
		
		AbandonedItems abandoned=new AbandonedItems(player.getUniqueId().toString());
		
		//lets find if they have items to get

		List<ItemStack> recovered_items=abandoned.get_items();
		if(recovered_items.size()>0) {


			
			Location where=player.getLocation();
			for(ItemStack a: recovered_items) 
			{
				
				where.getWorld().dropItemNaturally(where, a);

			}
		}
		else
		{
			Messages.sendMessage(player, RealEstate.instance.messages.msgSaveHasNoItems);
		}
		abandoned.delete_save();
	}
}

		
	

 


package me.EtienneDx.RealEstate;


import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
		Saved_Items=new CopyOnWriteArrayList<ItemStack>();
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
	public void set_items(List<ItemStack> new_set) 
	{
		Saved_Items=new_set;
		if(Saved_Items.size()>0) 
		{
			save();
		}
		else
		{
			delete_save();
		}
		
	}
	public void add_item(ItemStack addme) 
	{
				
		int remaining=addme.getAmount();
		
		//for(ItemStack a : current_items)
		//	we want the last instance of any particular item stack, or 0 
		int last_stack=-1;
		
		for(int i=0;i<Saved_Items.size();i++)
		{
			ItemStack a=Saved_Items.get(i);
			if(a ==null) 
			{
				i++;
				continue;
			}
			if(a.getType()==addme.getType()) 
			{
				last_stack=i;
			}
		}
		//at this point last_stack either points to the latest stack of items, which means we need to do some jiggling around, or it is -1 meaning no items of addme.type are in the array.
		if(last_stack>=0) {
			ItemStack a=Saved_Items.get(last_stack);
			
			int cursize=a.getAmount();
			int maxamount=a.getMaxStackSize();
			if(cursize<maxamount) //room to add more
			{
				cursize+=addme.getAmount();
				if(cursize>maxamount) 
				{
					cursize=maxamount;
					remaining-=maxamount-a.getAmount();
					addme.setAmount(remaining);
					Saved_Items.add(addme);
				}
				a.setAmount(cursize);//reset the amount in the array;
			}
			else//cursize was *exactly* maxamount, so we just add the stack as-is.
			{
				Saved_Items.add(addme);
			}
		}
			
		if(last_stack==-1) {
			Saved_Items.add(addme);		
		}
		if(Saved_Items.size()==0)//first item being put in, heh
		{	
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

		
	

 


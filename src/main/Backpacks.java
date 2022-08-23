package main;

import java.util.HashMap;

import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.inventory.PlayerInventoryToChestEvent;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Storage;
import net.risingworld.api.objects.custom.CustomItem;
import net.risingworld.api.objects.custom.CustomRecipe;
import net.risingworld.api.utils.Animation;
import net.risingworld.api.utils.ImageInformation;
import net.risingworld.api.utils.ModelInformation;
import net.risingworld.api.utils.Utils;

/**
 * This is our main plugin class. It has to extend "Plugin", and implement the
 * methods "onEnable()" (which is called when the plugin is loaded) and
 * "onDisable()" (which is called when the plugin is unloaded).
 * 
 * @author red51
 */

public class Backpacks extends Plugin implements Listener {
	
	private HashMap<Player, Integer> lastPackOpened;
	
	
	public Backpacks() {
		lastPackOpened = new HashMap<Player, Integer>();
	}
	
	@EventMethod
    public void onPlayerDisconnect(final PlayerDisconnectEvent event) {
		lastPackOpened.remove(event.getPlayer());
	}
	
    
    //Final variable (constant) to define the UUID of the item (has to be unique!)
    public final String BACKPACK_UUID = "main.backpacks.custombackpack";
    
    //Constant which defines the amount of slots a backpack should have
    public final int BACKPACK_SLOTS = 48;
    
    public void onEnable(){
        //Create our new custom backpack item, make sure to use a unique ID (first parameter)
        CustomItem item = new CustomItem(BACKPACK_UUID, "backpack");
        
        //Load the model, texture and icon
        ModelInformation model = new ModelInformation(getPath() + "/assets/backpack.obj");
        ImageInformation texture = new ImageInformation(getPath() + "/assets/backpack.dds");
        ImageInformation icon = new ImageInformation(getPath() + "/assets/backpack_icon.png");
        
        //Assign the model, texture and icon (we want to scale up the model, that can be done with the "modelSize" parameter)
        item.setModel(model, texture, 2.46f);
        item.setIcon(icon);
        
        //Item can not be stacked (so max stack size is 1)
        item.setMaxStacksize(1);
        
        //Select a proper idle animation ("HoldPelt1" seems to be suitable)
        item.setPlayerIdleAnimation(Animation.HoldPelt1);
        
        //Set a suitable item position, rotation and first person body position / rotation.
        //This part is a little bit tricky: To get proper coordinates, you can use the console
        //command "debugitem" to change the item position with your arrow keys (hold SHIFT to
        //rotate it). Press RETURN to copy the coordinates to clipboard (press SHIFT+RETURN
        //to copy the rotation).
        //For the first person body position, use the command "debugplayerbody" 
        //(which just works like the debugitem-command)
        item.setItemPosition(-0.1937611f, -0.32028213f, 0.08882258f);
        item.setItemRotation(0.5116603f, -0.7077507f, -0.2687852f, 0.406277f);
        item.setFPBodyPosition(-0.8087011f, -0.43947953f, 0.76546186f);
        item.setFPBodyRotation(-0.1111441f, -0.033417337f, -0.083279766f, 0.9897447f);
        
        //Setup localized names. Since the game currently only supports English and German, we only set these names
        item.setLocalizedNames("en=Backpack", "de=Rucksack");
        
        //Now the important part: We set a custom secondary action, i.e. if the player
        //uses the right mouse button, this action is executed. In this case, we
        //want to open a chest/storage which is associated with the backpack. Since we don't
        //want any delay, we select a "triggerDelay" of 0f
        item.setSecondaryAction(Animation.HoldPelt1, 0f, (player, collision) -> {
            //We call a convenient function we just created for this purpose.
            //In order to get the item reference, we retrieve it directly from the player
            openBackpack(player, player.getEquippedItem());
        });
        
        //Add more variations (just other colors in this case)
        item.setVariation(1, new ImageInformation(getPath() + "/assets/backpack_red.dds"), new ImageInformation(getPath() + "/assets/backpack_red_icon.png"));
        item.setVariation(2, new ImageInformation(getPath() + "/assets/backpack_green.dds"), new ImageInformation(getPath() + "/assets/backpack_green_icon.png"));
        item.setVariation(3, new ImageInformation(getPath() + "/assets/backpack_yellow.dds"), new ImageInformation(getPath() + "/assets/backpack_yellow_icon.png"));
        item.setVariation(4, new ImageInformation(getPath() + "/assets/backpack_purple.dds"), new ImageInformation(getPath() + "/assets/backpack_purple_icon.png"));
        
        //Once everything is set, register the custom item to the server
        getServer().registerCustomItem(item);
        
        
        //CUSTOM RECIPE
        
        //Now the item is registered, but we also want to be able to craft the backpack 
        //at the loom, so we create a CustomRecipe for it
        CustomRecipe recipe = new CustomRecipe(BACKPACK_UUID, CustomRecipe.Type.CustomItem, 0, "Backpacks", "loom");
        
        //Model size for the preview. We want it slightly bigger than our actual item
        //(see modelSize parameter above, when the CustomItem was created)
        recipe.setPreviewSize(2.75f);
        
        //Set the ingredients required to craft this item
        recipe.setIngredients("200x aluminiumingot", "240x cloth", "100x leather", "10x mithrilingot");
        
        //For the name of the item, we just grab it from our actual custom item
        recipe.setLocalizedNames(item.getLocalizedNames());
        
        //Set up the actual names of the category
        recipe.setLocalizedCategories("en=Backpacks", "de=Rucksäcke");
        
        //Set up localized descriptions (english [en] and german [de])
        recipe.setLocalizedDescriptions("en=A basic backpack. \nHas room for " + BACKPACK_SLOTS + " items.", "de=Ein einfacher Rucksack. Bietet Platz für " + BACKPACK_SLOTS + " Items.");
        
        //Register the custom item to the server
        getServer().registerCustomRecipe(recipe);
        
        registerEventListener(this);
    }
    
    public void onDisable() {}

    
    @EventMethod
    public void onPlayerInventoryToChest(final PlayerInventoryToChestEvent event) {
    	Item item = event.getItem();
    	Item.Attribute attribute = item.getAttribute();
    	
    	boolean cancelTransfer = false;
    	//int chestId = event.getChestID();
    	
    	//Storage storage = getWorld().getStorage(chestId);
    	//event.getPlayer().sendTextMessage("Debug: Storage Item = " + storage.toString());
    	
    	if(attribute != null) {
    		if(attribute.toString().contains("backpack")) {
    			cancelTransfer = true;
    			event.getPlayer().sendTextMessage("SYSTEM: Backpacks cannot be added to this storage unit!");
    		}    		
    	}
    	
    	event.setCancelled(cancelTransfer);
    }
    
	@EventMethod
    public void onPlayerCommand(final PlayerCommandEvent event) {
		Player cmdPlayer = event.getPlayer();	
		String[] cmdParams = event.getCommand().split(" ");
		
		switch(cmdParams.length) {
			case 1:
				
				break;
			case 2:
				if(cmdParams[0].equals("/bp") && cmdParams[1].equals("open")) {
					boolean st = openBackpack(cmdPlayer, cmdPlayer.getEquippedItem());
					
					if( !st ) {
						//event.getPlayer().sendTextMessage("Debug: No pack equiped. Using last known storage id.");
						Integer storageId = lastPackOpened.get(cmdPlayer);
						
						if(storageId != null) {
							openBackpack(cmdPlayer, storageId);
							//event.getPlayer().sendTextMessage("Debug: Opened last known pack. Storage Id = " + storageId);
						} else {
							//event.getPlayer().sendTextMessage("Debug: No last known storage Id.");
						}
					} else {
						//event.getPlayer().sendTextMessage("Debug: Opened equiped pack.");
					}
				}

				break;
			default:
			
		}
	}
	
	private void openBackpack(Player player, Integer storageID){
		player.showStorage(storageID);
        player.playGameSound("donkey_panniers");
	}
    
    
    /**
     * This method opens the backpack for a certain player. Both the player and backpack
     * item are passed as parameters. The method looks for the backpack storage ID 
     * (which is stored as attribute) and opens the storage UI for the player
     * (showing the backpack "storage content").
     * @param player the player you want to open the backback for.
     * @param backpack the backpack you want to open.
     */
    private boolean openBackpack(Player player, Item backpack){
    	boolean retVal = false;
    	
        //To avoid a NPE: if backpack item is null, return
        if(backpack == null) return retVal;
        
        //We will store the id of the storage in the item attribute.
        //Check if the item has the attribute we're looking for.
        if(backpack.getAttribute() instanceof Item.CustomItemAttribute){
            Item.CustomItemAttribute attribute = (Item.CustomItemAttribute) backpack.getAttribute();
            
            //Variable for the storage ID. This is used below
            int storageID;
            
            //Check if the backpack / storage ID is set. Then we can retrieve it and open the according storage GUI
            if(attribute.hasAttribute("backpack_id")){
                //Just to make assurance doubly sure: check if the backpack id is an integer
                String backpack_id = attribute.getAttribute("backpack_id");
                if(Utils.StringUtils.isInteger(backpack_id)){
                    //Convert the string to an int
                    storageID = Integer.parseInt(backpack_id); 
                    retVal = true;
                }
                else{
                    //Apparently the backpack ID is broken. Either something went wrong,
                    //or another plugin overrided this attribute. In this case,
                    //we will send a message to the player (show status message for 2 seconds)
                    player.showStatusMessage("This backpack is broken!", 2);
                    
                    return retVal;
                }
            }
            //Otherwise create a new storage and store it in the attribute
            else{
                //Create a new storage
                Storage storage = getWorld().createNewStorage(BACKPACK_SLOTS);
                
                //Store the storage ID in the attribute. We can only store strings,
                //so we have to turn the integer ID into a string
                attribute.setAttribute("backpack_id", String.valueOf(storage.getID()));
                
                //Assign the storage ID to the variable
                storageID = storage.getID();
                retVal = true;
            }
            
            //Open storage GUI for player (that's why we had to store the storageID variable above)
            player.showStorage(storageID);

            //Play a sound effect. You could create a custom sound, but for this
            //example, one of the default game sounds is sufficient (sound of donkey panniers
            //seems to be suitable in this particular case)
            player.playGameSound("donkey_panniers");
            
            lastPackOpened.put(player,storageID);
        }
        
        return retVal;
    }
    
}

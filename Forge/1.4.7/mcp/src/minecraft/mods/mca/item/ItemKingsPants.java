/*******************************************************************************
 * ItemKingsPants.java
 * Copyright (c) 2013 WildBamaBoy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/

package mods.mca.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.mca.core.forge.ClientProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IArmorTextureProvider;

/**
 * Defines what the King's Pants is and how it behaves.
 */
public class ItemKingsPants extends ItemArmor implements IArmorTextureProvider
{
    /**
     * Constructor
     * 
     * @param 	id	The item's ID.
     */
    public ItemKingsPants(int id)
    {
        super(id, EnumArmorMaterial.GOLD, 0, 2);
        setItemName("MonarchPants");
        maxStackSize = 1;
        setCreativeTab(CreativeTabs.tabMisc);
        setIconIndex(15);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public String getTextureFile()
    {
    	return ClientProxy.items;
    }

	@Override
	public String getArmorTextureFile(ItemStack itemstack) 
	{
		return "/mods/mca/textures/armor/crown_layer_2.png";
	}
}

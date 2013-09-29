/*******************************************************************************
 * ItemWeddingRing.java
 * Copyright (c) 2013 WildBamaBoy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/

package mods.mca.item;

import java.util.List;

import mods.mca.core.forge.ClientProxy;
import mods.mca.core.util.LanguageHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Defines what the Wedding Ring is and how it behaves.
 */
public class ItemWeddingRing extends Item
{
    /**
     * Constructor
     * 
     * @param 	id	The item's ID.
     */
    public ItemWeddingRing(int id)
    {
        super(id);
        setItemName("Wedding Ring");
        maxStackSize = 1;
        setCreativeTab(CreativeTabs.tabMisc);
        setIconIndex(6);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public String getTextureFile()
    {
    	return ClientProxy.items;
    }
    
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List informationList, boolean unknown)
	{
		informationList.add(LanguageHelper.getString("information.weddingring.line1"));
		informationList.add(LanguageHelper.getString("information.weddingring.line2"));
		informationList.add(LanguageHelper.getString("information.weddingring.line3"));
	}
}
package com.pg85.otg.forge.util;

import com.pg85.otg.OTG;
import com.pg85.otg.forge.ForgeEngine;
import com.pg85.otg.forge.generator.OTGBiome;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraftforge.common.DimensionManager;

public abstract class WorldHelper
{
    public static boolean isVanillaWorld(World world)
    {
        int dimensionId = world.provider.getDimension();
        // If vanilla or TC, return overworld
        if (dimensionId == 0 || dimensionId == 1 || dimensionId == -1)
        {
            return true;
        }

        return false;
    }

    /**
     * Gets the generation id of the given biome. This is usually equal to the
     * id of the BiomeBase, but when using virtual biomes it may be different.
     *
     * @param biomeBase The biome to check.
     * @return The generation id.
     */
    public static int getGenerationId(Biome biomeBase)
    {
        if (biomeBase instanceof OTGBiome)
        {
            return ((OTGBiome) biomeBase).generationId;
        }
        return ((ForgeEngine)OTG.getEngine()).getBiomeRegistryId(biomeBase);
    }

    /**
     * Gets the name of the given world.
     * @param world The world.
     * @return The name.
     */
    public static String getName(World world)
    {
    	if(
			world.provider.getDimension() > 1 &&
			(
				(
					world.getWorldInfo() instanceof DerivedWorldInfo &&
					((DerivedWorldInfo)world.getWorldInfo()).delegate.getGeneratorOptions().equals("OpenTerrainGenerator")
				) || (
					world.getWorldInfo().getGeneratorOptions().equals("OpenTerrainGenerator")
				)
			)
		)
    	{
        	// This dimension was created by TC, use the given dimension name
        	return DimensionManager.getProviderType(world.provider.getDimension()).getName();
        }

        World defaultWorld = DimensionManager.getWorld(0);
        // If vanilla or we are dealing with an implementation that supports
        // multiple save handlers, return the world name
        if (isVanillaWorld(world) || (defaultWorld != null && world.getWorldInfo() != null && world.getSaveHandler() != defaultWorld.getSaveHandler()))
        {
            return world.getWorldInfo().getWorldName();
        }

        // This is the best we can do for Forge
        return world.provider.getSaveFolder();
    }

    private WorldHelper()
    {
    }
}

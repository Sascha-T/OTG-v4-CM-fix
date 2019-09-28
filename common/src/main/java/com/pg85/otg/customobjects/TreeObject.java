package com.pg85.otg.customobjects;

import com.pg85.otg.LocalBiome;
import com.pg85.otg.LocalWorld;
import com.pg85.otg.OTG;
import com.pg85.otg.configuration.io.SettingsReaderOTGPlus;
import com.pg85.otg.configuration.settingType.Setting;
import com.pg85.otg.configuration.settingType.Settings;
import com.pg85.otg.generator.SpawnableObject;
import com.pg85.otg.util.BoundingBox;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.Rotation;
import com.pg85.otg.util.minecraftTypes.TreeType;

import java.util.Map;
import java.util.Random;

/**
 * A Minecraft tree, viewed as a custom object.
 *
 * <p>For historical reasons, TreeObject implements {@link CustomObject} instead
 * of just {@link SpawnableObject}. We can probably refactor the Tree resource
 * to accept {@link SpawnableObject}s instead of {@link CustomObject}s, so that
 * all the extra methods are no longer needed.
 */
public class TreeObject implements CustomObject
{
    // Non-OTG+
    @Override
    public boolean trySpawnAt(LocalWorld world, Random random, Rotation rotation, int x, int y, int z)
    {
        if (y < minHeight || y > maxHeight)
        {
            return false;
        }
        
        return spawnForced(world, random, rotation, x, y, z);
    }
    
    @Override
    public boolean process(LocalWorld world, Random random, ChunkCoordinate chunkCoord)
    {
        // A tree has no frequency or rarity, so spawn it once in the chunk
        int x = chunkCoord.getBlockXCenter() + random.nextInt(ChunkCoordinate.CHUNK_X_SIZE);
        int z = chunkCoord.getBlockZCenter() + random.nextInt(ChunkCoordinate.CHUNK_Z_SIZE);
                
        int y = world.getHighestBlockYAt(x, z);
        return trySpawnAt(world, random, Rotation.NORTH, x, y, z);
    }
    //
	
    private static class TreeSettings extends Settings
    {
        static final Setting<Integer> MIN_HEIGHT = intSetting("MinHeight",
                OTG.WORLD_DEPTH, OTG.WORLD_DEPTH, OTG.WORLD_HEIGHT);
        static final Setting<Integer> MAX_HEIGHT = intSetting("MaxHeight",
                OTG.WORLD_HEIGHT, OTG.WORLD_DEPTH, OTG.WORLD_HEIGHT);
    }

    private TreeType type;
    private int minHeight = OTG.WORLD_DEPTH;
    private int maxHeight = OTG.WORLD_HEIGHT;

    public TreeObject(TreeType type)
    {
        this.type = type;
    }

    @Override
    public void onEnable(Map<String, CustomObject> otherObjectsInDirectory)
    {
        // Stub method
    }

    public TreeObject(TreeType type, SettingsReaderOTGPlus settings)
    {
        this.type = type;
        this.minHeight = settings.getSetting(TreeSettings.MIN_HEIGHT, TreeSettings.MIN_HEIGHT.getDefaultValue());
        this.maxHeight = settings.getSetting(TreeSettings.MAX_HEIGHT, TreeSettings.MAX_HEIGHT.getDefaultValue());
    }

    @Override
    public String getName()
    {
        return type.name();
    }

    @Override
    public boolean canSpawnAsTree()
    {
        return true;
    }

    @Override
    public boolean canSpawnAsObject()
    {
        return false;
    }

    @Override
    public boolean spawnForced(LocalWorld world, Random random, Rotation rotation, int x, int y, int z)
    {
        return world.placeTree(type, random, x, y, z);
    }

    @Override
    public boolean spawnAsTree(LocalWorld world, Random random, int x, int z)
    {      	
    	throw new RuntimeException(); // Fix this properly, re-do the abstraction/inheritance for BO2/BO3/TreeObject/MCObject/CustomObject
    }

    @Override
    public CustomObject applySettings(SettingsReaderOTGPlus settings)
    {
        return new TreeObject(type, settings);
    }

    @Override
    public boolean hasPreferenceToSpawnIn(LocalBiome biome)
    {
        return true;
    }
    
    @Override
    public boolean canRotateRandomly()
    {
        // Trees cannot be rotated
        return false;
    }
    
    // TODO: Clean up inheritance for CustomObject, these methods shouldn't be here
    
    @Override
    public int getMaxBranchDepth()
    {
    	throw new RuntimeException();
    }    
    
	@Override
	public BoundingBox getBoundingBox(Rotation rotation)
	{
		return BoundingBox.newEmptyBox();
	}
}

package com.pg85.otg.customobjects.bo3;

import com.pg85.otg.LocalWorld;
import com.pg85.otg.configuration.CustomObjectConfigFunction;
import com.pg85.otg.customobjects.CustomObjectCoordinate;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.util.Rotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Represents a block in a BO3.
 */
public class SpawnerFunction extends BO3Function
{
    public int x;
    public int y;
    public int z;

    public Boolean firstSpawn = true;
    public String mobName = "";
    public String nbtFileName = "";
    public String originalnbtFileName = "";
    public int groupSize = 1;
    public int interval = 40;
    public int intervalOffset = 0;
    public int spawnChance = 100;
    public int maxCount = 0;

    public int despawnTime = 0;

    public double velocityX = 0;
    public double velocityY = 0;
    public double velocityZ = 0;

    public float yaw = 0.0F;
    public float pitch = 0.0F;

    public boolean velocityXSet = false;
    public boolean velocityYSet = false;
    public boolean velocityZSet = false;

    @Override
    public void load(List<String> args) throws InvalidConfigException
    {
        assureSize(8, args);
        // Those limits are arbitrary, LocalWorld.setBlock will limit it
        // correctly based on what chunks can be accessed
		x = readInt(args.get(0), -100, 100);
        y = readInt(args.get(1), -1000, 1000);
        z = readInt(args.get(2), -100, 100);
        mobName = args.get(3);

        boolean param4isNBT = false;
        try
        {
        	groupSize = (int) Double.parseDouble(args.get(4));
        }
        catch(NumberFormatException ex)
        {
        	// TODO: Get relative path or saves can't be copied to a different location
        	originalnbtFileName = args.get(4);
        	param4isNBT = true;
        }

        if(originalnbtFileName != null && originalnbtFileName.toLowerCase().trim().endsWith(".txt"))
        {
        	nbtFileName = getHolder().getFile().getParentFile().getAbsolutePath() + File.separator + originalnbtFileName;
        }

        if(param4isNBT)
        {
        	groupSize = readInt(args.get(5), 0, Integer.MAX_VALUE);
        }

        if(!param4isNBT)
        {
        	interval = readInt(args.get(5), 0, Integer.MAX_VALUE);
        } else {
        	interval = readInt(args.get(6), 0, Integer.MAX_VALUE);
        }
        if(!param4isNBT)
        {
        	spawnChance = readInt(args.get(6), 0, Integer.MAX_VALUE);
        } else {
        	spawnChance = readInt(args.get(7), 0, Integer.MAX_VALUE);
        }
        if(!param4isNBT)
        {
        	maxCount = readInt(args.get(7), 0, Integer.MAX_VALUE);
        } else {
        	maxCount = readInt(args.get(8), 0, Integer.MAX_VALUE);
        }

        if(!param4isNBT)
        {
	        if(args.size() > 8)
	        {
	        	despawnTime = readInt(args.get(8), 0, Integer.MAX_VALUE);
	        }
        } else {
	        if(args.size() > 9)
	        {
	        	despawnTime = readInt(args.get(9), 0, Integer.MAX_VALUE);
	        }
        }
        if(!param4isNBT)
        {
	        if(args.size() > 9)
	        {
	        	velocityX = readDouble(args.get(9), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityXSet = true;
	        }
        } else {
	        if(args.size() > 10)
	        {
	        	velocityX = readDouble(args.get(10), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityXSet = true;
	        }
        }
        if(!param4isNBT)
        {
	        if(args.size() > 10)
	        {
	        	velocityY = readDouble(args.get(10), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityYSet = true;
	        }
        } else {
	        if(args.size() > 11)
	        {
	        	velocityY = readDouble(args.get(11), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityYSet = true;
	        }
        }
        if(!param4isNBT)
        {
	        if(args.size() > 11)
	        {
	        	velocityZ = readDouble(args.get(11), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityZSet = true;
	        }
        } else {
	        if(args.size() > 12)
	        {
	        	velocityZ = readDouble(args.get(12), Integer.MIN_VALUE, Integer.MAX_VALUE);
	        	velocityZSet = true;
	        }
        }
        if(!param4isNBT)
        {
	        if(args.size() > 12)
	        {
	        	yaw = (float)readDouble(args.get(12), 0, Integer.MAX_VALUE);
	        }
        } else {
        	if(args.size() > 13)
        	{
        		yaw = (float)readDouble(args.get(13), 0, Integer.MAX_VALUE);
        	}
        }
        if(!param4isNBT)
        {
	        if(args.size() > 13)
	        {
	        	pitch = (float)readDouble(args.get(13), 0, Integer.MAX_VALUE);
	        }
        } else {
	        if(args.size() > 14)
	        {
	        	pitch = (float)readDouble(args.get(14), 0, Integer.MAX_VALUE);
	        }
        }
    }

    private String metaDataTag;
    public String getMetaData()
    {
    	if(nbtFileName != null && nbtFileName.length() > 0 && metaDataTag == null)
    	{
    		File metaDataFile = new File(nbtFileName);
    		StringBuilder stringbuilder = new StringBuilder();
    	    if(metaDataFile.exists())
    	    {
    			try {
    				BufferedReader reader = new BufferedReader(new FileReader(metaDataFile));
    				try {
    					String line = reader.readLine();

    				    while (line != null) {
    				    	stringbuilder.append(line);
    				        line = reader.readLine();
    				    }
    				} finally {
    					reader.close();
    				}
    			} catch (FileNotFoundException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
    			catch (IOException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
    	    }

            metaDataTag = stringbuilder.toString();
    	}
    	return metaDataTag;
    }

    @Override
    public String makeString()
    {
        return "Spawner(" + x + ',' + y + ',' + z + ',' + mobName + (originalnbtFileName != null && originalnbtFileName.length() > 0 ? "," + originalnbtFileName : "") + ',' + groupSize + ',' + interval + ',' + spawnChance + ',' + maxCount + ',' + despawnTime + ',' + velocityX + ',' + velocityY + ',' + velocityZ + ',' + yaw + ',' + pitch + ')';
    }

    @Override
    public SpawnerFunction rotate()
    {
    	SpawnerFunction rotatedBlock = new SpawnerFunction();
        rotatedBlock.x = z;
        rotatedBlock.y = y;
        rotatedBlock.z = -x;
        rotatedBlock.mobName = mobName;

        rotatedBlock.originalnbtFileName = originalnbtFileName;
        rotatedBlock.nbtFileName = nbtFileName;

        rotatedBlock.groupSize = groupSize;
        rotatedBlock.interval = interval;
        rotatedBlock.spawnChance = spawnChance;
        rotatedBlock.maxCount = maxCount;
        rotatedBlock.despawnTime = despawnTime;

        rotatedBlock.velocityX = velocityZ;
        rotatedBlock.velocityY = velocityY;
        rotatedBlock.velocityZ = -velocityX;

        rotatedBlock.velocityXSet = velocityZSet;
        rotatedBlock.velocityYSet = velocityYSet;
        rotatedBlock.velocityZSet = velocityXSet;

        rotatedBlock.yaw = yaw; // TODO: Rotate! +90 or -90?
        rotatedBlock.pitch = pitch;

        return rotatedBlock;
    }

    public SpawnerFunction rotate(Rotation rotation)
    {
    	SpawnerFunction rotatedBlock = new SpawnerFunction();

        CustomObjectCoordinate rotatedCoords = CustomObjectCoordinate.getRotatedBO3CoordsJustified(x, y, z, rotation);

        rotatedBlock.x = rotatedCoords.getX();
        rotatedBlock.y = rotatedCoords.getY();
        rotatedBlock.z = rotatedCoords.getZ();

        rotatedBlock.velocityX = velocityX;
        rotatedBlock.velocityY = velocityY;
        rotatedBlock.velocityZ = velocityZ;

        rotatedBlock.velocityXSet = velocityXSet;
        rotatedBlock.velocityYSet = velocityYSet;
        rotatedBlock.velocityZSet = velocityZSet;

        double newVelocityX = rotatedBlock.velocityX;
        double newVelocityZ = rotatedBlock.velocityZ;

        boolean newVelocityXSet = rotatedBlock.velocityXSet;
        boolean newVelocityZSet = rotatedBlock.velocityZSet;

    	for(int i = 0; i < rotation.getRotationId(); i++)
    	{
            newVelocityX = rotatedBlock.velocityZ;
            newVelocityZ = -rotatedBlock.velocityX;

            rotatedBlock.velocityX = newVelocityX;
            rotatedBlock.velocityY = rotatedBlock.velocityY;
            rotatedBlock.velocityZ = newVelocityZ;

            newVelocityXSet = rotatedBlock.velocityZSet;
            newVelocityZSet = rotatedBlock.velocityXSet;

            rotatedBlock.velocityXSet = newVelocityXSet;
            rotatedBlock.velocityYSet = rotatedBlock.velocityYSet;
            rotatedBlock.velocityZSet = newVelocityZSet;
    	}

        rotatedBlock.mobName = mobName;

        rotatedBlock.originalnbtFileName = originalnbtFileName;
        rotatedBlock.nbtFileName = nbtFileName;

        rotatedBlock.groupSize = groupSize;
        rotatedBlock.interval = interval;
        rotatedBlock.spawnChance = spawnChance;
        rotatedBlock.maxCount = maxCount;
        rotatedBlock.despawnTime = despawnTime;

        rotatedBlock.yaw = yaw; // TODO: Rotate! +90 or -90?
        rotatedBlock.pitch = pitch;

        return rotatedBlock;
    }

    /**
     * Spawns this block at the position. The saved x, y and z in this block are
     * ignored.
     * <p/>
     * @param world  The world to spawn in.
     * @param random The random number generator.
     * @param x      The absolute x to spawn. The x-position in this object is
     *               ignored.
     * @param y      The absolute y to spawn. The y-position in this object is
     *               ignored.
     * @param z      The absolute z to spawn. The z-position in this object is
     *               ignored.
     */
    public void spawn(LocalWorld world, Random random, int x, int y, int z, boolean markBlockForUpdate)
    {
    	throw new RuntimeException();
    }

    @Override
    public boolean isAnalogousTo(CustomObjectConfigFunction<BO3Config> other)
    {
        if(!getClass().equals(other.getClass()))
        {
            return false;
        }
        SpawnerFunction block = (SpawnerFunction) other;
        return block.x == x && block.y == y && block.z == z && block.mobName.equalsIgnoreCase(mobName) && block.originalnbtFileName.equalsIgnoreCase(originalnbtFileName) && block.groupSize == groupSize && block.interval == interval && block.spawnChance == spawnChance && block.maxCount == maxCount && block.despawnTime == despawnTime && block.velocityX == velocityX && block.velocityY == velocityY && block.velocityZ == velocityZ && block.yaw == yaw && block.pitch == pitch;
    }
}

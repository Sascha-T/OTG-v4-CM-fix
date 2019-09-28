package com.pg85.otg.forge.generator;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.pg85.otg.OTG;
import com.pg85.otg.forge.ForgeBiome;
import com.pg85.otg.forge.ForgeWorld;
import com.pg85.otg.generator.biome.BiomeGenerator;
import com.pg85.otg.generator.biome.OutputType;
import com.pg85.otg.util.minecraftTypes.DefaultBiome;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;

/**
 * Minecraft's biome generator class is {@link BiomeProvider}, we use
 * {@link BiomeGenerator}. This class provides a bridge between the two,
 * allowing us to use custom biome generators.
 */
public class OTGBiomeProvider extends BiomeProvider
{   
    private final BiomeGenerator biomeGenerator;
    private final ForgeWorld localWorld;

    public OTGBiomeProvider(ForgeWorld world, BiomeGenerator biomeGenerator)
    {
        this.localWorld = world;
        this.biomeGenerator = biomeGenerator;
    }
    
    @Override
    public Biome getBiome(BlockPos blockPos)
    {
        return OTG.isForge ? ((ForgeBiome)OTG.getBiomeAllWorlds(this.biomeGenerator.getBiome(blockPos.getX(), blockPos.getZ()))).getHandle() : this.localWorld.getBiomeById(this.biomeGenerator.getBiome(blockPos.getX(), blockPos.getZ())).getHandle();
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultOption)
    {
        // For forge make sure all dimensions are queried since the biome we're looking for may be owned by another dimension
        ForgeBiome biome = OTG.isForge ? (ForgeBiome) OTG.getBiomeAllWorlds(this.biomeGenerator.getBiome(pos.getX(), pos.getZ())) : this.localWorld.getBiomeByIdOrNull(this.biomeGenerator.getBiome(pos.getX(), pos.getZ()));
        if (biome != null)
        {
            return biome.getHandle();
        }
        return defaultOption;
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] paramArrayOfBiomeBase, int x, int z, int width, int height)
    {
        if (paramArrayOfBiomeBase == null || (paramArrayOfBiomeBase.length < width * height))
        {
            paramArrayOfBiomeBase = new Biome[width * height];
        }

        int[] arrayOfInt = this.biomeGenerator.getBiomesUnZoomed(null, x, z, width, height,
                OutputType.DEFAULT_FOR_WORLD);

        // Replaces ids with BiomeBases
        for (int i = 0; i < width * height; i++)
        {
            paramArrayOfBiomeBase[i] = OTG.isForge ? ((ForgeBiome)OTG.getBiomeAllWorlds(arrayOfInt[i])).getHandle() : this.localWorld.getBiomeById(arrayOfInt[i]).getHandle();
        }

        return paramArrayOfBiomeBase;
    }

    @Override
    public Biome[] getBiomes(Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag)
    {
        if ((listToReuse == null) || (listToReuse.length < width * length))
        {
            listToReuse = new Biome[width * length];
        }

        int[] arrayOfInt = this.biomeGenerator.getBiomes(null, x, z, width, length, OutputType.DEFAULT_FOR_WORLD);

        // Replace ids with BiomeBases
        for (int i = 0; i < width * length; i++)
        {
            listToReuse[i] = OTG.isForge ? ((ForgeBiome)OTG.getBiomeAllWorlds(arrayOfInt[i])).getHandle() : this.localWorld.getBiomeById(arrayOfInt[i]).getHandle();
        }

        return listToReuse;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed)
    {
        // Hack for villages in other biomes
        // (The alternative would be to completely override the village spawn
        // code)
        if (allowed == MapGenVillage.VILLAGE_SPAWN_BIOMES && this.localWorld.villageGen != null)
        {
            allowed = this.localWorld.villageGen.villageSpawnBiomes;
        }

        int i = x - radius >> 2;
        int j = z - radius >> 2;
        int k = x + radius >> 2;
        int m = z + radius >> 2;

        int n = k - i + 1;
        int i1 = m - j + 1;
        Biome[] arrayOfInt = this.getBiomesForGeneration(null, i, j, n, i1);
        for (int i2 = 0; i2 < n * i1; i2++)
        {
            Biome localBiomeBase = arrayOfInt[i2];
            if (!allowed.contains(localBiomeBase))
                return false;
        }

        return true;
    }

    private static List<Biome> forbiddenBiomes = Lists.newArrayList(Biomes.HELL, Biomes.SKY, Biomes.VOID);
    
    @Override
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random)
    {       	
        int i = x - range >> 2;
        int j = z - range >> 2;
        int k = x + range >> 2;
        int m = z + range >> 2;

        int n = k - i + 1;
        int i1 = m - j + 1;
        int[] arrayOfInt = this.biomeGenerator.getBiomesUnZoomed(null, i, j, n, i1, OutputType.DEFAULT_FOR_WORLD);
        BlockPos blockPos = null;
        int i2 = 0;       
        
        for (int i3 = 0; i3 < arrayOfInt.length; i3++)
        {
            if (arrayOfInt[i3] >= DefaultBiome.values().length)
            {
                continue;
            }
            
            int i4 = i + i3 % n << 2;
            int i5 = j + i3 / n << 2;
            Biome localBiomeBase = Biome.getBiome(arrayOfInt[i3]);            
            if (
        		!forbiddenBiomes.contains(localBiomeBase) && 
        		!(blockPos != null && random.nextInt(i2 + 1) != 0))
            {
                blockPos = new BlockPos(i4, 0, i5);
                i2++;
            }
        }

        return blockPos;
    }

    @Override
    public void cleanupCache()
    {
        this.biomeGenerator.cleanupCache();
    }
}
package com.pg85.otg.configuration;

import com.pg85.otg.BiomeIds;
import com.pg85.otg.LocalBiome;
import com.pg85.otg.LocalWorld;
import com.pg85.otg.OTG;
import com.pg85.otg.configuration.BiomeConfigFinder.BiomeConfigStub;
import com.pg85.otg.configuration.io.FileSettingsReader;
import com.pg85.otg.configuration.io.FileSettingsWriter;
import com.pg85.otg.configuration.io.SettingsMap;
import com.pg85.otg.configuration.standard.BiomeStandardValues;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.configuration.standard.StandardBiomeTemplate;
import com.pg85.otg.configuration.standard.WorldStandardValues;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.util.helpers.FileHelper;
import com.pg85.otg.util.minecraftTypes.DefaultBiome;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

/**
 * Holds the WorldConfig and all BiomeConfigs.
 *
 * <h3>A note about {@link LocalWorld} usage</h3>
 * <p>Currently, a {@link LocalWorld} instance is passed to the constructor of
 * this class. That is bad design. The plugin should be able to read the
 * settings and then create a world based on that. Now the world is created, and
 * then the settings are injected. It is also strange that the configuration
 * code is now able to spawn a cow, to give one example.</p>
 *
 * <p>Fixing that will be a lot of work - {@link LocalWorld} is currently a God
 * class that is required everywhere. If a rewrite of that class is ever
 * planned, be sure to split that class up!</p>
 */
public final class ServerConfigProvider implements ConfigProvider
{
    private static final int MAX_INHERITANCE_DEPTH = 15;
    private LocalWorld world;
    private File settingsDir;
    private WorldConfig worldConfig;

    /**
     * Holds all biome configs. Generation Id => BiomeConfig
     * <p>
     * Must be simple array for fast access. Warning: some ids may contain
     * null values, always check.
     */
    private LocalBiome[] biomes;

    /**
     * The number of loaded biomes.
     */
    private int biomesCount;

    /**
     * Loads the settings from the given directory for the given world.
     * @param settingsDir The directory to load from.
     * @param world       The world to load the settings for.
     */
    public ServerConfigProvider(File settingsDir, LocalWorld world)
    {
        this.settingsDir = settingsDir;
        this.world = world;
        this.biomes = new LocalBiome[world.getMaxBiomesCount()];

        loadSettings();
    }

    /**
     * Loads all settings. Expects the biomes array to be empty (filled with
     * nulls), the savedBiomes collection to be empty and the biomesCount
     * field to be zero.
     */
    private void loadSettings()
    {
        SettingsMap worldConfigSettings = loadWorldConfig();
        loadBiomes(worldConfigSettings);

        // We have to wait for the loading in order to get things like
        // temperature
        worldConfig.biomeGroupManager.processBiomeData(world);
    }

    private SettingsMap loadWorldConfig()
    {
        File worldConfigFile = new File(settingsDir, WorldStandardValues.WORLD_CONFIG_FILE_NAME);
        SettingsMap settingsMap = FileSettingsReader.read(world.getName(), worldConfigFile);
        this.worldConfig = new WorldConfig(settingsDir, settingsMap, world);
        FileSettingsWriter.writeToFile(worldConfig.getSettingsAsMap(), worldConfigFile, worldConfig.SettingsMode);

        return settingsMap;
    }

    public void saveWorldConfig()
    {
    	File worldConfigFile = new File(settingsDir, WorldStandardValues.WORLD_CONFIG_FILE_NAME);
    	FileSettingsWriter.writeToFile(worldConfig.getSettingsAsMap(), worldConfigFile, worldConfig.SettingsMode);
    }

    private void loadBiomes(SettingsMap worldConfigSettings)
    {
        // Establish folders
        List<File> biomeDirs = new ArrayList<File>(2);
        // OpenTerrainGenerator/worlds/<WorldName>/<WorldBiomes/
        biomeDirs.add(new File(settingsDir, correctOldBiomeConfigFolder(settingsDir)));
        // OpenTerrainGenerator/GlobalBiomes/
        biomeDirs.add(new File(OTG.getEngine().getTCDataFolder(), PluginStandardValues.BiomeConfigDirectoryName));

        FileHelper.makeFolders(biomeDirs);

        // Build a set of all biomes to load
        Collection<BiomeLoadInstruction> biomesToLoad = new HashSet<BiomeLoadInstruction>();
        Collection<? extends BiomeLoadInstruction> defaultBiomes = world.getDefaultBiomes();
        biomesToLoad.addAll(defaultBiomes);

        // This adds all custombiomes that have been listed in WorldConfig to
        // the arrayList
        for (Entry<String, Integer> entry : worldConfig.customBiomeGenerationIds.entrySet())
        {
            String biomeName = entry.getKey();
            int generationId = entry.getValue();
            biomesToLoad.add(new BiomeLoadInstruction(biomeName, generationId, new StandardBiomeTemplate(worldConfig.worldHeightScale)));
        }

        // Load all files
        BiomeConfigFinder biomeConfigFinder = new BiomeConfigFinder(worldConfig, OTG.getPluginConfig().biomeConfigExtension);
        Map<String, BiomeConfigStub> biomeConfigStubs = biomeConfigFinder.findBiomes(biomeDirs, biomesToLoad);

        // Read all settings
        Map<String, BiomeConfig> loadedBiomes = readAndWriteSettings(worldConfigSettings, biomeConfigStubs);

        // Index all necessary settings
        String loadedBiomeNames = indexSettings(loadedBiomes);

        OTG.log(LogMarker.INFO, "{} biomes Loaded", biomesCount);
        OTG.log(LogMarker.TRACE, "{}", loadedBiomeNames);
    }

    @Override
    public WorldConfig getWorldConfig()
    {
        return worldConfig;
    }

    @Override
    public LocalBiome getBiomeByIdOrNull(int id)
    {
        if (id < 0 || id > biomes.length)
        {
            return null;
        }
        return biomes[id];
    }

    @Override
    public void reload()
    {
        // Clear biome collections
        Arrays.fill(this.biomes, null);
        this.biomesCount = 0;

        // Load again
        loadSettings();
    }

    private Map<String, BiomeConfig> readAndWriteSettings(SettingsMap worldConfigSettings, Map<String, BiomeConfigStub> biomeConfigStubs)
    {
        Map<String, BiomeConfig> loadedBiomes = new HashMap<String, BiomeConfig>();

        for (BiomeConfigStub biomeConfigStub : biomeConfigStubs.values())
        {
            // Allow to let world settings influence biome settings
            //biomeConfigStub.getSettings().setFallback(worldConfigSettings); // TODO: Make sure this can be removed safely

            // Inheritance
            processInheritance(biomeConfigStubs, biomeConfigStub, 0);
            processMobInheritance(biomeConfigStubs, biomeConfigStub, 0);

            // Settings reading
            BiomeConfig biomeConfig = new BiomeConfig(biomeConfigStub.getLoadInstructions(), biomeConfigStub, biomeConfigStub.getSettings(), worldConfig);
            loadedBiomes.put(biomeConfigStub.getBiomeName(), biomeConfig);

            // Settings writing
            File writeFile = biomeConfigStub.getFile();
            if (!biomeConfig.biomeExtends.isEmpty())
            {
                writeFile = new File(writeFile.getAbsolutePath() + ".inherited");
            }
            FileSettingsWriter.writeToFile(biomeConfig.getSettingsAsMap(), writeFile, worldConfig.SettingsMode);
        }

        return loadedBiomes;
    }

    /**
     * Gets the generation id that the given biome should have, based on
     * {@link DefaultBiome the default biomes} and
     * {@link WorldConfig#customBiomeGenerationIds the CustomBiomes setting}.
     * @param biomeConfig The biome.
     * @return The preferred generation id.
     */
    private int getRequestedGenerationId(BiomeConfig biomeConfig)
    {
        Integer requestedGenerationId = DefaultBiome.getId(biomeConfig.getName());
        if (requestedGenerationId == null)
        {
            requestedGenerationId = biomeConfig.worldConfig.customBiomeGenerationIds.get(biomeConfig.getName());
        }
        if (requestedGenerationId == null)
        {
            throw new RuntimeException(biomeConfig.getName() + " is not a default biome and not a custom biome. This is a bug!");
        }
        return requestedGenerationId;
    }

    private String indexSettings(Map<String, BiomeConfig> loadedBiomes)
    {
        StringBuilder loadedBiomeNames = new StringBuilder();

        List<BiomeConfig> loadedBiomeList = new ArrayList<BiomeConfig>(loadedBiomes.values());
        Collections.sort(loadedBiomeList, new Comparator<BiomeConfig>() {
            @Override
            public int compare(BiomeConfig a, BiomeConfig b) {
                return getRequestedGenerationId(a) - getRequestedGenerationId(b);
            }
        });

        // Now that all settings are loaded, we can index them,
        // cross-reference between biomes, etc.
        for (BiomeConfig biomeConfig : loadedBiomeList)
        {
            // Statistics of the loaded biomes
            this.biomesCount++;
            loadedBiomeNames.append(biomeConfig.getName());
            loadedBiomeNames.append(", ");

            int requestedGenerationId = getRequestedGenerationId(biomeConfig);

            // Get correct saved id (defaults to generation id, but can be set
            // to use the generation id of another biome)
            int requestedSavedId = requestedGenerationId;
            if (!biomeConfig.replaceToBiomeName.isEmpty())
            {
                BiomeConfig replaceToConfig = loadedBiomes.get(biomeConfig.replaceToBiomeName);
                if (replaceToConfig == null)
                {
                    OTG.log(LogMarker.WARN, "Invalid ReplaceToBiomeName in biome {}: biome {} doesn't exist", biomeConfig.getName(),
                            biomeConfig.replaceToBiomeName);
                    biomeConfig.replaceToBiomeName = "";
                } else if (!replaceToConfig.replaceToBiomeName.isEmpty())
                {
                    OTG.log(LogMarker.WARN, "Invalid ReplaceToBiomeName in biome {}: biome {} also has a ReplaceToBiomeName value",
                            biomeConfig.getName(), biomeConfig.replaceToBiomeName);
                    biomeConfig.replaceToBiomeName = "";
                } else
                {
                    requestedSavedId = getRequestedGenerationId(replaceToConfig);
                }
            }

            // Create biome
            LocalBiome biome = world.createBiomeFor(biomeConfig, new BiomeIds(requestedGenerationId, requestedSavedId), this);

            int generationId = biome.getIds().getGenerationId();

            this.biomes[generationId] = biome;

            // Update WorldConfig with actual id
            worldConfig.customBiomeGenerationIds.put(biome.getName(), generationId);

            // Indexing ReplacedBlocks
            if (!this.worldConfig.BiomeConfigsHaveReplacement)
            {
                this.worldConfig.BiomeConfigsHaveReplacement = biomeConfig.replacedBlocks.hasReplaceSettings();
            }

            // Indexing MaxSmoothRadius
            if (this.worldConfig.maxSmoothRadius < biomeConfig.smoothRadius)
            {
                this.worldConfig.maxSmoothRadius = biomeConfig.smoothRadius;
            }

            // Indexing BiomeColor
            if (this.worldConfig.biomeMode == OTG.getBiomeModeManager().FROM_IMAGE)
            {
                if (this.worldConfig.biomeColorMap == null)
                {
                    this.worldConfig.biomeColorMap = new HashMap<Integer, Integer>();
                }

                int color = biomeConfig.biomeColor;
                this.worldConfig.biomeColorMap.put(color, biome.getIds().getGenerationId());
            }
        }

        // Forge dimensions are seperate worlds that can share biome configs so
        // use the highest maxSmoothRadius of any of the loaded worlds.
        // Worlds loaded before this one will not use biomes from this world
        // so no need to change their this.worldConfig.maxSmoothRadius
        ArrayList<LocalWorld> worlds = OTG.getAllWorlds();
        if(worlds != null)
        {
	        for(LocalWorld world : worlds)
	        {
	            if (this.worldConfig.maxSmoothRadius < world.getConfigs().getWorldConfig().maxSmoothRadius)
	            {
	                this.worldConfig.maxSmoothRadius = world.getConfigs().getWorldConfig().maxSmoothRadius;
	            }
	        }
        }

        if (this.biomesCount > 0)
        {
            // Remove last ", "
            loadedBiomeNames.delete(loadedBiomeNames.length() - 2, loadedBiomeNames.length());
        }
        return loadedBiomeNames.toString();
    }

    private void processInheritance(Map<String, BiomeConfigStub> biomeConfigStubs, BiomeConfigStub biomeConfigStub, int currentDepth)
    {
        if (biomeConfigStub.biomeExtendsProcessed)
        {
            // Already processed earlier
            return;
        }

        String extendedBiomeName = biomeConfigStub.getSettings().getSetting(BiomeStandardValues.BIOME_EXTENDS);
        if (extendedBiomeName.isEmpty())
        {
            // Not extending anything
            biomeConfigStub.biomeExtendsProcessed = true;
            return;
        }

        // This biome extends another biome
        BiomeConfigStub extendedBiomeConfig = biomeConfigStubs.get(extendedBiomeName);
        if (extendedBiomeConfig == null)
        {
            OTG.log(LogMarker.WARN, "The biome {} tried to extend the biome {}, but that biome doesn't exist.",
                    biomeConfigStub.getBiomeName(), extendedBiomeName);
            return;
        }

        // Check for too much recursion
        if (currentDepth > MAX_INHERITANCE_DEPTH)
        {
            OTG.log(LogMarker.FATAL,
                    "The biome {} cannot extend the biome {} - too much configs processed already! Cyclical inheritance?",
                    biomeConfigStub.getBiomeName(), extendedBiomeConfig.getBiomeName());
        }

        if (!extendedBiomeConfig.biomeExtendsProcessed)
        {
            // This biome has not been processed yet, do that first
            processInheritance(biomeConfigStubs, extendedBiomeConfig, currentDepth + 1);
        }

        // Merge the two
        biomeConfigStub.getSettings().setFallback(extendedBiomeConfig.getSettings());

        // Done
        biomeConfigStub.biomeExtendsProcessed = true;
    }

    private void processMobInheritance(Map<String, BiomeConfigStub> biomeConfigStubs, BiomeConfigStub biomeConfigStub, int currentDepth)
    {
        if (biomeConfigStub.inheritMobsBiomeNameProcessed)
        {
            // Already processed earlier
            return;
        }

        String stubInheritMobsBiomeName = biomeConfigStub.getSettings().getSetting(BiomeStandardValues.INHERIT_MOBS_BIOME_NAME, biomeConfigStub.getLoadInstructions().getBiomeTemplate().defaultInheritMobsBiomeName);

        if(stubInheritMobsBiomeName != null && stubInheritMobsBiomeName.length() > 0)
        {
            String[] inheritMobsBiomeNames = stubInheritMobsBiomeName.split(",");
	        for(String inheritMobsBiomeName : inheritMobsBiomeNames)
	        {
	            if (inheritMobsBiomeName.isEmpty())
	            {
	                // Not extending anything
	                biomeConfigStub.inheritMobsBiomeNameProcessed = true;
	                return;
	            }

		        // This biome inherits mobs from another biome
		        BiomeConfigStub inheritMobsBiomeConfig = biomeConfigStubs.get(inheritMobsBiomeName);
		        if (inheritMobsBiomeConfig == null)
		        {
		            OTG.log(LogMarker.WARN, "The biome {} tried to inherit mobs from the biome {}, but that biome doesn't exist.", new Object[] { biomeConfigStub.getFile().getName(), inheritMobsBiomeName});
		            continue;
		        }

		        // Check for too much recursion
		        if (currentDepth > MAX_INHERITANCE_DEPTH)
		        {
		            OTG.log(LogMarker.FATAL, "The biome {} cannot inherit mobs from biome {} - too much configs processed already! Cyclical inheritance?", new Object[] { biomeConfigStub.getFile().getName(), inheritMobsBiomeConfig.getFile().getName()});
		        }

		        // BiomeConfigStubs is unique per world so if there is a duplicate biome name it must be a TC biome with the same name as a vanilla biome
		        if(inheritMobsBiomeConfig == biomeConfigStub)
		        {
		        	// Get the mobs that spawn in this vanilla biome (this will also inherit any mobs added to vanilla biomes by mods when MC started).
		        	world.mergeVanillaBiomeMobSpawnSettings(biomeConfigStub);

			        continue;
		        }

		        if (!inheritMobsBiomeConfig.inheritMobsBiomeNameProcessed)
		        {
		            // This biome has not been processed yet, do that first
		            processMobInheritance(biomeConfigStubs, inheritMobsBiomeConfig, currentDepth + 1);
		        }

		        // Merge the two
		        biomeConfigStub.mergeMobs(inheritMobsBiomeConfig);
	        }

	        // Done
	        biomeConfigStub.inheritMobsBiomeNameProcessed = true;
        }
    }

    private String correctOldBiomeConfigFolder(File settingsDir)
    {
        // Rename the old folder
        String biomeFolderName = WorldStandardValues.WORLD_BIOMES_DIRECTORY_NAME;
        File oldBiomeConfigs = new File(settingsDir, "BiomeConfigs");
        if (oldBiomeConfigs.exists())
        {
            if (!oldBiomeConfigs.renameTo(new File(settingsDir, biomeFolderName)))
            {
                OTG.log(LogMarker.WARN, "========================");
                OTG.log(LogMarker.WARN, "Fould old `BiomeConfigs` folder, but it could not be renamed to `", biomeFolderName, "`!");
                OTG.log(LogMarker.WARN, "Please rename the folder manually.");
                OTG.log(LogMarker.WARN, "========================");
                biomeFolderName = "BiomeConfigs";
            }
        }
        return biomeFolderName;
    }

    @Override
    public LocalBiome[] getBiomeArray()
    {
        return this.biomes;
    }
}

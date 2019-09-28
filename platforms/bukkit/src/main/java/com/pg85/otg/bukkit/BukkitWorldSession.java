package com.pg85.otg.bukkit;

import java.util.ArrayList;

import com.pg85.otg.LocalWorld;
import com.pg85.otg.WorldSession;
import com.pg85.otg.customobjects.bo3.ParticleFunction;
import com.pg85.otg.util.ChunkCoordinate;

public class BukkitWorldSession extends WorldSession
{
	ArrayList<ParticleFunction> ParticleFunctions = new ArrayList<ParticleFunction>();

	public BukkitWorldSession(LocalWorld world)
	{
		super(world);
	}

	@Override
	public ArrayList<ParticleFunction> getParticleFunctions()
	{
		return ParticleFunctions;
	}

	@Override
	public int getWorldBorderRadius()
	{
		return 0;
	}

	@Override
	public ChunkCoordinate getWorldBorderCenterPoint()
	{
		return ChunkCoordinate.fromBlockCoords(0, 0);
	}

	@Override
	public int getPregenerationRadius()
	{
		return 0;
	}

	@Override
	public int setPregenerationRadius(int value)
	{
		return 0;
	}

	@Override
	public int getPregeneratedBorderLeft()
	{
		return 0;
	}

	@Override
	public int getPregeneratedBorderRight()
	{
		return 0;
	}

	@Override
	public int getPregeneratedBorderTop()
	{
		return 0;
	}

	@Override
	public int getPregeneratedBorderBottom()
	{
		return 0;
	}

	@Override
	public ChunkCoordinate getPreGeneratorCenterPoint()
	{
		return ChunkCoordinate.fromBlockCoords(0, 0);
	}

	@Override
	public boolean getPreGeneratorIsRunning()
	{
		return false;
	}
}

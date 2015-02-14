package com.koletar.jj.mineresetlite;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.patterns.Pattern;

import java.util.List;
import java.util.Random;

public class MinePattern implements Pattern {
	Mine m;
	Random rand = new Random();
	List<Mine.CompositionEntry> probabilityMap;

	public MinePattern(Mine m) {
		this.m = m;
		probabilityMap = m.mapComposition(m.composition);
	}

	@Override
	public BaseBlock next(Vector vector) {
		return next(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
	}

	@Override
	public BaseBlock next(int x, int y, int z) {
		if (y == m.maxY && m.surface != null) {
			return new BaseBlock(m.surface.getBlockId(), m.surface.getData());
		}
		double r = rand.nextDouble();
		for (Mine.CompositionEntry ce : probabilityMap) {
			if (r <= ce.getChance()) {
				return new BaseBlock(ce.getBlock().getBlockId(), ce.getBlock().getData());
			}
		}
		return new BaseBlock(0);
	}
}

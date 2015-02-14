package com.koletar.jj.mineresetlite;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.masks.Mask;

public class MineMask implements Mask {
	private final Mine mine;

	public MineMask(Mine mine) {
		this.mine = mine;
	}

	@Override
	public void prepare(LocalSession localSession, LocalPlayer localPlayer, Vector vector) {

	}

	@Override
	public boolean matches(EditSession editSession, Vector vector) {
		return (vector.getX() >= mine.minX && vector.getX() <= mine.maxX)
				&& (vector.getY() >= mine.minY && vector.getY() <= mine.maxY)
				&& (vector.getZ() >= mine.minZ && vector.getZ() <= mine.maxZ);
	}
}

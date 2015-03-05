/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koletar.jj.mineresetlite.mine;

import com.koletar.jj.mineresetlite.MinePattern;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.primesoft.asyncworldedit.blockPlacer.BlockPlacer;
import org.primesoft.asyncworldedit.utils.FuncParamEx;
import org.primesoft.asyncworldedit.worldedit.CancelabeEditSession;
import us.myles.Sengine.Sengine;

/**
 *
 * @author prime
 */
public class MineResetManual implements FuncParamEx<Integer, CancelabeEditSession, MaxChangedBlocksException> {

    private final BlockPlacer m_blockPlacer;
    private final String m_name;
    private final IMine m_mine;
    private final Region m_region;
    private final MinePattern m_pattern;

    public MineResetManual(BlockPlacer bp, String name, 
            IMine mine, World world, MinePattern pattern,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        m_pattern = pattern;
        m_name = name;
        m_blockPlacer = bp;
        m_mine = mine;
       
        
        m_region = new CuboidRegion(new BukkitWorld(world), 
                new Vector(minX, minY, minZ), 
                new Vector(maxX, maxY, maxZ));
    }

    public Integer execute(CancelabeEditSession cancelabeEditSession) throws MaxChangedBlocksException {
        int jobID = -1;
        /* Brum. */
        try {            
            int i = cancelabeEditSession.setBlocks(m_region, m_pattern);

            m_mine.setTotal(i);
        } catch (Exception e) {
            Sengine.dump(e, "Mine: " + m_name, "ID: " + jobID);
        }

        return 0;
    }

}

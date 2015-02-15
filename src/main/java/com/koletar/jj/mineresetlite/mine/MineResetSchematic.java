/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koletar.jj.mineresetlite.mine;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import java.io.File;
import java.io.IOException;
import org.primesoft.asyncworldedit.blockPlacer.BlockPlacer;
import org.primesoft.asyncworldedit.blockPlacer.BlockPlacerEntry;
import org.primesoft.asyncworldedit.blockPlacer.entries.JobEntry;
import org.primesoft.asyncworldedit.playerManager.PlayerEntry;
import org.primesoft.asyncworldedit.utils.FuncParamEx;
import org.primesoft.asyncworldedit.worldedit.CancelabeEditSession;

/**
 *
 * @author prime
 */
public class MineResetSchematic implements FuncParamEx<Integer, CancelabeEditSession, MaxChangedBlocksException> {

    private final Vector m_origin;

    private final File m_schematic;
    
    private final BlockPlacer m_blockPlacer;
    
    private final String m_jobName;
    
    private final IMine m_mine;

    public MineResetSchematic(BlockPlacer bp, String name,
            File f, IMine mine,
            String origin, int minX, int maxY, int minZ) {
        m_jobName = name;
        m_blockPlacer = bp;
        m_schematic = f;
        m_mine = mine;

        Vector v = null;
        if (origin != null) {
            String parts[] = origin.split(",");
            
            if (parts != null && parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    
                    v = new Vector(x, y, z);
                } catch (NumberFormatException ex) {
                    
                }
            }
        }

        if (v == null) {
            m_origin = new Vector(minX, maxY, minZ);
        } else {
            m_origin = v;
        }
    }

    public Integer execute(CancelabeEditSession cancelabeEditSession) throws MaxChangedBlocksException {
        int jobID = 0;
        try {
            CuboidClipboard cc = CuboidClipboard.loadSchematic(m_schematic);
            cc.paste(cancelabeEditSession, m_origin, false);
            
            //This is gona crash
            for (JobEntry e : m_blockPlacer.getPlayerEvents(PlayerEntry.UNKNOWN).getJobs()) {
                if (e.getName().equalsIgnoreCase(m_jobName)) {
                    jobID = e.getJobId();
                }
            }
            int b = 0;
            for (BlockPlacerEntry e : m_blockPlacer.getPlayerEvents(PlayerEntry.UNKNOWN).getQueue()) {
                if (e.getJobId() == jobID) {
                    b++;
                }
            }
            
            m_mine.setJobID(jobID);
            m_mine.setTotal(b);
        } catch (IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
        } catch (MaxChangedBlocksException e) {
            System.out.println("Max Block Change Exception");
            e.printStackTrace();
        } catch (com.sk89q.worldedit.world.DataException e) {
            e.printStackTrace();
        }
        return 0;
    }

}

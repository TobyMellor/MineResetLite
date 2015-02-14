/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koletar.jj.mineresetlite.mine;

import com.koletar.jj.mineresetlite.MineResetLite;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.primesoft.asyncworldedit.blockPlacer.IJobEntryListener;
import org.primesoft.asyncworldedit.blockPlacer.entries.JobEntry;

/**
 *
 * @author SBPrime
 */
public class MineJobListener implements IJobEntryListener {

    private final String m_name;

    private final IMine m_mine;

    private final Location m_teleport;

    private final World m_world;

    private final int m_maxX;

    private final int m_maxY;

    private final int m_maxZ;
    
    
    private final int m_minX;

    private final int m_minY;

    private final int m_minZ;

    public MineJobListener(String name, String teleport,
            World world, 
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            IMine mine) {
        m_name = name;
        m_mine = mine;
        m_world = world;
        m_minX = minX;
        m_minY = minY;
        m_minZ = minZ;
        m_maxX = maxX;
        m_maxY = maxY;
        m_maxZ = maxZ;

        Location tp = null;
        String[] tpParts = null;
        if (teleport != null) {
            tpParts = teleport.split(",");
        }

        if (tpParts != null && tpParts.length >= 5) {
            try {
                final int x = Integer.parseInt(tpParts[0]);
                final int y = Integer.parseInt(tpParts[1]);
                final int z = Integer.parseInt(tpParts[2]);

                final float yaw = Float.parseFloat(tpParts[3]);
                final float pit = Float.parseFloat(tpParts[4]);

                tp = new Location(world, x, y, z, yaw, pit);
            } catch (NumberFormatException ex) {
                tp = null;
            }
        }

        m_teleport = tp;
    }

    @Override
    public void jobStateChanged(JobEntry job) {
//        jobID = job.getJobId();
        if (job.getStatus() == JobEntry.JobStatus.PlacingBlocks) {
            System.out.println("Placing blocks for, " + m_name + ".");
            m_mine.setPending(false);
            m_mine.setDone(false);
            //Pull players out
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                Location l = p.getLocation();
                if (m_mine.isInside(p) && m_teleport != null) {
                    Location tpLocation = m_teleport != null ? m_teleport : new Location(m_world, l.getX(), m_maxY + 2D, l.getZ());
                    p.teleport(tpLocation);
                }
            }
        }
        
        //This is gona fail!
        if (job.getStatus() == JobEntry.JobStatus.Done) {
            int air = 0;
            
            for (int x = m_minX; x <= m_maxX; x++) {
                for (int y = m_minY; y <= m_maxY; y++) {
                    for (int z = m_minZ; z <= m_maxZ; z++) {
                        if (m_world.getBlockAt(x, y, z).getTypeId() == 0) {
                            air++;
                        }
                    }
                }
            }
            
            m_mine.setAirCount(air);
            m_mine.setDone(true);
            m_mine.setPending(false);
            MineResetLite.instance.resetting = false;
            System.out.println("Finished mine, " + m_name + ".");
        }
    }
}

package com.koletar.jj.mineresetlite.mine;

import org.primesoft.asyncworldedit.blockPlacer.BlockPlacer;
import org.primesoft.asyncworldedit.blockPlacer.IBlockPlacerListener;
import org.primesoft.asyncworldedit.blockPlacer.IJobEntryListener;
import org.primesoft.asyncworldedit.blockPlacer.entries.JobEntry;
import org.primesoft.asyncworldedit.playerManager.PlayerEntry;

/**
 *
 * @author SBPrime
 */
public class MineBlockPlacerListener implements IBlockPlacerListener, Runnable {

    private final String m_jobName;

    private final IJobEntryListener m_stateListener;

    private final BlockPlacer m_blockPlacer;
    
    private final IMine m_mine;

    public MineBlockPlacerListener(IMine mine, String jobName, IJobEntryListener stateListener,
            BlockPlacer blockPlacer) {
        m_mine = mine;
        m_jobName = jobName;
        m_stateListener = stateListener;
        m_blockPlacer = blockPlacer;
    }

    public void jobAdded(JobEntry job) {
        //&& job.getPlayer().equals(PlayerEntry.UNKNOWN)
        if (job.getName().equals(m_jobName)) {
            job.addStateChangedListener(m_stateListener);
            

            m_mine.setJobID(job.getJobId());
            new Thread(this).start();
        }
    }

    @Override
    public void jobRemoved(JobEntry job) {
    }

    @Override
    public void run() {
        m_blockPlacer.removeListener(this);
    }
}

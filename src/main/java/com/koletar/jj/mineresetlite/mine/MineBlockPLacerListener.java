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
public class MineBlockPLacerListener implements IBlockPlacerListener, Runnable {

    private final String m_jobName;

    private final IJobEntryListener m_stateListener;

    private final BlockPlacer m_blockPlacer;

    public MineBlockPLacerListener(String jobName, IJobEntryListener stateListener,
            BlockPlacer blockPlacer) {
        m_jobName = jobName;
        m_stateListener = stateListener;
        m_blockPlacer = blockPlacer;
    }

    public void jobAdded(JobEntry job) {
        if (job.getName().equals(m_jobName)
                && job.getPlayer().equals(PlayerEntry.UNKNOWN)) {
            job.addStateChangedListener(m_stateListener);

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

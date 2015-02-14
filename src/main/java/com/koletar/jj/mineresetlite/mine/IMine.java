/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koletar.jj.mineresetlite.mine;

import org.bukkit.entity.Player;

/**
 *
 * @author SBPrime
 */
public interface IMine {

    public void setPending(boolean pending);

    public void setDone(boolean done);

    public boolean isInside(Player p);

    public void setAirCount(int air);
    
}

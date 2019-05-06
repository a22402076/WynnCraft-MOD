/*
 *  * Copyright © Wynntils - 2019.
 */

package com.wynntils.modules.richpresence.profiles;

import com.wynntils.core.events.custom.WynnTerritoryChangeEvent;
import net.minecraftforge.common.MinecraftForge;

public class DataProfile {

    String location = "Waiting";
    boolean unknownLocation = false;

    public DataProfile(){
    }

    public String getLocation() {
        return location;
    }

    public boolean getUnknownLocation() {
        return unknownLocation;
    }

    public void setLocation(String value) {
        MinecraftForge.EVENT_BUS.post(new WynnTerritoryChangeEvent(location, value));
        location = value;
    }

    public void setUnknownLocation(boolean value) {
        unknownLocation = value;
    }

}

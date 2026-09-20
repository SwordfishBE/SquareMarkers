package net.squaremarkers.fabric.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface NetherPortalInterface {

    void squareMarkers$createMarker(Level world);

    BlockPos squareMarkers$getPortalCenter();

}

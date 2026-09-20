package net.squaremarkers.core.helpers;

import xyz.jpenilla.squaremap.api.MapWorld;

public class WorldHelpers {

	public static boolean isOverworld(MapWorld world) {
		return isOverworld(world.identifier().asString());
	}

    public static boolean isOverworld(String worldKey) {
		return !isNether(worldKey) && !isEnd(worldKey);
    }

	public static boolean isNether(MapWorld world) {
		return isNether(world.identifier().asString());
	}

    public static boolean isNether(String worldKey) {
		return worldKey.equals("minecraft:the_nether") || worldKey.endsWith(":the_nether") || worldKey.endsWith("_nether");
    }

	public static boolean isEnd(MapWorld world) {
		return isEnd(world.identifier().asString());
	}

    public static boolean isEnd(String worldKey) {
		return worldKey.equals("minecraft:the_end") || worldKey.endsWith(":the_end") || worldKey.endsWith("_the_end");
    }

}

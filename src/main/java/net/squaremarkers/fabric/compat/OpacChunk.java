package net.squaremarkers.fabric.compat;

import net.minecraft.world.level.ChunkPos;
import net.squaremarkers.core.interfaces.entities.IPoint;

import java.util.List;
import java.util.UUID;

public record OpacChunk(ChunkPos pos, String playerName, String name, int color,
                        UUID ownerId, String subId, int subIndex) {

    /** Legacy constructor for geometry-only clients without an OPAC claim state. */
    public OpacChunk(ChunkPos pos, String playerName, String name, int color) {
        this(pos, playerName, name, color, null, "main", -1);
    }

    public String groupKey() {
        // '$' is not a valid OPAC sub-ID: a user-created subclaim named 'main'
        // must never collide with the owner's main claim group.
        return ownerId == null ? OpacClaim.createKey(playerName) : ownerKey() + ":" + (subIndex < 0 ? "$main" : subId);
    }

    public String ownerKey() {
        return OpacClaim.createKey(ownerId == null ? playerName : ownerId.toString());
    }

	public String getName() {
		return (name.isEmpty() ? "" : name + " - ") + playerName + "'s claim";
	}

	public String getKey() {
		return "OpacChunk:" + pos.x() + ":" + pos.z();
	}

	public List<IPoint> getCorners() {
		return List.of(
				// northwest
				OpacPoint.ofChunkPos(pos().x(), pos().z()),
				// northeast
				OpacPoint.ofChunkPos(pos().x() + 1, pos().z()),
				// southeast
				OpacPoint.ofChunkPos(pos().x() + 1, pos().z() + 1),
				// southwest
				OpacPoint.ofChunkPos(pos().x(), pos().z() + 1)
		);
	}

	public List<OpacEdge> getEdges() {
		var corners = getCorners();
		return List.of(
				// north
				new OpacEdge(corners.get(0), corners.get(1)),
				// east
				new OpacEdge(corners.get(1), corners.get(2)),
				// south
				new OpacEdge(corners.get(2), corners.get(3)),
				// west
				new OpacEdge(corners.get(3), corners.get(0))
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof OpacChunk other) {
			return pos.equals(other.pos);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}
}

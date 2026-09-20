package net.squaremarkers.fabric.compat;

import net.squaremarkers.core.interfaces.entities.IPoint;
import net.minecraft.world.level.ChunkPos;
import org.intellij.lang.annotations.Language;

import java.util.*;

public class OpacClaim {

	public final String key;
	@Language("HTML")
	public final String name;
	public final int color;

	private final Map<ChunkPos, OpacChunk> chunks;

	public OpacClaim(String playerName, @Language("HTML") String name, int color) {
		key = createKey(playerName);
		chunks = new HashMap<>();
		this.name = name;
		this.color = color;
	}

	public static String createKey(String playerName) {
		return "OpacClaim:" + playerName;
	}

	public void addChunk(OpacChunk chunk) {
		chunks.put(chunk.pos(), chunk);
	}

	public void removeChunk(OpacChunk chunk) {
		chunks.remove(chunk.pos());
	}

	public boolean removeChunk(int x, int z) {
		return chunks.remove(new ChunkPos(x, z)) != null;
	}

	public Collection<OpacChunk> chunks() {
		return List.copyOf(chunks.values());
	}

	public boolean isEmpty() {
		return chunks.isEmpty();
	}

	public List<List<IPoint>> getPolygons() {
		if (chunks.isEmpty()) {
			return Collections.emptyList();
		}

		Map<OpacEdge, Integer> edgeCounts = new HashMap<>();
		for (OpacChunk chunk : chunks.values()) {
			for (OpacEdge edge : chunk.getEdges()) {
				edgeCounts.put(edge, edgeCounts.getOrDefault(edge, 0) + 1);
			}
		}

		Map<IPoint, List<OpacEdge>> adjacencyMap = new HashMap<>();
		Set<OpacEdge> availableEdges = new HashSet<>();
		for (Map.Entry<OpacEdge, Integer> entry : edgeCounts.entrySet()) {
			if (entry.getValue() == 1) { // Boundary edge
				OpacEdge edge = entry.getKey();
				availableEdges.add(edge);
				adjacencyMap.computeIfAbsent(edge.p1(), k -> new ArrayList<>()).add(edge);
				adjacencyMap.computeIfAbsent(edge.p2(), k -> new ArrayList<>()).add(edge);
			}
		}

		List<List<IPoint>> orderedLoops = new ArrayList<>();
		while (!availableEdges.isEmpty()) {
			OpacEdge startEdge = availableEdges.iterator().next();
			availableEdges.remove(startEdge);

			List<IPoint> currentLoop = new ArrayList<>();

			IPoint startPoint = startEdge.p1();
			IPoint current = startEdge.p2();
			currentLoop.add(startPoint);

			while (true) {
				currentLoop.add(current);
				if (current.equals(startPoint)) {
					break;
				}
				List<OpacEdge> connectedEdges = adjacencyMap.get(current);
				if (connectedEdges == null) {
					break;
				}
				OpacEdge nextEdge = null;
				for (OpacEdge edge : connectedEdges) {
					if (availableEdges.contains(edge)) {
						nextEdge = edge;
						break;
					}
				}
				if (nextEdge == null) {
					break;
				}
				availableEdges.remove(nextEdge);
				current = (nextEdge.p1().equals(current)) ? nextEdge.p2() : nextEdge.p1();
			}
			orderedLoops.add(currentLoop);
		}

		return orderedLoops;
	}

}

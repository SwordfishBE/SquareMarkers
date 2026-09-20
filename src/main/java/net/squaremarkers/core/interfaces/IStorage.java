package net.squaremarkers.core.interfaces;

public interface IStorage {

	IWorldRepository getWorldRepository(String worldIdentifier);

	void close();
}

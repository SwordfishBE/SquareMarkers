package net.squaremarkers.core.interfaces.entities;

import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.Point;

public interface IPoint extends Comparable<IPoint> {

	int x();

	int y();

	int z();

	IPoint add(int dx, int dy, int dz);

	IPoint set(int x, int y, int z);

	/**
	 * Convert this point to a squaremap point. The y coordinate is ignored because the map is top down.
	 *
	 * @return squaremap point with the same x and z coordinates as this point
	 */
	default Point toMapPoint() {
		return Point.of(x(), z());
	}

	/**
	 * Get the distance of 2 points. The y coordinate is ignored as they are not important
	 *
	 * @param other the point to measure the distance to
	 * @return the distance between the two points, calculated using the Pythagorean theorem, ignoring the y coordinate
	 */
	default double distance(@NotNull IPoint other) {
		return Math.hypot((double) x() - other.x(), (double) z() - other.z());
	}

	/**
	 * Get the distance of 2 points. The y coordinate is ignored as they are not important
	 *
	 * @param x the x-coordinate of the other point
	 * @param z the z-coordinate of the other point
	 * @return the distance between the two points, calculated using the Pythagorean theorem, ignoring the y coordinate
	 */
	default double distance(int x, int z) {
		return Math.hypot((double) x() - x, (double) z() - z);
	}

	/**
	 * Get the distance of 2 points.
	 *
	 * @param x the x-coordinate of the other point
	 * @param y the y-coordinate of the other point
	 * @param z the z-coordinate of the other point
	 * @return the distance between the two points, calculated using the Pythagorean theorem, ignoring the y coordinate
	 */
	default double distance(int x, int y, int z) {
		double dx = (double) x() - x;
		double dy = (double) y() - y;
		double dz = (double) z() - z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Get the middle between these 2 points.
	 *
	 * @param other the other point
	 * @return the middle of 2 points
	 */
	default IPoint middle(IPoint other) {
		return this.set(
				(int) (((long) this.x() + other.x()) / 2L),
				(int) (((long) this.y() + other.y()) / 2L),
				(int) (((long) this.z() + other.z()) / 2L)
		);
	}

	/**
	 * Compare this point to another point. The comparison is based on the sum of the differences in x and z
	 * coordinates.
	 * The y coordinate is ignored as they are not important
	 *
	 * @param other the object to be compared.
	 * @return a negative integer, zero, or a positive integer as this point is less than, equal to, or greater than the
	 * 		specified object.
	 */
	@Override
	default int compareTo(@NotNull IPoint other) {
		int byX = Integer.compare(x(), other.x());
		if (byX != 0) {
			return byX;
		}
		int byZ = Integer.compare(z(), other.z());
		return byZ != 0 ? byZ : Integer.compare(y(), other.y());
	}

	default String serialize() {
		return x() + ":" + y() + ":" + z();
	}

	/**
	 * Compare this point to another point.
	 * The y coordinate is ignored if either this points or the supplied y is equal to {@code Integer.MIN_VALUE}
	 *
	 * @param x x-coordinate to compare to
	 * @param y y-coordinate to compare to
	 * @param z z-coordinate to compare to
	 * @return {@code true} if the x and z coordinates are equal and the y coordinates are either equal or one of them
	 * 		is {@code Integer.MIN_VALUE}, {@code false} otherwise
	 */
	default boolean equals(int x, int y, int z) {
		return x() == x
				&& (y() == y || y() == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
				&& z() == z;
	}

}

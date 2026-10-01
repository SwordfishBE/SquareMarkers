package net.squaremarkers.core.json.serializers;

import com.google.gson.*;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.json.entities.Point;

import java.lang.reflect.Type;

public class PointSerializer implements JsonSerializer<IPoint>, JsonDeserializer<IPoint> {

	@Override
	public JsonElement serialize(IPoint src, Type typeOfSrc, JsonSerializationContext context) {
		var obj = new JsonArray();
		obj.add(src.x());
		obj.add(src.y());
		obj.add(src.z());
		return obj;
	}

	@Override
	public IPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (!json.isJsonArray() || json.getAsJsonArray().size() != 3) {
			throw new JsonParseException("A marker position must contain exactly three integer coordinates");
		}
		var arr = json.getAsJsonArray();
		int x = coordinate(arr.get(0));
		int y = coordinate(arr.get(1));
		int z = coordinate(arr.get(2));
		if (typeOfT.equals(Point.class)) {
			return new Point(x, y, z);
		}
		throw new JsonParseException("Unsupported IPoint type: " + typeOfT.getTypeName());
	}

	private static int coordinate(JsonElement element) {
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new JsonParseException("A marker coordinate must be an integer");
		}
		try {
			return element.getAsBigDecimal().intValueExact();
		} catch (ArithmeticException | NumberFormatException exception) {
			throw new JsonParseException("Invalid marker coordinate", exception);
		}
	}

}

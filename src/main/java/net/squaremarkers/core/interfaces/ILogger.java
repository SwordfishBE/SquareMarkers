package net.squaremarkers.core.interfaces;

public interface ILogger {

	void debug(String message);

	void warn(String message, Throwable throwable);

}

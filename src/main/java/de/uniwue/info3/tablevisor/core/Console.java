package de.uniwue.info3.tablevisor.core;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Paths;

public class Console {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		TableVisor tv;

		try {
			if (args.length > 0) {
				// set configuration file via command line arguments
				tv = new TableVisor(Paths.get(args[0]));
			}
			else {
				// set default configuration file (primary for testing purpose)
				tv = new TableVisor(Paths.get("src/main/resources/config.yml"));
			}

			tv.start();
		}
		catch (IOException e) {
			logger.error("Could not initialize TableVisor", e);
		}
	}
}

package org.badiff.io;

import java.io.IOException;

public class RuntimeIOException extends RuntimeException {

	public RuntimeIOException(IOException cause) {
		super(cause);
	}

	public RuntimeIOException(String message, IOException cause) {
		super(message, cause);
	}

}

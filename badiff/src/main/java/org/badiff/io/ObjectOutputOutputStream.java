package org.badiff.io;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

/**
 * {@link OutputStream} that reads from an {@link ObjectOutput}
 * @author robin
 *
 */
public class ObjectOutputOutputStream extends OutputStream {

	protected ObjectOutput out;
	
	public ObjectOutputOutputStream(ObjectOutput out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

}
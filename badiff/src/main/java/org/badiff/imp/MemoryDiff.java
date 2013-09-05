package org.badiff.imp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.badiff.Diff;
import org.badiff.Op;
import org.badiff.io.Serialization;
import org.badiff.io.Serialized;
import org.badiff.q.ListOpQueue;
import org.badiff.q.OpQueue;

/**
 * Implementation of {@link Diff} that lives entirely in memory, backed
 * by a {@link List} of {@link Op}.
 * @author robin
 *
 */
public class MemoryDiff implements Diff, Serialized {
	private static final long serialVersionUID = 0;
	
	protected List<Op> ops = new ArrayList<Op>();

	@Override
	public void apply(InputStream orig, OutputStream target)
			throws IOException {
		for(Op e : ops)
			e.apply(orig, target);
	}

	@Override
	public void store(Iterator<Op> ops) {
		this.ops.clear();
		while(ops.hasNext())
			this.ops.add(ops.next());
	}

	@Override
	public OpQueue queue() {
		return new MemoryOpQueue(ops);
	}

	private class MemoryOpQueue extends ListOpQueue {
		private MemoryOpQueue(List<Op> ops) {
			super(ops);
		}
	
		@Override
		public boolean offer(Op e) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void serialize(Serialization serial, OutputStream out)
			throws IOException {
		serial.writeObject(out, Integer.class, ops.size());
		for(Op op : ops)
			serial.writeObject(out, Op.class, op);
	}

	@Override
	public void deserialize(Serialization serial, InputStream in)
			throws IOException {
		int size = serial.readObject(in, Integer.class);
		for(int i = 0; i < size; i++)
			ops.add(serial.readObject(in, Op.class));
	}

}

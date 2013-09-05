package org.badiff.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.WriteAbortedException;
import java.net.URI;
import java.util.Iterator;

import org.badiff.Diff;
import org.badiff.DiffOp;
import org.badiff.q.OpQueue;
import org.badiff.util.Streams;

public abstract class FileDiff extends File implements Diff {
	private static final long serialVersionUID = 0;
	
	protected abstract Serialization serialization();

	public FileDiff(File parent, String child) {
		super(parent, child);
	}

	public FileDiff(String parent, String child) {
		super(parent, child);
	}

	public FileDiff(String pathname) {
		super(pathname);
	}

	public FileDiff(URI uri) {
		super(uri);
	}

	public FileDiff(File file) {
		this(file.toURI());
	}
	
	@Override
	public void apply(InputStream orig, OutputStream target) throws IOException {
		InputStream self = new FileInputStream(this);
		try {
			long count = serialization().readObject(self, Long.class);
			for(long i = 0; i < count; i++)
				serialization().readObject(self, DiffOp.class).apply(orig, target);
		} finally { 
			self.close();
		}
	}
	
	@Override
	public OpQueue queue() throws IOException {
		return new FileOpQueue();
	}
	
	@Override
	public void store(Iterator<DiffOp> ops) throws IOException {
		write(ops);
	}
	
	public long write(Iterator<DiffOp> q) throws IOException {
		long count = 0;
		File tmp = File.createTempFile(getName(), ".tmp");
		
		FileOutputStream out = new FileOutputStream(tmp);
		while(q.hasNext()) {
			DiffOp e = q.next();
			serialization().writeObject(out, e);
			count++;
		}
		out.close();
		
		out = new FileOutputStream(this);
		InputStream in = new FileInputStream(tmp);
		serialization().writeObject(out, count);
		Streams.copy(in, out);
		in.close();
		out.close();
		
		tmp.delete();
		return count;
	}
	
	private class FileOpQueue extends OpQueue {
		private InputStream self;
		private long count;
		private long i;
		private boolean closed;
		
		public FileOpQueue() throws IOException {
			self = new FileInputStream(FileDiff.this);
			count = serialization().readObject(self, Long.class);
			i = 0;
			closed = false;
			if(count == 0)
				close();
		}
		
		@Override
		public boolean offer(DiffOp e) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		protected void shift() {
			if(!closed && i < count) {
				try {
					super.offer(serialization().readObject(self, DiffOp.class));
					i++;
				} catch(IOException ioe) {
					close();
					throw new RuntimeIOException(ioe);
				} finally {
					if(i == count)
						close();
				}
			}
			super.shift();
		}
		
		private void close() {
			try {
				self.close();
			} catch(IOException ioe) {
				throw new RuntimeIOException(ioe);
			} finally {
				closed = true;
			}
		}
	}
}

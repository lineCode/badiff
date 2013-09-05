package org.badiff.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.badiff.Applyable;
import org.badiff.io.RuntimeIOException;
import org.badiff.q.ChunkingOpQueue;
import org.badiff.q.CoalescingOpQueue;
import org.badiff.q.OpQueue;
import org.badiff.q.ParallelGraphOpQueue;
import org.badiff.q.ReplaceOpQueue;
import org.badiff.q.StreamChunkingOpQueue;

/**
 * Utility methods for computing and using differences
 * @author robin
 *
 */
public class Diffs {
	
	public static OpQueue queue(byte[] orig, byte[] target) {
		return new ReplaceOpQueue(orig, target);
	}
	
	public static OpQueue queue(InputStream orig, InputStream target) {
		return new StreamChunkingOpQueue(orig, target);
	}
	
	public static OpQueue improved(OpQueue q) {
		q = new ChunkingOpQueue(q);
		q = new ParallelGraphOpQueue(q);
		q = new CoalescingOpQueue(q);
		return q;
	}
	
	public static byte[] apply(Applyable a, byte[] orig) {
		ByteArrayInputStream in = new ByteArrayInputStream(orig);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			a.apply(in, out);
		} catch(IOException ioe) {
			throw new RuntimeIOException(ioe);
		}
		return out.toByteArray();
	}
	
	public static void apply(Applyable a, File orig, File target) throws IOException {
		FileInputStream in = new FileInputStream(orig);
		try {
			FileOutputStream out = new FileOutputStream(target);
			try {
				a.apply(in, out);
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

	private Diffs() {
	}

}

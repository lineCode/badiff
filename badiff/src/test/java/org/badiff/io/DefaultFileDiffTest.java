package org.badiff.io;

import java.io.File;
import java.util.Arrays;

import org.badiff.Diffs;
import org.badiff.imp.DefaultFileDiff;
import org.badiff.imp.FileDiff;
import org.junit.Assert;
import org.junit.Test;

public class DefaultFileDiffTest {

	@Test
	public void testWriteRead() throws Exception {
		byte[] orig = "Hello world!".getBytes();
		byte[] target = "Hellish cruel world!".getBytes();
		
		FileDiff fd = new DefaultFileDiff(File.createTempFile("filediff", ".tmp"));
		fd.store(Diffs.improved(Diffs.queue(orig, target)));
		
		byte[] result = Diffs.apply(fd.queue(), orig);
		
		fd.delete();
		
		Assert.assertTrue(Arrays.equals(target, result));
		
	}

}

/**
 * badiff - byte array diff - fast pure-java byte-level diffing
 * 
 * Copyright (c) 2013, Robin Kirkman All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 2) Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3) Neither the name of the badiff nor the names of its contributors may be 
 *    used to endorse or promote products derived from this software without 
 *    specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.badiff;

import java.util.Arrays;

import org.badiff.io.DefaultSerialization;
import org.badiff.io.Serialization;
import org.junit.Assert;
import org.junit.Test;

public class ByteArrayDiffsTest {
	
	protected Serialization serial;
	
	public ByteArrayDiffsTest() {
		this(DefaultSerialization.newInstance());
	}
	
	protected ByteArrayDiffsTest(Serialization serial) {
		this.serial = serial;
		
	}

	@Test
	public void testDiff() throws Exception {
		String orig = "Hello world!";
		String target = "Hellish cruel world!";
		
		byte[] diff = ByteArrayDiffs.diff(orig.getBytes(), target.getBytes());
		System.out.println("diff:" + diff.length);
		
		byte[] udiff = ByteArrayDiffs.udiff(orig.getBytes(), target.getBytes());
		System.out.println("udiff:" + udiff.length);
	}
	
	@Test
	public void testDiff_issue_2() throws Exception {
		byte[] prior = new byte[2270];
		byte[] current = new byte[2281];
		@SuppressWarnings("unused")
		byte[] diff = ByteArrayDiffs.diff(prior, current);
		
	}
	
	@Test
	public void testUndo() throws Exception {
		byte[] orig = "Hello World".getBytes();
		byte[] target = "Hellish Cruel World".getBytes();
		
		byte[] diff = ByteArrayDiffs.diff(orig, target);
		
		byte[] undone = ByteArrayDiffs.undo(target, diff);
		
		Assert.assertTrue(Arrays.equals(orig, undone));
	}

	
}

package org.badiff.patcher;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.badiff.io.Serialization;
import org.badiff.io.Serialized;
import org.badiff.util.Digests;

public class SerializedDigest implements Serialized {
	private String algorithm;
	private byte[] digest;
	
	public SerializedDigest() {}
	
	public SerializedDigest(String algorithm, File content) throws IOException {
		if(content == null)
			throw new IllegalArgumentException();
		this.algorithm = algorithm;
		this.digest = Digests.digest(content, Digests.digest(algorithm));
	}
	
	public SerializedDigest(String algorithm, String content) {
		this(algorithm, Digests.digest(algorithm).digest(content.getBytes(Charset.forName("UTF-8"))));
	}
	
	public SerializedDigest(String algorithm, byte[] digest) {
		if(digest == null)
			throw new IllegalArgumentException();
		this.digest = digest;
	}
	
	public SerializedDigest(String asString) {
		String[] f = asString.split(" ", 2);
		algorithm = f[0];
		digest = Digests.parse(f[1]);
	}

	@Override
	public void serialize(Serialization serial, DataOutput out)
			throws IOException {
		serial.writeObject(out, String.class, algorithm);
		serial.writeObject(out, byte[].class, digest);
	}

	@Override
	public void deserialize(Serialization serial, DataInput in)
			throws IOException {
		algorithm = serial.readObject(in, String.class);
		digest = serial.readObject(in, byte[].class);
	}
	
	@Override
	public String toString() {
		return algorithm + " " + Digests.pretty(digest);
	}
	
	public String getAlgorithm() {
		return algorithm;
	}
	
	public byte[] getDigest() {
		return digest;
	}
}

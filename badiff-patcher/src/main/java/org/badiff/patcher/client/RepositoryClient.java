package org.badiff.patcher.client;

import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.badiff.imp.BadiffFileDiff;
import org.badiff.io.Serialization;
import org.badiff.patcher.PatcherSerialization;
import org.badiff.patcher.PathDiff;
import org.badiff.patcher.PathDigest;
import org.badiff.patcher.SerializedDigest;
import org.badiff.util.Data;
import org.badiff.util.Digests;

public class RepositoryClient {
	protected RepositoryAccess serverAccess;
	
	protected PathDiffChain chain;
	protected Map<String, SerializedDigest> digests;
	
	protected File storage;
	
	public RepositoryClient(RepositoryAccess access, File storage) throws IOException {
		this.serverAccess = access;
		chain = new PathDiffChain(this);
		digests = new HashMap<String, SerializedDigest>();
		if(!storage.isDirectory() && !storage.mkdirs())
			throw new IOException("Unable to create directory " + storage);
		File ff = new File(storage, "ff");
		if(!ff.isDirectory() && !ff.mkdirs())
			throw new IOException("Unable to create directory " + ff);
		File rw = new File(storage, "rw");
		if(!rw.isDirectory() && !rw.mkdirs())
			throw new IOException("Unable to create directory " + rw);
		File id = new File(storage, "id");
		if(!id.isDirectory() && !id.mkdirs())
			throw new IOException("Unable to create directory " + id);
		this.storage = storage;
	}
	
	public String pathForId(SerializedDigest pathId) throws IOException {
		File id = new File(storage, "id/" + pathId);
		if(id.isFile()) {
			InputStream in = new FileInputStream(id);
			try {
				return IOUtils.toString(in, Charset.forName("UTF-8"));
			} finally {
				in.close();
			}
		}
		
		InputStream in = serverAccess.get("id/" + pathId).open();
		if(in == null)
			return null;
		String path;
		try {
			path = IOUtils.toString(in, Charset.forName("UTF-8"));
		} finally {
			in.close();
		}
		OutputStream out = new FileOutputStream(id);
		try {
			IOUtils.write(path, out, Charset.forName("UTF-8"));
		} finally {
			out.close();
		}
		return path;
	}
	
	public void updateChain() throws IOException {
		chain.clear();
		loadChain(new FileRepositoryAccess(storage));
		loadChain(serverAccess);
	}
	
	protected void loadChain(RepositoryAccess access) throws IOException {
		List<RemotePath> diffs = Arrays.asList(access.get("ff").list());
		Collections.sort(diffs, RemotePath.LAST_MODIFIED_ORDER);
		for(RemotePath d : diffs) {
			chain.add(PathDiff.parseName(d.name()));
		}
	}
	
	public void updateDigests() throws IOException {
		digests.clear();
		InputStream in = serverAccess.get("digests").open();
		DataInput data = Data.asInput(in);
		Serialization serial = PatcherSerialization.newInstance();
		int size = serial.readObject(data, int.class);
		for(int i = 0; i < size; i++) {
			PathDigest pd = serial.readObject(data, PathDigest.class);
			digests.put(pd.getPath(), pd.getDigest());
		}
	}
	
	public PathDiff localFastForward(PathDiff pd) throws IOException {
		BadiffFileDiff ff = new BadiffFileDiff(storage, "ff/" + pd.getName());
		if(!ff.exists()) {
			File tmp = new File(ff.getParentFile(), ff.getName() + ".download");
			ff.getParentFile().mkdirs();
			OutputStream out = new FileOutputStream(tmp);
			InputStream in = serverAccess.get("ff/" + pd.getName()).open();
			IOUtils.copy(in, out);
			in.close();
			out.close();
			if(!tmp.renameTo(ff))
				throw new IOException("Unable to replace " + ff);
		}
		return new PathDiff(pd.getName(), ff);
	}
	
	public PathDiff localRewind(PathDiff pd) throws IOException {
		BadiffFileDiff rw = new BadiffFileDiff(storage, "rw/" + pd.getName());
		if(!rw.exists()) {
			File tmp = new File(rw.getParentFile(), rw.getName() + ".download");
			rw.getParentFile().mkdirs();
			OutputStream out = new FileOutputStream(tmp);
			InputStream in = serverAccess.get("rw/" + pd.getName()).open();
			IOUtils.copy(in, out);
			in.close();
			out.close();
			if(!tmp.renameTo(rw))
				throw new IOException("Unable to replace " + rw);
		}
		return new PathDiff(pd.getName(), rw);
	}
	
	public PathAction actionFor(File root, String path, SerializedDigest toId) throws IOException {
		if(toId == null)
			toId = new SerializedDigest(Digests.DEFAULT_ALGORITHM, Digests.defaultZeroes());
		SerializedDigest pathId = new SerializedDigest(Digests.DEFAULT_ALGORITHM, path);
		SerializedDigest fromId = new SerializedDigest(Digests.DEFAULT_ALGORITHM, new File(root, path));
		return chain.actionFor(pathId, fromId, toId);
	}
	
	public RepositoryAccess getServerAccess() {
		return serverAccess;
	}
	
	public PathDiffChain getChain() {
		return chain;
	}
	
	public Map<String, SerializedDigest> getDigests() {
		return digests;
	}
	
	public File getStorage() {
		return storage;
	}
	
	public void setStorage(File storage) {
		this.storage = storage;
	}
}

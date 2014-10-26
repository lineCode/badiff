package org.badiff.patcher;

import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.badiff.imp.BadiffFileDiff;
import org.badiff.io.Serialization;
import org.badiff.patcher.util.Files;
import org.badiff.patcher.util.Sets;
import org.badiff.util.Data;
import org.badiff.util.Digests;

public class LocalRepository {
	protected File root;
	
	public LocalRepository(File root) {
		if(!root.isDirectory())
			throw new IllegalArgumentException("repository root must be a directory");
		this.root = root;
		if(!getWorkingCopyRoot().isDirectory() && !getWorkingCopyRoot().mkdirs())
			throw new IllegalArgumentException(getWorkingCopyRoot() + " is not a directory");
		if(!getPathDiffsRoot().isDirectory() && !getPathDiffsRoot().mkdirs())
			throw new IllegalArgumentException(getPathDiffsRoot() + " is not a directory");
	}
	
	public void commit(File newWorkingCopyRoot) throws IOException {
		Set<String> diffsNames = new HashSet<String>(Files.listRelativePaths(getPathDiffsRoot()));
		Set<String> fromPaths = new HashSet<String>(Files.listRelativePaths(getWorkingCopyRoot()));
		Set<String> toPaths = new HashSet<String>(Files.listRelativePaths(newWorkingCopyRoot));
		
		Set<String> deletedPaths = Sets.subtraction(fromPaths, toPaths);
		Set<String> createdOrModifiedPaths = toPaths;
		
		Set<String> deletedPrefixes = new HashSet<String>();
		for(String path : deletedPaths)
			deletedPrefixes.add(new SerializedDigest(path).toString());
		
		Iterator<String> dni = diffsNames.iterator();
		while(dni.hasNext()) {
			String diffName = dni.next();
			if(deletedPrefixes.contains(diffName.split("\\.")[0])) {
				if(!new File(getPathDiffsRoot(), diffName).delete())
					throw new IOException("unable to delete " + diffName);
				dni.remove();
			}
		}
		
		Set<PathDigest> pathDigests = new HashSet<PathDigest>();
		
		for(String path : createdOrModifiedPaths) {
			File fromFile = new File(getWorkingCopyRoot(), path);
			if(!fromFile.exists())
				continue;
			File toFile = new File(newWorkingCopyRoot, path);
			SerializedDigest fromDigest = new SerializedDigest(Digests.DEFAULT_ALGORITHM, fromFile);
			SerializedDigest toDigest = new SerializedDigest(Digests.DEFAULT_ALGORITHM, toFile);
			if(fromDigest.equals(toDigest)) {
				pathDigests.add(new PathDigest(path, toDigest));
				continue;
			}
			String prefix = new SerializedDigest(Digests.DEFAULT_ALGORITHM, path).toString();
			
			BadiffFileDiff tmpDiff = new BadiffFileDiff(root, "tmp." + prefix + ".badiff");
			tmpDiff.diff(fromFile, toFile);
			PathDiff tmpPD = new PathDiff(path, tmpDiff);
			BadiffFileDiff diff = new BadiffFileDiff(getPathDiffsRoot(), tmpPD.getName());
			tmpDiff.renameTo(diff);
			
			pathDigests.add(new PathDigest(path, toDigest));
		}
		
		OutputStream out = new FileOutputStream(new File(root, "digests"));
		DataOutput data = Data.asOutput(out);
		Serialization serial = PatcherSerialization.newInstance();
		serial.writeObject(data, int.class, pathDigests.size());
		for(PathDigest pd : pathDigests)
			serial.writeObject(data, PathDigest.class, pd);
		out.close();
	}
	
	public File getRoot() {
		return root;
	}
	
	public File getWorkingCopyRoot() {
		return new File(root, "working_copy");
	}
	
	public File getPathDiffsRoot() {
		return new File(root, "diffs");
	}
}

package org.badiff;

import java.util.Set;

public interface Patch extends FileApplyable {
	public Set<String> keySet();
	public boolean containsKey(String path);
	public PatchOp get(String path);
	public PatchOp put(String path, PatchOp diff);
}

package org.badiff.nat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.badiff.Op;
import org.badiff.alg.Graph;

public class NativeGraph extends Graph {
	private static final byte STOP = Op.STOP;
	private static final byte DELETE = Op.DELETE;
	private static final byte INSERT = Op.INSERT;
	private static final byte NEXT = Op.NEXT;
	
	private native void compute0(byte[] orig, byte[] target);

	private native boolean walk0();
	private native byte flag0();
	private native byte val0();
	private native void prev0();
	
	private native void free0();
	
	private long data;
	
	public NativeGraph() {
		super(1);
	}
	
	@Override
	public void compute(byte[] orig, byte[] target) {
		free0();
		compute0(orig, target);
	}
	
	@Override
	public List<Op> rlist() {
		if(!walk0())
			throw new IllegalStateException("Graph not computed");
		List<Op> ret = new ArrayList<Op>();

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte op = Op.STOP;
		int run = 0;
		
		while(flag0() != Op.STOP) {
			byte fop = flag0();
			if(op != Op.STOP && op != fop) {
				byte[] data = null;
				if(op == Op.INSERT || op == Op.DELETE) {
					byte[] rdata = buf.toByteArray();
					data = new byte[rdata.length];
					for(int i = 0; i < rdata.length; i++) {
						data[data.length - i - 1] = rdata[i];
					}
				}
				ret.add(new Op(op, run, data));
				run = 0;
				buf.reset();
			}
			op = fop;
			run++;
			if(op == Op.INSERT) {
				buf.write(val0());
			}
			if(op == Op.DELETE) {
				buf.write(val0());
			}
			prev0();
		}
		
		if(op != Op.STOP) {
			byte[] data = null;
			if(op == Op.INSERT || op == Op.DELETE) {
				byte[] rdata = buf.toByteArray();
				data = new byte[rdata.length];
				for(int i = 0; i < rdata.length; i++) {
					data[data.length - i - 1] = rdata[i];
				}
			}
			ret.add(new Op(op, run, data));
		}

		return ret;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		free0();
	}
}

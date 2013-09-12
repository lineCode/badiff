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
package org.badiff.alg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.badiff.Op;
import org.badiff.q.CompactingOpQueue;
import org.badiff.q.ListOpQueue;
import org.badiff.q.OpQueue;

public class InnertialGraph implements Graph {

	private class Node {
		public boolean nextable;
		public short enterDeleteCost, enterInsertCost, enterNextCost;
		public short leaveDeleteCost, leaveInsertCost, leaveNextCost;

		public void computeDeleteCost() {
			int cost;

			cost = enterDeleteCost; // appending a delete is free

			if(enterInsertCost + 2 < cost) { // costs 3 to switch from insert to delete
				cost = enterInsertCost + 3;
			}

			if(enterNextCost + 2 < cost) { // costs 3 to switch from next to delete
				cost = enterNextCost + 3;
			}

			leaveDeleteCost = (short) cost;
		}

		public void computeInsertCost() {
			int cost;

			cost = enterInsertCost + 1; // appending an insert costs 1

			if(enterDeleteCost + 3 < cost) { // costs 3 to switch from delete to insert
				cost = enterDeleteCost + 3;
			}

			if(enterNextCost + 3 < cost) { // costs 3 to switch from next to insert
				cost = enterNextCost + 3;
			}

			leaveInsertCost = (short) cost;
		}

		public void computeNextCost() {
			if(!nextable) {
				leaveNextCost = Short.MAX_VALUE;
				return;
			}

			int cost;

			cost = enterNextCost; // appending a next is free

			if(enterDeleteCost + 2 < cost) { // costs 2 to switch from delete to next
				cost = enterDeleteCost + 2;
			}

			if(enterInsertCost + 2 < cost) { // costs 2 to switch from insert to next
				cost = enterInsertCost + 2;
			}

			leaveNextCost = (short) cost;
		}
	}

	private int capacity;
	private Node[] nodes;
	private byte[] xval;
	private byte[] yval;

	public InnertialGraph(int capacity) {
		if(capacity < 4)
			throw new IllegalArgumentException("capacity must be >= 4");

		this.capacity = capacity;
		nodes = new Node[capacity];
		for(int i = 0; i < capacity; i++)
			nodes[i] = new Node();

		nodes[0].leaveDeleteCost = 2;
		nodes[0].leaveInsertCost = 3;
		nodes[0].leaveNextCost = 2;
	}

	public void compute(byte[] orig, byte[] target) {
		if((orig.length + 1) * (target.length + 1) > capacity)
			throw new IllegalArgumentException("diff axes exceed graph capacity");
		
		xval = new byte[orig.length + 1]; System.arraycopy(orig, 0, xval, 1, orig.length);
		yval = new byte[target.length + 1]; System.arraycopy(target, 0, yval, 1, target.length);

		int pos = -1;
		for(int y = 0; y < yval.length; y++) {
			for(int x = 0; x < xval.length; x++) {
				pos++;
				if(x == 0 && y == 0)
					continue;

				// mark entry costs
				Node node = nodes[pos];
				node.nextable = x > 0 && y > 0 && xval[x] == yval[y];
				node.enterDeleteCost = (x == 0) ? Short.MAX_VALUE : nodes[pos-1].leaveDeleteCost;
				node.enterInsertCost = (y == 0) ? Short.MAX_VALUE : nodes[pos-xval.length].leaveInsertCost;
				node.enterNextCost = (x == 0 || y == 0) ? Short.MAX_VALUE : nodes[pos-1-xval.length].leaveNextCost;

				node.computeDeleteCost();
				node.computeInsertCost();
				node.computeNextCost();
			}
		}	
	}

	public OpQueue queue() {
		OpQueue rq = new GraphOpQueue();
		List<Op> ops = new ArrayList<Op>();
		for(Op e = rq.poll(); e != null; e = rq.poll())
			ops.add(0, e);
		OpQueue q = new ListOpQueue(ops);
		q = new CompactingOpQueue(q);
		return q;
	}
	
	private class GraphOpQueue extends OpQueue {
		private int pos;
		
		
		public GraphOpQueue() {
			pos = xval.length * yval.length - 1;
			
			Node node = nodes[pos];
			int cost = node.leaveNextCost;
			byte op = Op.NEXT;
			
			if(node.leaveInsertCost < cost) {
				cost = node.leaveInsertCost;
				op = Op.INSERT;
			}
			
			if(node.leaveDeleteCost < cost) {
				cost = node.leaveDeleteCost;
				op = Op.DELETE;
			}
			
			switch(op) {
			case Op.NEXT:
				prepare(new Op(Op.NEXT, 1, null));
				break;
			case Op.INSERT:
				prepare(new Op(Op.INSERT, 1, new byte[] {yval[pos / xval.length]}));
				break;
			case Op.DELETE:
				prepare(new Op(Op.DELETE, 1, new byte[] {xval[pos % xval.length]}));
				break;
			}
		}
		
		@Override
		protected boolean pull() {
			if(pos == 0)
				return false;
			
			Node node = nodes[pos];
			
			byte op = Op.NEXT;
			int cost = node.enterNextCost;
			
			if(node.enterInsertCost < cost) {
				op = Op.INSERT;
				cost = node.enterInsertCost;
			}
			
			if(node.enterDeleteCost < cost) {
				op = Op.DELETE;
				cost = node.enterDeleteCost;
			}
			
			Op e = null;
			
			switch(op) {
			case Op.NEXT:
				pos = pos - 1 - xval.length;
				e = new Op(Op.NEXT, 1, null);
				break;
			case Op.INSERT:
				pos = pos - xval.length;
				e = new Op(Op.INSERT, 1, new byte[] {yval[pos / xval.length]});
				break;
			case Op.DELETE:
				pos = pos - 1;
				e = new Op(Op.DELETE, 1, new byte[] {xval[pos % xval.length]});
				break;
			}
			
			if(pos > 0)
				prepare(e);
			
			return true;
		}
	}
	
}

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
package org.badiff.q;

import org.badiff.Op;

/**
 * {@link OpQueue} that draws its pending elements from another wrapped {@link OpQueue}
 * @author robin
 *
 */
public class FilterOpQueue extends OpQueue {
	/**
	 * The source of elements for this {@link FilterOpQueue}
	 */
	protected OpQueue source;
	
	/**
	 * Create a new {@link OpQueue} that draws elements into {@link OpQueue#pending}
	 * by calling {@link OpQueue#poll()} on the argument
	 * @param source
	 */
	public FilterOpQueue(OpQueue source) {
		this.source = source;
	}
	
	/**
	 * Offers the element to the wrapped {@link OpQueue}
	 */
	@Override
	public boolean offer(Op e) {
		return source.offer(e);
	}
	
	@Override
	protected void shift() {
		filter();
		super.shift();
	}
	
	/**
	 * Called when an element should be moved from {@link #source} to {@link OpQueue#pending}
	 */
	protected void filter() {
		if(pending.size() == 0)
			shiftPending();
	}
	
	/**
	 * Actually move an element from {@link #source} to {@link OpQueue#pending}
	 * @return
	 */
	protected boolean shiftPending() {
		Op e = source.poll();
		if(e != null)
			pending.offerLast(e);
		return e != null;
	}

}

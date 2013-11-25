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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.badiff.Diff;
import org.badiff.Op;
import org.badiff.alg.AdjustableInertialGraph;
import org.badiff.alg.EditGraph;
import org.badiff.alg.Graph;
import org.badiff.alg.InertialGraph;

/**
 * {@link OpQueue} that locates pairs of ({@link Op#DELETE},{@link Op#INSERT}) and
 * applies {@link Graph} to them, in parallel.  This {@link OpQueue} is <b>PARTIALLY LAZY</b>.
 * Partially lazy means that it will eagerly draw elements until all worker threads are active
 * any time a lazy element request is made.
 * @author robin
 *
 */
public class ParallelGraphOpQueue extends FilterOpQueue {

	public static interface GraphFactory {
		public Graph newGraph(int capacity);
	}

	public static final GraphFactory EDIT_GRAPH = new GraphFactory() {
		@Override
		public Graph newGraph(int capacity) {
			return new EditGraph(capacity);
		}
	};

	public static final GraphFactory INERTIAL_GRAPH = new GraphFactory() {
		@Override
		public Graph newGraph(int capacity) {
			return new InertialGraph(capacity);
		}
	};

	public static final GraphFactory ADJUSTABLE_GRAPH = new GraphFactory() {
		@Override
		public Graph newGraph(int capacity) {
			return new AdjustableInertialGraph(capacity);
		}
	};

	private static final GraphFactory DEFAULT_GRAPH = INERTIAL_GRAPH;

	/**
	 * The real source of elements
	 */
	protected OpQueue input;
	/**
	 * The chunk size
	 */
	protected int chunk;
	/**
	 * Thread pool for parallelization
	 */
	protected ThreadPoolExecutor pool;

	protected ChainOpQueue chain;

	protected GraphFactory graphFactory;

	/**
	 * Thread-local of {@link Graph} to avoid allocating ridonkulous amounts of memory
	 */
	protected ThreadLocal<Graph> graphs = new ThreadLocal<Graph>() {
		protected Graph initialValue() {
			return graphFactory.newGraph((chunk+1) * (chunk+1));
		}
	};

	/**
	 * Create a new parallel graphing {@link OpQueue} with {@link Runtime#availableProcessors()}
	 * number of worker threads.
	 * @param source
	 */
	public ParallelGraphOpQueue(OpQueue source) {
		this(source, Runtime.getRuntime().availableProcessors(), Diff.DEFAULT_CHUNK, DEFAULT_GRAPH);
	}

	public ParallelGraphOpQueue(OpQueue source, GraphFactory graphFactory) {
		this(source, Runtime.getRuntime().availableProcessors(), Diff.DEFAULT_CHUNK, graphFactory);
	}

	/**
	 * Create a new parallel graphing {@link OpQueue} with the specified number of worker threads.
	 * @param source
	 * @param workers
	 */
	public ParallelGraphOpQueue(OpQueue source, int workers, int chunk, GraphFactory graphFactory) {
		super(new ChainOpQueue());
		this.input = source;
		this.chunk = chunk;
		this.graphFactory = graphFactory;
		pool = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, ParallelGraphOpQueue.this.toString());
				t.setDaemon(true);
				return t;
			}
		});
		chain = (ChainOpQueue) super.source;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + input;
	}

	/*
	 * Offer the input to the actual input queue, not the wrapped chain
	 * (non-Javadoc)
	 * @see org.badiff.q.FilterOpQueue#offer(org.badiff.Op)
	 */
	@Override
	public boolean offer(Op e) {
		return input.offer(e);
	}

	@Override
	protected boolean require(int count) {
		while(filtering.size() < count) {
			Op e = input.poll();
			if(e == null)
				return false;
			filtering.add(e);
		}
		return true;
	}

	@Override
	protected void prepare(Op e) {
		chain.offer(e);
	}

	protected void prepare(Future<OpQueue> f) {
		chain.offer(new FutureOpQueue(f));
		chain.offer(new OpQueue());
	}

	protected Callable<OpQueue> newTask(final Op delete, final Op insert) {
		return new Callable<OpQueue>() {
			@Override
			public OpQueue call() throws Exception {
				OpQueue graphed = new ReplaceOpQueue(delete.getData(), insert.getData());
				graphed = new GraphOpQueue(graphed, graphs.get());
				graphed = new ListOpQueue(graphed);
				return graphed;
			}
		};
	}
	
	@Override
	public Op poll() {
		pump();
		return super.poll();
	}
	
	protected void pump() {
		if(require(2)) {
			while(require(2) && pool.getActiveCount() < pool.getMaximumPoolSize()) {
				Op delete;
				Op insert;

				if(filtering.get(0).getOp() == Op.DELETE && filtering.get(1).getOp() == Op.INSERT) {
					delete = filtering.remove(0);
					insert = filtering.remove(0);
				} else if(filtering.get(0).getOp() == Op.INSERT && filtering.get(1).getOp() == Op.DELETE) {
					insert = filtering.remove(0);
					delete = filtering.remove(0);
				} else {
					prepare(filtering.remove(0));
					continue;
				}

				// construct a task and submit it to the pool
				prepare(pool.submit(newTask(delete, insert)));
			}

			if(!require(2)) {
				flush();
				pool.shutdown();
			}
		}
	}

	@Override
	protected boolean pull() {
		pump();

		Op e = chain.poll();
		
		if(e != null)
			super.prepare(e);
		return e != null;
	}
}

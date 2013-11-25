package org.badiff.p;

import org.badiff.alg.GraphFactory;
import org.badiff.alg.InertialGraph;
import org.badiff.q.ChunkingOpQueue;
import org.badiff.q.CoalescingOpQueue;
import org.badiff.q.CompactingOpQueue;
import org.badiff.q.GraphOpQueue;
import org.badiff.q.OneWayOpQueue;
import org.badiff.q.OpQueue;
import org.badiff.q.ParallelGraphOpQueue;
import org.badiff.q.PumpingOpQueue;
import org.badiff.q.RewindingOpQueue;
import org.badiff.q.UnchunkingOpQueue;
import org.badiff.q.UndoOpQueue;

public enum Pipes implements Pipe {
	OPQUEUE('q'),
	COALESS('c'),
	COMPACT('C'),
	GRAPH('g'),
	ONE_WAY('o'),
	PARALLEL_GRAPH('G'),
	PUMP('p'),
	REWIND('r'),
	UNCHUNK('u'),
	UNDO('U'),
	;
	
	private char code;
	
	private Pipes(char code) {
		this.code = code;
	}
	
	public char code() {
		return code;
	}
	
	public static Pipe fromCode(char code) {
		for(Pipes pipe : Pipes.values())
			if(pipe.code() == code)
				return pipe;
		throw new IllegalArgumentException("No such pipe code:" + code);
	}

	public static Pipe[] fromCodes(String codes) {
		Pipe[] pipes = new Pipe[codes.length()];
		for(int i = 0; i < pipes.length; i++)
			pipes[i] = Pipes.fromCode(codes.charAt(i));
		return pipes;
	}
	
	@Override
	public Pipeline from(OpQueue q) {
		switch(this) {
		case OPQUEUE:
			break;
		case COALESS:
			q = new CoalescingOpQueue(q);
			break;
		case COMPACT:
			q = new CompactingOpQueue(q);
			break;
		case GRAPH:
			q = new GraphOpQueue(q, new InertialGraph());
			break;
		case ONE_WAY:
			q = new OneWayOpQueue(q);
			break;
		case PARALLEL_GRAPH:
			q = new ParallelGraphOpQueue(q, GraphFactory.INERTIAL_GRAPH);
			break;
		case PUMP:
			q = new PumpingOpQueue(q);
			break;
		case REWIND:
			q = new RewindingOpQueue(q);
			break;
		case UNCHUNK:
			q = new UnchunkingOpQueue(q);
			break;
		case UNDO:
			q = new UndoOpQueue(q);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return new Pipeline(q);
	}
	
}
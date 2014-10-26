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
package org.badiff.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.badiff.Op;
import org.badiff.imp.FileDiff;
import org.badiff.imp.MemoryDiff;
import org.badiff.util.Data;
import org.badiff.util.HierarchicalMap;

public class DefaultSerialization implements Serialization {
	
	public static DefaultSerialization newInstance() {
		return new DefaultSerialization();
	}

	protected static abstract class Serializer<T> {
		protected Class<T> type;
		
		public Serializer(Class<T> type) {
			this.type = type;
		}
		
		public Class<T> type() {
			return type;
		}
		public abstract void write(DataOutput out, T obj) throws IOException;
		public abstract T read(DataInput in) throws IOException;
	}

	private List<Serializer<?>> serializers = new ArrayList<Serializer<?>>();
	private int depth;
	private Map<Object, Object> context = new HashMap<Object, Object>();
	private Map<Object, Object> graphContext = new HierarchicalMap<Object, Object>(new HashMap<Object, Object>(), context);
	
	@SuppressWarnings("rawtypes")
	public DefaultSerialization() {
		serializers.add(new Serializer<Class>(Class.class) {

			@Override
			public void write(DataOutput out, Class obj) throws IOException {
				for(int i = 0; i < serializers.size(); i++) {
					if(serializers.get(i).type() == obj) {
						writeLong(out, i);
						return;
					}
				}
				throw new NotSerializableException(obj.getName());
			}

			@Override
			public Class read(DataInput in) throws IOException {
				return serializers.get((int) readLong(in)).type();
			}
		});
		serializers.add(new Serializer<Byte>(byte.class) {

			@Override
			public void write(DataOutput out, Byte obj) throws IOException {
				out.writeByte(obj);
			}

			@Override
			public Byte read(DataInput in) throws IOException {
				return in.readByte();
			}
		});
		serializers.add(new Serializer<Integer>(int.class) {

			@Override
			public void write(DataOutput out, Integer obj) throws IOException {
				writeLong(out, obj);
			}

			@Override
			public Integer read(DataInput in) throws IOException {
				return (int) readLong(in);
			}
		});
		serializers.add(new Serializer<Long>(long.class) {

			@Override
			public void write(DataOutput out, Long obj) throws IOException {
				writeLong(out, obj);
			}

			@Override
			public Long read(DataInput in) throws IOException {
				return readLong(in);
			}
		});
		serializers.add(new Serializer<String>(String.class) {

			@Override
			public void write(DataOutput out, String obj) throws IOException {
				out.writeUTF(obj);
			}

			@Override
			public String read(DataInput in) throws IOException {
				return in.readUTF();
			}
		});
		serializers.add(new Serializer<byte[]>(byte[].class) {

			@Override
			public void write(DataOutput out, byte[] obj) throws IOException {
				writeLong(out, obj != null ? obj.length + 1 : 0);
				if(obj != null)
					out.write(obj);
			}

			@Override
			public byte[] read(DataInput in) throws IOException {
				int size = (int) readLong(in) - 1;
				if(size == -1)
					return null;
				byte[] obj = new byte[size];
				in.readFully(obj);
				return obj;
			}
		});
		
		serializers.add(new Serializer<Op>(Op.class) {

			@Override
			public void write(DataOutput out, Op obj) throws IOException {
				obj.serialize(DefaultSerialization.this, Data.asStream(out));
			}

			@Override
			public Op read(DataInput in) throws IOException {
				Op op = new Op();
				op.deserialize(DefaultSerialization.this, Data.asStream(in));
				return op;
				 
			}
		});
		
		serializers.add(new Serializer<MemoryDiff>(MemoryDiff.class) {

			@Override
			public void write(DataOutput out, MemoryDiff obj)
					throws IOException {
				obj.serialize(DefaultSerialization.this, Data.asStream(out));
			}

			@Override
			public MemoryDiff read(DataInput in) throws IOException {
				MemoryDiff md = new MemoryDiff();
				md.deserialize(DefaultSerialization.this, Data.asStream(in));
				return md;
			}
		});
		
		serializers.add(new Serializer<FileDiff>(FileDiff.class) {

			@Override
			public void write(DataOutput out, FileDiff obj)
					throws IOException {
				obj.serialize(DefaultSerialization.this, Data.asStream(out));
			}

			@Override
			public FileDiff read(DataInput in) throws IOException {
				FileDiff md = new FileDiff(File.createTempFile("FileDiff", ".tmp"));
				md.deserialize(DefaultSerialization.this, Data.asStream(in));
				return md;
			}
		});
		
	}
	
	protected void addSerializer(Serializer<?> s) {
		serializers.add(s);
	}
	
	public void writeLong(DataOutput out, long val) throws IOException {
		int v = (int)(val & 0x7f);
		if((val >>> 7) != 0)
			v |= 0x80;
		out.writeByte(v);
		if((val >>> 7) != 0)
			writeLong(out, val >>> 7);
	}
	
	public long readLong(DataInput in) throws IOException {
		return readLong(in, 0, 0);
	}
	
	private long readLong(DataInput in, long accum, int shift) throws IOException {
		long b = 0xff & in.readByte();
		accum |= (b & 0x7f) << (shift);
		if((b & 0x80) == 0)
			return accum;
		return readLong(in, accum, shift + 7);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> void writeObject(DataOutput out, Class<T> type, T object)
			throws IOException {
		if(depth == 0)
			graphContext.clear();
		depth++;
		try {
			for(@SuppressWarnings("rawtypes") Serializer s : serializers) {
				if(s.type() == type) {
					s.write(out, object);
					return;
				}
			}
			throw new NotSerializableException(type.getName());
		} finally {
			depth--;
		}
	}

	@Override
	public <T> T readObject(DataInput in, Class<T> type) throws IOException {
		if(depth == 0)
			graphContext.clear();
		depth++;
		try {
			for(@SuppressWarnings("rawtypes") Serializer s : serializers) {
				if(s.type() == type) {
					return type.cast(s.read(in));
				}
			}
			throw new NotSerializableException(type.getName());
		} finally {
			depth--;
		}
	}

	@Override
	public Map<Object, Object> context() {
		return context;
	}

	@Override
	public Map<Object, Object> graphContext() {
		return graphContext;
	}
	
}

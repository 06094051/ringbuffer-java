import java.io.*;
import java.nio.*;
import java.nio.channels.*;

// circular buffer / fifo
public class RingBuffer implements ByteChannel {
	static final int DEFAULT_CAPACITY;
	
	final int capacity;
	final ByteBuffer buffer;

	int head;
	int len;
	
	RingBuffer() {
		this( DEFAULT_CAPACITY );
	}

	RingBuffer( ByteBuffer buffer ) {
		this.buffer = buffer;
		capacity = buffer.capacity();
		init( this );
	}
	
	public RingBuffer( int capacity ) {
		this( ByteBuffer.allocateDirect( capacity ) );
	}

	static void init( RingBuffer rb ) {
		rb.head = 0;
		rb.len = 0;
	}

	static int tail( RingBuffer rb ) {
		int r;
		if ( 0 == rb.len || 0 == rb.capacity ) {
			r = rb.head;
		} else {
			r = ( rb.head + rb.len - 1 ) % rb.capacity;
		}
		return r;
	}
	
	public int peek( ByteBuffer dst ) throws IOException {
		int r;
		int t1;
		int t2;
		r = Math.min( this.len, dst.remaining() );
		if ( 0 != r ) {
			// XXX: must set position before reading, writing, etc
			if ( head + r >= capacity ) {
				t1 = capacity - head;
				buffer.position( head );
				buffer.limit( head + t1 );
				dst.put( buffer );
				t2 = r - t1;
				buffer.position( 0 );
				buffer.limit( 0 + t2 );
				dst.put( buffer );
			} else {
				buffer.position( head );
				buffer.limit( head + r );
				dst.put( buffer );
			}
		}
		return r;
	}

	@Override
	public int read( ByteBuffer dst ) throws IOException {
		int r;
		r = peek( dst );
		if ( r > 0 ) {
			skip( r );
		}
		return r;
	}

	@Override
	public int write( ByteBuffer src ) throws IOException {
		int r;
		int tail;
		int t1;
		int t2;
		r = Math.min( available(), src.remaining() );
		if ( 0 != r ) {
			tail = tail( this );
			// XXX: must set position before reading, writing, etc
			if ( tail + r >= capacity ) {
				t1 = capacity - tail;
				buffer.position( head );
				buffer.limit( head + t1 );
				src.limit( src.position() + t1 );
				buffer.put( src );
				t2 = r - t1;
				src.limit( src.position() + t2 );
				buffer.position( 0 );
				buffer.limit( 0 + t2 );
				buffer.put( src );
			} else {
				buffer.position( head );
				buffer.limit( head + r );
				buffer.put( src );
			}
		}
		this.len += r;
		return r;
	}

	public int send( RingBuffer in ) throws IOException {
		int r;

		@SuppressWarnings("resource")
		RingBuffer out = this;
		
		int orig_head_in;
		int orig_head_out;

		if ( null == in ) {
			throw new NullPointerException();
		}

		r = Math.min( in.size(), out.available() );
		if ( 0 == r ) {
			return 0;
		}

		orig_head_out = out.head;
		orig_head_in = in.head;

		// XXX: @CF: FIXME: Realigning both buffers simplifies the op, but is not optimized
		out.realign();
		in.realign();

		in.buffer.position( in.head );
		in.buffer.limit( in.head + r );
		out.write( in.buffer );
		
		// update output length
		// XXX: already done in write()
		//out.len += r;
		
		// XXX: @CF: FIXME: Realigning requires that both heads are put back where they came from
		arrayShift( out.buffer.array(), orig_head_out );
		out.head = orig_head_out;
		arrayShift( in.buffer.array(), orig_head_in );
		in.head = orig_head_in;

		// advance input by r
		in.skip( r );

		return r;
	}
	
	public int skip( int len ) {
		int r = -1;
		r = Math.min( this.len, len );
		if ( r > 0 ) {
			this.len -= r;
			head += r;
			if ( capacity > 0 ) {
				head %= capacity;
			}
		}
		return r;
	}

	public int size() {
		return len;
	}

	public int available() {
		return capacity - len;
	}

	public void reset() {
		init( this );
	}
	
	public void realign() {
		
		byte[] a;
		
		if ( 0 == head ) {
			return;
		}
		
		a = buffer.array();
		
		arrayShift( a, capacity - head );
		
		head = 0;
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void close() throws IOException {
	}

	static void arrayReverse( byte[] a, int start, int end ) {
		byte tmp;
		if ( start < 0 || end < 0 || start >= a.length || end >= a.length ) {
			throw new ArrayIndexOutOfBoundsException();
		}
		for( ; start < end; start++, end-- ) {
			tmp = a[ start ];
			a[ start ] = a[ end ];
			a[ end ] = tmp;
		}
	}

	static void arrayShift( byte[] a, int m ) {
		if ( null == a ) {
			throw new NullPointerException();
		}
		if ( 0 == a.length ) {
			return;
		}
		m %= a.length;
		if ( m < 0 ) {
			m += a.length;
		}
		if ( 0 == m ) {
			return;
		}
		arrayReverse( a, 0, a.length - 1 );
		arrayReverse( a, 0, m - 1 );
		arrayReverse( a, m, a.length - 1 );
	}
	
	static {
		DEFAULT_CAPACITY = 32;
	}
}

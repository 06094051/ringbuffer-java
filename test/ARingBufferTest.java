

import static org.junit.Assert.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import org.junit.*;

public abstract class ARingBufferTest {
	
	RingBuffer rb;
	
	static final int max_buf_len = 16;
	static final byte[] buf_template = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, };
	
	public ARingBufferTest() {
		byte[] templ = Arrays.copyOf( buf_template, size() );
		ByteBuffer bb = ByteBuffer.wrap( templ );
		rb = new RingBuffer( bb );
	}
	// size for a given test
	abstract int size();
	
	@Before
	public void reset() throws IOException {
		rb.reset();
	}
	
	// XXX: @CF: FIXME: probably can do less here, since there is partial duplication of the skip tests
	void _do_read_test( boolean peek, int expected_r ) throws IOException {

		final byte[] expected_val = buf_template;

		int actual_r;

		int expected_head;
		int actual_head;
		int original_head;

		int expected_len;
		int actual_len;
		ByteBuffer actual_val_bb;
		byte actual_val[];
		int actual_val_len;

		original_head = rb.head;

		actual_val_len = 0 == rb.len ? 1 : expected_r;
		actual_val_bb = ByteBuffer.allocate( actual_val_len );
		actual_val = actual_val_bb.array();
		
		if ( peek || 0 == expected_r ) {
			// for peek operations, the length and head MUST NOT CHANGE!!
			expected_len = rb.len;
			expected_head = rb.head;
		} else {
			// for read operations where >= 1 byte is read, the length and head MUST CHANGE!!
			expected_len = rb.len - expected_r;
			expected_head = rb.head + expected_r;
			if ( 0 != rb.capacity ) {
				 expected_head %= rb.capacity;
			}
		}

		if ( peek ) {
			actual_r = rb.peek( actual_val_bb );
		} else {
			actual_r = rb.read( actual_val_bb );
		}
		actual_len = rb.len;
		actual_head = rb.head;

		assertEquals( "did not read the expected number of bytes " + expected_r, expected_r, actual_r );
		assertEquals( "after read, did not have the expected_len" + expected_len, expected_len, actual_len );
		assertEquals( "after read, did not have the expected_head" + expected_head, expected_head, actual_head );
		if ( actual_r > 0 ) {
			// verify data
			assertArrayEquals( "", Arrays.copyOfRange( expected_val, original_head, expected_r ), Arrays.copyOfRange( actual_val, 0, actual_r ) );
		}

	}
	void do_peek_test( int expected_r ) throws IOException {
		_do_read_test( true, expected_r );
	}
	void do_read_test( int expected_r ) throws IOException {
		_do_read_test( false, expected_r );
	}
	
	//
	// Tests for RingBuffer.peek()
	//
	
	// try to peek at 0 elements
	@Test
	public void peekEmpty() throws IOException {
		int expected_r = 0;
		do_peek_test( expected_r );
	}
	// peek at the 0th element
	@Test
	public void peekOne() throws IOException {
		rb.len = rb.capacity;
		int expected_r = Math.min( 1, rb.len );
		do_peek_test( expected_r );
	}
	// peek at all of the elements starting from the 0th
	@Test
	public void peekAll() throws IOException {
		rb.len = rb.capacity;
		int expected_r = rb.len;
		do_peek_test( expected_r );
	}
	// peek at all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void peekWrap() throws IOException {
		rb.len = rb.capacity / 2;
		int expected_r = rb.len;
		do_peek_test( expected_r );
	}
	// request to peek at len <= n <= cap elements
	@Test
	public void peekMoreThenLen() throws IOException {
		// not possible if using ByteBuffer instead of array + len
	}
	// request to peek at len <= cap <= n elements
	@Test
	public void peekMoreThenCap() throws IOException {
		// not possible if using ByteBuffer instead of array + len
	}

	//
	// Tests for RingBuffer.read()
	//
	
	// try to read at 0 elements
	@Test
	public void readEmpty() throws IOException {
		int expected_r = 0;
		do_read_test( expected_r );
	}
	// read at the 0th element
	@Test
	public void readOne() throws IOException {
		rb.len = rb.capacity;
		int expected_r = Math.min( 1, rb.len );
		do_read_test( expected_r );
	}
	// read at all of the elements starting from the 0th
	@Test
	public void readAll() throws IOException {
		rb.len = rb.capacity;
		int expected_r = rb.len;
		do_read_test( expected_r );
	}
	// read at all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void readWrap() throws IOException {
		rb.len = rb.capacity / 2;
		int expected_r = rb.len;
		do_read_test( expected_r );
	}
	// request to read at len <= n <= cap elements
	@Test
	public void readMoreThenLen() throws IOException {
		// not possible if using ByteBuffer instead of array + len
	}
	// request to read at len <= cap <= n elements
	@Test
	public void readMoreThenCap() throws IOException {
		// not possible if using ByteBuffer instead of array + len
	}
	
	//
	// Tests for RingBuffer.write()
	//

	void do_write_test( int expected_r ) throws IOException {
		// all zeros
		final byte[] expected_val = new byte[ max_buf_len ];

		int actual_r;

		int expected_head;
		int actual_head;

		int expected_len;
		int actual_len;
		
		
		byte[] actual_val;
		ByteBuffer actual_val_bb;
		ByteBuffer expected_val_bb;
		int expected_val_len;

		expected_head = rb.head;

		expected_val_len = Math.min( expected_r, rb.available() );
		//expected_val_len = 0 == expected_val_len ? 1 : expected_val_len;
		expected_len = rb.len + expected_r;

		expected_val_bb = ByteBuffer.wrap( expected_val, 0, expected_val_len );
		
		actual_r = rb.write( expected_val_bb );

		actual_val_bb = rb.buffer;
		actual_val = actual_val_bb.array();
		
		actual_len = rb.len;
		actual_head = rb.head;

		assertEquals( "return value is not as expected", expected_r, actual_r );
		assertEquals( "length is not as expected", expected_len, actual_len );
		assertEquals( "head is not as expected", expected_head, actual_head );
		if ( actual_r > 0 ) {
			// verify data			
			assertArrayEquals( "arrays differ", Arrays.copyOfRange( expected_val, 0, expected_r ), Arrays.copyOfRange( actual_val, 0, actual_r ) );
		}
	}

	// try to write 0 elements
	@Test
	public void writeEmpty() throws IOException {
		final int expected_r = 0;
		do_write_test( expected_r );
	}

	// write the 0th element
	@Test
	public void writeOne () throws IOException {
		int expected_r;
		int avail;
		rb.len = 0;
		avail = rb.available();
		expected_r = Math.min( 1, avail );
		do_write_test( expected_r );
	}

	// write all of the elements starting from the 0th
	@Test
	public void writeAll () throws IOException {
		rb.len = 0;
		int expected_r = rb.capacity;
		do_write_test( expected_r );
	}

	// write all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void writeWrap () throws IOException {
		rb.len = 0;
		rb.head = rb.capacity / 2;
		int expected_r = rb.capacity;
		do_write_test( expected_r );
	}

	// write len <= n <= cap elements
	@Test
	public void writeMoreThanAvail () {
		// not possible if using ByteBuffer instead of array + len
	}

	// write len <= cap <= n elements
	@Test
	public void writeMoreThanCap () {
		// not possible if using ByteBuffer instead of array + len
	}

	//
	// Tests for RingBuffer.send()
	//

	@Test
	public void sendSome() throws IOException {

		byte[] buf;
		RingBuffer in;
		RingBuffer out;

		ByteBuffer bb = ByteBuffer.allocate( buf_template.length );
		buf = bb.array();
		System.arraycopy( buf_template, 0, buf, 0, buf.length );

		in = new RingBuffer( bb );
		in.len = in.capacity / 2;

		int expected_head_out;
		int actual_head_out;
		int expected_head_in;
		int actual_head_in;

		int expected_len_out;
		int actual_len_out;
		int expected_len_in;
		int actual_len_in;

		int expected_r;
		int actual_r;

		in.reset();
		in.len = in.capacity / 2;
		in.head = in.capacity / 4;

		out = rb;

		out.len = Math.min( 2, out.capacity );
		out.head = out.capacity / 2;

		expected_r = Math.min( out.available(), in.size() );

		expected_head_out = out.head;
		expected_head_in = 0 == in.capacity ? in.head : ( in.head + expected_r ) % in.capacity;

		expected_len_out = out.len + expected_r;
		expected_len_in = in.len - expected_r;

		actual_r = out.send( in );

		actual_head_out = out.head;
		actual_head_in = in.head;

		actual_len_out = out.len;
		actual_len_in = in.len;

		assertEquals( "return value not as expected", expected_r, actual_r );

		assertEquals( "output head not as expected", expected_head_out, actual_head_out );
		assertEquals( "input head not as expected", expected_head_in, actual_head_in );

		assertEquals( "output len not as expected", expected_len_out, actual_len_out );
		assertEquals( "input len not as expected", expected_len_in, actual_len_in );
	}

	//
	// Tests for RingBuffer.skip()
	//

	void do_skip_test( int requested_skip ) {
		int expected_r;
		int actual_r;
		int expected_head;
		int actual_head;
		int expected_len;
		int actual_len;

		expected_r = Math.min( requested_skip, rb.len );
		expected_head = rb.head + expected_r;
		if ( rb.capacity > 0 ) {
			expected_head %= rb.capacity;
		}
		expected_len = rb.len - expected_r;

		actual_r = rb.skip( requested_skip );
		actual_head = rb.head;
		actual_len = rb.len;

		assertEquals( "return value not as expected", expected_r, actual_r );
		assertEquals( "head not as expected", expected_head, actual_head );
		assertEquals( "len not as expected", expected_len, actual_len );
	}

	// skip zero elements
	@Test
	public void skipZero () {
		final int requested_skip = 0;
		rb.len = 0;
		do_skip_test( requested_skip );
	}

	// skip one element
	@Test
	public void skipOne () {
		final int requested_skip = 1;
		rb.len = rb.capacity;
		do_skip_test( requested_skip );
	}

	// skip all elements starting from the 0th
	@Test
	public void skipAll () {
		int requested_skip;
		rb.len = rb.capacity;
		requested_skip = rb.len;
		do_skip_test( requested_skip );
	}

	// skip at all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void skipWrap () {
		int requested_skip;
		rb.len = rb.capacity;
		rb.head = rb.len / 2;
		requested_skip = rb.len;
		do_skip_test( requested_skip );
	}

	// skip len <= n <= cap elements
	@Test
	public void skipMoreThanLen () {
		int requested_skip;
		rb.len = rb.capacity / 2;
		requested_skip = rb.len + 1;
		do_skip_test( requested_skip );
	}

	// skip len <= cap <= n elements
	@Test
	public void skipMoreThanCap () {
		int requested_skip;
		rb.len = rb.capacity / 2;
		requested_skip = rb.capacity + 1;
		do_skip_test( requested_skip );
	}	
	
	//
	// Tests for RingBuffer.size()
	//
	
	void do_size_test( int expected_size ) {
		int actual_size;

		actual_size = rb.size();

		assertEquals( "size not as expected", expected_size, actual_size );
	}

	// size zero elements
	@Test
	public void sizeZero () {
		final int expected_size = 0;
		rb.len = expected_size;
		do_size_test( expected_size );
	}

	// size one element
	@Test
	public void sizeOne () {
		final int expected_size = 1;
		rb.len = expected_size;
		do_size_test( expected_size );
	}

	// size of all elements starting from the 0th
	@Test
	public void sizeAll () {
		int expected_size;
		rb.len = rb.capacity;
		expected_size = rb.len;
		do_size_test( expected_size );
	}

	// size of all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void sizeWrap () {
		int expected_size;
		rb.len = rb.capacity;
		rb.head = rb.len / 2;
		expected_size = rb.len;
		do_size_test( expected_size );
	}
	
	//
	// Tests for RingBuffer.available()
	//

	void do_available_test( int size ) {
		int actual_available;
		int expected_available;

		expected_available = rb.capacity - size;

		actual_available = rb.available();

		assertEquals( "available not as expected", expected_available, actual_available );
	}

	// avaiability when size is zero elements
	@Test
	public void availableZero () {
		final int size = 0;
		rb.len = size;
		do_available_test( size );
	}

	// avaiability when size is one element
	@Test
	public void availableOne () {
		int size;
		size = Math.min( 1, rb.capacity );
		rb.len = size;
		do_available_test( size );
	}

	// avaiability when size is all elements starting from the 0th
	@Test
	public void availableAll () {
		int size;
		rb.len = rb.capacity;
		size = rb.len;
		do_available_test( size );
	}

	// avaiability when size is all elements (head of buffer is in the middle rather than at 0)
	@Test
	public void availableWrap () {
		int size;
		rb.len = rb.capacity;
		rb.head = rb.len / 2;
		size = rb.len;
		do_available_test( size );
	}
	
	//
	// Tests for RingBuffer.reset()
	//

	@Test
	public void resetNonEmpty() {

		final int expected_head = 0;
		final int expected_len = 0;

		int actual_head;
		int actual_len;

		rb.len = rb.capacity;
		rb.head = rb.capacity / 2;

		rb.reset();

		actual_head = rb.head;
		actual_len = rb.len;

		assertEquals( "head not as expected", expected_head, actual_head );
		assertEquals( "len not as expected", expected_len, actual_len );
	}


	//
	// Tests for RingBuffer.realign()
	//
	
	void do_realign_test( int head ) {
		
		final int expected_head = 0;
	
		int original_head;	
		int actual_head;

		int expected_len;
		int actual_len;

		byte[] expected_val;
		byte[] actual_val;

		rb.len = rb.capacity;	
		rb.head = head;
		
		expected_len = rb.len;
		
		original_head = rb.head;
		
		expected_val = Arrays.copyOf( buf_template, rb.capacity );
		RingBuffer.arrayShift( expected_val, rb.capacity - original_head );
	
		rb.realign();
	
		actual_head = rb.head;
		actual_len = rb.len;
	
		assertEquals( "head not as expected", expected_head, actual_head );
		assertEquals( "len not as expected", expected_len, actual_len );
	
		actual_val = rb.buffer.array();
	
		if ( rb.len > 0 ) {
			assertArrayEquals( "backing storage not as expected", expected_val, actual_val );
		}
	}
	
	// realign an already-realigned buffer
	@Test
	public void realignZero() {
		do_realign_test( 0 );
	}
	
	// realign a buffer with the head half-way
	@Test
	public void realignHalf () {
		do_realign_test( rb.capacity / 2 );
	}

}

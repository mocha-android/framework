/**
 *  @author Shaun
 *  @date 4/22/14
 *  @copyright 2014 Mocha. All rights reserved.
 */
package mocha.foundation;

import android.net.Uri;
import mocha.ui.Application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class Data extends MObject implements Copying<Data> {

	private byte[] bytes;

	public Data() { }

	public Data(byte[] bytes) {
		this.bytes = bytes;
	}

	public Data(Data data) {
		this(data.bytes);
	}

	public static Data withContentsOfURL(URL url) throws IOException {
		InputStream inputStream = null;
		IOException exception = null;
		Data data = null;

		try {
			inputStream = url.openStream();
			data = new Data(inputStream);
		} catch (IOException e) {
			exception = e;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) { }
			}
		}

		if(exception != null) {
			throw exception;
		} else {
			return data;
		}
	}

	public Data(InputStream inputStream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			byte[] byteChunk = new byte[4096];
			int n;

			while ((n = inputStream.read(byteChunk)) > 0) {
				outputStream.write(byteChunk, 0, n);
			}

			this.bytes = outputStream.toByteArray();
			outputStream.close();
		} catch (IOException exception) {
			outputStream.close();
			throw exception;
		}
	}

	public int length() {
		return this.bytes == null ? 0 : this.bytes.length;
	}

	public byte[] getBytes() {
		return this.bytes;
	}

	public int hashCode() {
		if(this.bytes != null) {
			return Arrays.hashCode(this.bytes);
		} else {
			return 0;
		}
	}

	public boolean equals(Object o) {
		if(this == o) {
			return true;
		} else if(o instanceof Data) {
			Data data = (Data)o;
			return this.bytes == data.bytes || this.hashCode() == data.hashCode();
		} else {
			return false;
		}
	}

	public Data copy() {
		return new Data(this);
	}

}

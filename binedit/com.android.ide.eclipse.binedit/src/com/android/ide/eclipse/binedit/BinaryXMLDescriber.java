package com.android.ide.eclipse.binedit;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryXMLDescriber implements IContentDescriber {
	
	private final static short RES_XML_TYPE = 0x0003;

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.content.IContentDescriber#describe(java.io.InputStream, org.eclipse.core.runtime.content.IContentDescription)
	 */
	@Override
	public int describe(InputStream contents, IContentDescription description)
			throws IOException {
		int length = 8;
		int isSize = contents.available();
		byte[] bytes = new byte[length];
		if (contents.read(bytes , 0, length) != length) {
			return INVALID;
		}
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		short type = buf.getShort();
		buf.getShort(); // move position
		int size = buf.getInt();
		if (type == RES_XML_TYPE && isSize >= size ) {
			return VALID;
		}
		return INVALID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.content.IContentDescriber#getSupportedOptions()
	 */
	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

}

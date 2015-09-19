/*******************************************************************************
 * Copyright (c) 2000, 2010 SnPe Informacioni Sistemi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SnPe Informacioni sistemi - initial API and implementation
 *******************************************************************************/
package rs.snpe.android.refactoring.changes;


/**
 * @author Sara
 *
 */
public class AndroidLayoutChangeDescription {
	
	private String className;
	private String newName;
	private int type;
	public static final int VIEW_TYPE = 0;
	public static final int STANDALONE_TYPE = 1;
	/**
	 * @param className
	 * @param newName
	 */
	public AndroidLayoutChangeDescription(String className,
			String newName, int type) {
		this.className = className;
		this.newName = newName;
		this.type = type;
	}

	public String getClassName() {
		return className;
	}

	public String getNewName() {
		return newName;
	}

	public int getType() {
		return type;
	}

	public boolean isStandalone() {
		return type == STANDALONE_TYPE;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((newName == null) ? 0 : newName.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AndroidLayoutChangeDescription other = (AndroidLayoutChangeDescription) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (newName == null) {
			if (other.newName != null)
				return false;
		} else if (!newName.equals(other.newName))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AndroidLayoutChangeDescription [className=" + className
				+ ", newName=" + newName + ", type=" + type + "]";
	}

}

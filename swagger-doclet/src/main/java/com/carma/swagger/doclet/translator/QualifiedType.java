package com.carma.swagger.doclet.translator;

import com.sun.javadoc.Type;

/**
 * The QualifiedType represents a type with a qualifier.
 * @version $Id$
 * @author conor.roche
 */
public class QualifiedType {

	private final String qualifier;
	private final Type type;
	private final String typeName;

	/**
	 * This creates a QualifiedType
	 * @param qualifier
	 * @param type
	 */
	public QualifiedType(String qualifier, Type type) {
		super();
		this.qualifier = qualifier;
		this.type = type;
		this.typeName = type == null ? null : this.type.qualifiedTypeName();
	}

	/**
	 * This creates a QualifiedType
	 * @param type
	 */
	public QualifiedType(Type type) {
		this(null, type);
	}

	/**
	 * This gets the qualifier
	 * @return the qualifier
	 */
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * This gets the type
	 * @return the type
	 */
	public Type getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		QualifiedType that = (QualifiedType) o;

		if (qualifier != null ? !qualifier.equals(that.qualifier) : that.qualifier != null) return false;
		if (type != null ? !type.equals(that.type) : that.type != null) return false;
		return !(typeName != null ? !typeName.equals(that.typeName) : that.typeName != null);

	}

	@Override
	public int hashCode() {
		int result = qualifier != null ? qualifier.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (typeName != null ? typeName.hashCode() : 0);
		return result;
	}
}

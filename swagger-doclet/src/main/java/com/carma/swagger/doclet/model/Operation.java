package com.carma.swagger.doclet.model;

import static com.google.common.base.Strings.emptyToNull;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class Operation {

	private HttpMethod method;
	private String nickname;
	private String type; // void, primitive, complex or a container
	private String format; // format for primitives
	private PropertyItems items;

	private List<ApiParameter> parameters;
	private String summary; // cap at 60 characters for readability in the UI
	private String notes;

	private List<ApiResponseMessage> responseMessages;

	private List<String> consumes;
	private List<String> produces;

	private OperationAuthorizations authorizations;

	private String deprecated;

	@SuppressWarnings("unused")
	private Operation() {
	}

	private Operation(Operation o) {
		this.method = o.method;
		this.nickname = o.nickname;
		this.type = o.type;
		this.format = o.format;
		this.items = o.items;
		this.parameters = o.parameters;
		this.responseMessages = o.responseMessages;
		this.summary = o.summary;
		this.notes = o.notes;
		this.consumes = o.consumes;
		this.produces = o.produces;
		this.authorizations = o.authorizations;
		this.deprecated = o.deprecated;
	}

	public Operation(Method method) {
		this.method = method.getMethod();
		this.nickname = emptyToNull(method.getMethodName());
		this.type = emptyToNull(method.getReturnType());
		this.format = emptyToNull(method.getReturnTypeFormat());
		if (method.getReturnTypeItemsRef() != null || method.getReturnTypeItemsType() != null) {
			this.items = new PropertyItems(method.getReturnTypeItemsRef(), method.getReturnTypeItemsType(), method.getReturnTypeItemsFormat(), method.getReturnTypeItemsAllowableValues());
		}
		this.parameters = method.getParameters().isEmpty() ? null : ImmutableList.copyOf(method.getParameters());
		this.responseMessages = method.getResponseMessages().isEmpty() ? null : ImmutableList.copyOf(method.getResponseMessages());
		this.summary = emptyToNull(method.getSummary());
		this.notes = emptyToNull(method.getNotes());
		this.consumes = method.getConsumes() == null || method.getConsumes().isEmpty() ? null : ImmutableList.copyOf(method.getConsumes());
		this.produces = method.getProduces() == null || method.getProduces().isEmpty() ? null : ImmutableList.copyOf(method.getProduces());
		this.authorizations = method.getAuthorizations();

		if (method.isDeprecated()) {
			this.deprecated = "true";
		}
	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getNickname() {
		return this.nickname;
	}

	public String getType() {
		return this.type;
	}

	/**
	 * This gets the format
	 * @return the format
	 */
	public String getFormat() {
		return this.format;
	}

	/**
	 * This gets the items
	 * @return the items
	 */
	public PropertyItems getItems() {
		return this.items;
	}

	public List<ApiParameter> getParameters() {
		return this.parameters;
	}

	public List<ApiResponseMessage> getResponseMessages() {
		return this.responseMessages;
	}

	public String getSummary() {
		return this.summary;
	}

	public String getNotes() {
		return this.notes;
	}

	/**
	 * This gets the consumes
	 * @return the consumes
	 */
	public List<String> getConsumes() {
		return this.consumes;
	}

	/**
	 * Returns a new Operation with the given consumes. This object itself will not be changed.
	 * @param consumes
	 *            consumes to set
	 * @return new Operations
	 */
	public Operation consumes(Collection<String> consumes) {
		Operation clone = new Operation(this);
		clone.consumes = consumes == null || consumes.isEmpty() ? null : ImmutableList.copyOf(consumes);
		return clone;
	}

	/**
	 * This gets the produces
	 * @return the produces
	 */
	public List<String> getProduces() {
		return this.produces;
	}

	/**
	 * Returns a new Operation with the given produces. This object itself will not be changed.
	 * @param produces
	 *            produces to set
	 * @return new Operations
	 */
	public Operation produces(Collection<String> produces) {
		Operation clone = new Operation(this);
		clone.produces = produces == null || produces.isEmpty() ? null : ImmutableList.copyOf(produces);
		return clone;
	}

	/**
	 * This gets the authorizations
	 * @return the authorizations
	 */
	public OperationAuthorizations getAuthorizations() {
		return this.authorizations;
	}

	/**
	 * This gets the deprecated
	 * @return the deprecated
	 */
	public String getDeprecated() {
		return this.deprecated;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.authorizations == null) ? 0 : this.authorizations.hashCode());
		result = prime * result + ((this.consumes == null) ? 0 : this.consumes.hashCode());
		result = prime * result + ((this.deprecated == null) ? 0 : this.deprecated.hashCode());
		result = prime * result + ((this.format == null) ? 0 : this.format.hashCode());
		result = prime * result + ((this.items == null) ? 0 : this.items.hashCode());
		result = prime * result + ((this.method == null) ? 0 : this.method.hashCode());
		result = prime * result + ((this.nickname == null) ? 0 : this.nickname.hashCode());
		result = prime * result + ((this.notes == null) ? 0 : this.notes.hashCode());
		result = prime * result + ((this.parameters == null) ? 0 : this.parameters.hashCode());
		result = prime * result + ((this.produces == null) ? 0 : this.produces.hashCode());
		result = prime * result + ((this.responseMessages == null) ? 0 : this.responseMessages.hashCode());
		result = prime * result + ((this.summary == null) ? 0 : this.summary.hashCode());
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Operation other = (Operation) obj;
		if (this.authorizations == null) {
			if (other.authorizations != null) {
				return false;
			}
		} else if (!this.authorizations.equals(other.authorizations)) {
			return false;
		}
		if (this.consumes == null) {
			if (other.consumes != null) {
				return false;
			}
		} else if (!this.consumes.equals(other.consumes)) {
			return false;
		}
		if (this.deprecated == null) {
			if (other.deprecated != null) {
				return false;
			}
		} else if (!this.deprecated.equals(other.deprecated)) {
			return false;
		}
		if (this.format == null) {
			if (other.format != null) {
				return false;
			}
		} else if (!this.format.equals(other.format)) {
			return false;
		}
		if (this.items == null) {
			if (other.items != null) {
				return false;
			}
		} else if (!this.items.equals(other.items)) {
			return false;
		}
		if (this.method != other.method) {
			return false;
		}
		if (this.nickname == null) {
			if (other.nickname != null) {
				return false;
			}
		} else if (!this.nickname.equals(other.nickname)) {
			return false;
		}
		if (this.notes == null) {
			if (other.notes != null) {
				return false;
			}
		} else if (!this.notes.equals(other.notes)) {
			return false;
		}
		if (this.parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!this.parameters.equals(other.parameters)) {
			return false;
		}
		if (this.produces == null) {
			if (other.produces != null) {
				return false;
			}
		} else if (!this.produces.equals(other.produces)) {
			return false;
		}
		if (this.responseMessages == null) {
			if (other.responseMessages != null) {
				return false;
			}
		} else if (!this.responseMessages.equals(other.responseMessages)) {
			return false;
		}
		if (this.summary == null) {
			if (other.summary != null) {
				return false;
			}
		} else if (!this.summary.equals(other.summary)) {
			return false;
		}
		if (this.type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!this.type.equals(other.type)) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Operation [method=" + this.method + ", nickname=" + this.nickname + ", type=" + this.type + ", format=" + this.format + ", items=" + this.items
				+ ", parameters=" + this.parameters + ", summary=" + this.summary + ", notes=" + this.notes + ", responseMessages=" + this.responseMessages
				+ ", consumes=" + this.consumes + ", produces=" + this.produces + ", authorizations=" + this.authorizations + ", deprecated=" + this.deprecated
				+ "]";
	}

}

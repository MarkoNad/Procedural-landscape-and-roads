package hr.fer.zemris.engine.model;

import java.util.Arrays;

public class TexturedModelComp {
	
	public TexturedModel[] children;

	public TexturedModelComp(TexturedModel... children) {
		this.children = children;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(children);
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
		TexturedModelComp other = (TexturedModelComp) obj;
		if (!Arrays.equals(children, other.children))
			return false;
		return true;
	}

}

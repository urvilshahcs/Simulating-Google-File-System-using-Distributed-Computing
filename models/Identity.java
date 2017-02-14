package edu.utdallas.cs6378.models;

import java.io.Serializable;

/**
 * This class is an Identity model that is used to identify itself to the
 * connection accepting node with an instance of this class which has two
 * attributes name and function.
 * 
 */

public class Identity implements Serializable{
	
	private static final long serialVersionUID = 6597773221623501215L;
	private String name;
	private String function;
	
	public Identity(String name, String function) {
		this.name = name;
		this.function = function;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getFunction() {
		return this.function;
	}
	
}

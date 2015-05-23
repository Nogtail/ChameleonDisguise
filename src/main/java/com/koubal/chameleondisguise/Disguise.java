package com.koubal.chameleondisguise;

public class Disguise {
	private final String tag;
	private final String textures;

	public Disguise(String tag, String textures) {
		this.tag = tag;
		this.textures = textures;
	}

	public String getTag() {
		return tag;
	}

	public String getTextures() {
		return textures;
	}
}

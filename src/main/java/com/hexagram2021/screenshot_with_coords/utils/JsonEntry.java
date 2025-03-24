package com.hexagram2021.screenshot_with_coords.utils;

public record JsonEntry(String filename, double x, double y, double z, float yaw, float pitch) {
	public String toJson() {
		return "{\"filename\": \"%s\", \"x\": %f, \"y\": %f, \"z\": %f, \"yaw\": %f, \"pitch\": %f}"
				.formatted(this.filename, this.x, this.y, this.z, this.yaw, this.pitch);
	}
}

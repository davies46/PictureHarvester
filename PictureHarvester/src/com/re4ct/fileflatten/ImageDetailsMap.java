package com.re4ct.fileflatten;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ImageDetailsMap {
	HashMap<Long, Set<ImageDetails>> map;

	public ImageDetailsMap() {
		map = new HashMap<>(16384);
	}

	public Set<ImageDetails> get(long digest) {
		return map.get(digest);
	}

	public boolean put(long digest, ImageDetails imageDetails) {
		Set<ImageDetails> existing = map.get(digest);
		if (existing == null) {
			existing = new HashSet<>();
			map.put(digest, existing);
		}
		return existing.add(imageDetails);
	}

	public boolean has(long digest) {
		return map.containsKey(digest);
	}

	public int size() {
		return map.size();
	}

	public Collection<Set<ImageDetails>> values() {
		// TODO Auto-generated method stub
		return map.values();
	}

}

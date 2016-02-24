package com.iise.shawn.ssd;

@SuppressWarnings("rawtypes")
public class Pair<K, Double extends Comparable> implements Comparable<Pair<K, Double>>{
	public K key;
	public Double value;
	
	public Pair(K _key, Double _value) {
		this.key = _key;
		this.value = _value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Pair<K, Double> o) {
		// TODO Auto-generated method stub
		if ((this.value.compareTo(0.0) < 0) && (o.value.compareTo(0.0) >= 0)) {
			return 1;
		}
		else if ((this.value.compareTo(0.0) < 0) && (o.value.compareTo(0.0) < 0)) {
			return this.value.compareTo(o.value);
		}
		else if ((this.value.compareTo(0.0) >= 0) && (o.value.compareTo(0.0) < 0)) {
			return -1;
		}
		return this.value.compareTo(o.value);
	}
}
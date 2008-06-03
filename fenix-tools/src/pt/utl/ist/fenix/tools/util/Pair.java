package pt.utl.ist.fenix.tools.util;

import java.io.Serializable;

/**
 * A pair is simple aggregation of two values. This class can be used to keep
 * two values together, like a key and value, without depending in any
 * particular data structure.
 * 
 * @author <a href="mailto:goncalo@ist.utl.pt">Goncalo Luiz</a> <br/> <br/>
 *         <br/> Created on 13:27:42,31/Mar/2006
 * @version $Id: Pair.java 35158 2008-04-07 15:23:35Z nmgo $
 */
public class Pair<K, V> implements Serializable {

    private K key;
    private V value;

    public Pair(K key, V value) {
	this.key = key;
	this.value = value;
    }

    public K getKey() {
	return key;
    }

    public V getValue() {
	return value;
    }

    @Override
    public String toString() {
	return "Pair(" + getKey() + ", " + getValue() + ")";
    }

}

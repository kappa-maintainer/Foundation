
package top.outlands.foundation.trie;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * a data structure for prefix trie
 * @author Lin Chi-Min (v381654729@gmail.com)
 * 
 * @param <V> a generic type 
 */
public class PrefixTrie<V> extends AbstractTrie<V> {

	/**
	 * constructor for an empty trie
	 */
	public PrefixTrie() {
		super();
	}

	/**
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public PrefixTrie(List<String> keys, List<V> values) {
		super(keys, values);
	}
	
	/**
	 * constructor for constructing a trie with the keys and values and scores;
	 * the scores are used for TrieNode comparison 
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 * @param scores: the scores of each of the key-value pairs 
	 */
	public PrefixTrie(List<String> keys, List<V> values, int[] scores){
		super(keys, values, scores);
	}
	
	/**
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public PrefixTrie(String[] keys, V[] values) {
		super(Arrays.asList(keys), Arrays.asList(values));
	}
	
	/**
	 * constructor for constructing a trie with the keys and values and scores;
	 * the scores are used for TrieNode comparison 
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 * @param scores: the scores of each of the key-value pairs 
	 */
	public PrefixTrie(String[] keys, V[] values, int[] scores) {
		super(Arrays.asList(keys), Arrays.asList(values), scores);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean put(String key, V value, int score) {
		
		TrieNode<V> node = root;
		char[] chars = key.toCharArray();
		int[] indices = lookupIndices(chars);
		if (indices == null){
			// not allowed to add this word if one of the chars is unsupported
			return false;
		}
		
		int level = 0;
		for (int i = 0; i < chars.length; i++) {
			level++;
			int index = indices[i];
			if (node.children[index] == null) {
				TrieNode<V> temp = new TrieNode<>(chars[i], level);
				node.addChildIndex(index);
				node.children[index] = temp;
				temp.parent = node;
				if (node.level + 1 != temp.level) {
					throw new RuntimeException("PrefixTrie: Bugs occurred: "
							+ "node.level + 1 should be equal to temp.level, while node.level = " + 
							node.level + ", temp.level = " + temp.level);
				}
				node = temp;
			} else {
				node = node.children[index];
			}
			if (node.level < 0) {
				throw new RuntimeException("PrefixTrie: Bugs occurred: "
						+ "node.level should be nonnegative, while node.level = " + node.level);
			}
		}
		
		if (!node.isKeyValueNode){
			size++;
		}
		node.isKeyValueNode = true;
		node.value = value;
		node.score = score;
		return true;
	}
	
	
	/**
	 * 1. For input word "abcde", 
	 * if the node that has the longest common prefix with level &lt;= maxPrefixLength is "abc3", 
	 * return node 'c'
	 * 
	 * 2. Equivalent to getNodeWithLongestCommonPart(word.substring(0, maxPrefixLength))
	 *  
	 * @param word : a word
	 * @param maxPrefixLength : as described above
	 * @return the node that has the longest common prefix with word
	 */
	@Override
	protected TrieNode<V> getNodeWithLongestCommonPart(String word, int maxPrefixLength) {
		if (maxPrefixLength < 0) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxPrefixLength' (" + maxPrefixLength + ") should be non-negative.");
		} else if (maxPrefixLength > word.length()) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxPrefixLength' (" + maxPrefixLength + ") should not be larger than word.length().");
		}
		
		TrieNode<V> node = root;
		for (int i = 0; i < maxPrefixLength; i++) {
			int index = charToIndex(word.charAt(i));
			if (index >= 0 && node.children[index] != null) {
				node = node.children[index];
			} else {
				break;
			}
		}
		return node;
	}
	
	public TrieNode<V> getFirstKeyValueNode(String key) {
		TrieNode<V> node = root;
		for (int i = 0; i < key.length(); i++) {
			if (node.isKeyValueNode) {
				return node;
			}
			int index = charToIndex(key.charAt(i));
			if (index >= 0 && node.children[index] != null) {
				node = node.children[index];
			} else {
				return null;
			}
		}
		if (node.isKeyValueNode) {
			return node;
		}
		return null;
	}
	
	
	

}

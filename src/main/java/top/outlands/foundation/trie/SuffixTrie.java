package top.outlands.foundation.trie;

import java.util.Arrays;
import java.util.List;


/**
 * a data structure for suffix trie
 * 
 * @author Lin Chi-Min (v381654729@gmail.com)
 * 
 * @param <V> a generic type 
 */
public class SuffixTrie<V> extends AbstractTrie<V> {
	
	

	/**
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public SuffixTrie(List<String> keys, List<V> values) {
		super(keys, values);
	}
	
	/**
	 * constructor for constructing a trie with the keys and values and scores;
	 * the scores are used for TrieNode comparison 
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 * @param scores: the scores of each of the key-value pairs 
	 */
	public SuffixTrie(List<String> keys, List<V> values, int[] scores){
		super(keys, values, scores);
	}
	
	
	/**
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public SuffixTrie(String[] keys, V[] values) {
		super(Arrays.asList(keys), Arrays.asList(values));
	}
	
	/**
	 * constructor for constructing a trie with the keys and values and scores;
	 * the scores are used for TrieNode comparison 
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 * @param scores: the scores of each of the key-value pairs 
	 */
	public SuffixTrie(String[] keys, V[] values, int[] scores) {
		super(Arrays.asList(keys), Arrays.asList(values), scores);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean put(String word, V value, int score) {
		
		TrieNode<V> node = root;
		
		char[] chars = word.toCharArray();
		int[] indices = lookupIndices(chars);
		if (indices == null){
			// not allowed to add this word if one of the chars is unsupported
			return false;
		}
		
		int level = 0;
		for (int i = chars.length - 1; i >= 0; i--) {
			level++;
			int index = indices[i];
			if (node.children[index] == null) {
				TrieNode<V> temp = new TrieNode<V>(chars[i], level);
				node.addChildIndex(index);
				node.children[index] = temp;
				temp.parent = node;
				if (node.level + 1 != temp.level) {
					throw new RuntimeException("SuffixTrie: Bugs occurred: "
							+ "node.level + 1 should be equal to temp.level, while node.level = " + 
							node.level + ", temp.level = " + temp.level);
				}
				node = temp;
			} else {
				node = node.children[index];
			}
			if (node.level < 0) {
				throw new RuntimeException("SuffixTrie: Bugs occurred: "
						+ "node.level should be nonnegative, while node.level = " + node.level);
			}
		}
		
		if (node.isKeyValueNode == false){
			size++;
		}
		node.isKeyValueNode = true;
		node.value = value;
		node.score = score;
		return true;
	}
	

	
	
	/**
	 *
	 * 1. For input word "abcde", 
	 * if the node that has the longest common suffix with level &lt;= maxSuffixLength is "r4de", 
	 * return node 'd'
	 * 
	 * 2. Equivalent to getNodeWithLongestCommonPart(word.substring(word.length() - maxSuffixLength, word.length()))
	 *  
	 * @param word : a word
	 * @param maxSuffixLength : as described above
	 * @return the node that has the longest common prefix with word
	 */
	@Override
	protected TrieNode<V> getNodeWithLongestCommonPart(String word, int maxSuffixLength) {
		
		if (maxSuffixLength < 0) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxSuffixLength' (" + maxSuffixLength + ") should be non-negative.");
		} else if (maxSuffixLength > word.length()) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxSuffixLength' (" + maxSuffixLength + ") should not be larger than word.length().");
		}
		
		TrieNode<V> node = root;
		int start = word.length() - 1, end = word.length() - maxSuffixLength;
		for (int i = start; i >= end; i--) {
			int index = charToIndex(word.charAt(i));
			if (index >= 0 && node.children[index] != null) {
				node = node.children[index];
			} else {
				break;
			}
		}
		return node;
	}
	
	
	
	

	
}
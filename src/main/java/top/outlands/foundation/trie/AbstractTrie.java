package top.outlands.foundation.trie;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;



/**
 *
 * abstract class currently for PrefixTrie and SuffixTrie
 * @author Lin Chi-Min (v381654729@gmail.com)
 *
 * @param <V> a generic type 
 */
abstract class AbstractTrie<V>  {
	
	protected static int[] CHAR_TO_INDEX_MAP = TrieNode.CHAR_TO_INDEX_MAP;
	
	
	/**
	 * the root node of this trie
	 */
	protected TrieNode<V> root;
	
	/**
	 * number of key-value nodes added to this trie
	 */
	protected int size;
	
	/**
	 * true if all words are added with the constructor, and false if one or more words are not added
	 */
	private boolean isAllAdded;
	
	/**
	 * an internal constructor
	 */
	protected AbstractTrie() {
		root = new TrieNode<V>((char) 0, 0);
	}

	/**
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public AbstractTrie(List<String> keys, List<V> values) {
		this(keys, values, ones(keys.size()));
	}

	/**
	 * constructor for constructing a trie with the keys and values and scores;
	 * the scores are used for TrieNode comparison 
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 * @param scores: the scores of each of the key-value pairs 
	 */
	public AbstractTrie(List<String> keys, List<V> values, int[] scores) {
		this();
		if (keys.size() != values.size() || keys.size() != scores.length){
			throw new IllegalArgumentException("IllegalArgumentException: the sizes of 'elements', 'values' and 'scores' should agree; "
					+ "elements.size() = " + keys.size() + ", values.size() = " + values.size() +  ", scores.length = " + scores.length);
		}
		boolean allAdded = true;
		for (int i = 0; i < keys.size(); i++) {
			boolean added = put(keys.get(i), values.get(i), scores[i]);
			if (!added){
				allAdded = false;
			}
		}
		isAllAdded = allAdded;
	}
	
	/**
	 * <pre>
	 * Words that contain one or more unsupported chars are automatically not added to a trie.
	 * To check if all words are added, check trie.isAllAdded() to see if all characters are added;
	 * </pre>
	 * @return true if all words are added with the constructor, and false if one or more words are not added
	 */
	public boolean isAllAdded() {
		return isAllAdded;
	}
	
	/**
	 * @param length : the array length
	 * @return a int array in which all elements are 1
	 */
	private static int[] ones(int length){
		int[] ones = new int[length];
		Arrays.fill(ones, 1);
		return ones;
	}
	
	/**
	 * look up char indices
	 * @param chars : the chars of a prefix or suffix, from .toCharArray()
	 * @return indices of the chars according to CHAR_TO_INDEX_MAP  
	 */
	protected int[] lookupIndices(char[] chars) {
		int[] result = new int[chars.length];
		for (int i = 0; i < chars.length; i++) {
			int index = charToIndex(chars[i]);
			if (index == -1) {
				return null;
			} else {
				result[i] = index;
			}
		}
		return result;
	}
	
	
	/**
	 * @return number of key-value nodes added to this trie
	 */
	public int size(){
		return size;
	}
	
	/**
	 * @return the root
	 */
	public TrieNode<V> getRoot() {
		return root;
	}
	
	/**
	 * inserts a key and its value, a key-value pair, into this trie.
	 * @param key : the key 
	 * @param value : the value 
	 */
	public void put(String key, V value) {
		put(key, value, 1);
	}

	
	/**
	 * a fast way to convert char to index at the 'children' field of TrieNode
	 * @param c : a char
	 * @return index of c according to CHAR_TO_INDEX_MAP
	 */
	protected final int charToIndex(char c) {
		return c >= CHAR_TO_INDEX_MAP.length ? -1 : CHAR_TO_INDEX_MAP[c];
	}
	

	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @return the node that is prefixed or suffixed with word; 
	 * it may be a non-key-value node or a leaf node
	 */
	public TrieNode<V> getNode(String word) {
		TrieNode<V> theNode = getNodeWithLongestCommonPart(word);
		return (theNode != null && theNode.level == word.length()) ? theNode : null;
	}
	
	/**
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * @return the node that is prefixed with word.substring(0, length) 
	 * or suffixed with word.substring(word.length() - length, word.length()); 
	 * it may be a key-value node or not a key-value node
	 */
	public TrieNode<V> getNode(String word, int substringLength) {
		
		TrieNode<V> theNode = getNodeWithLongestCommonPart(word, substringLength);
		return (theNode != null && theNode.level == word.length()) ? theNode : null;
	}
	
	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @return either a key-value node or a non key-value node
	 */
	public TrieNode<V> getNodeWithLongestCommonPart(String word) {
		return getNodeWithLongestCommonPart(word, word.length());
	}
	
	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie 
	 * @return all key-value nodes prefixed or suffixed with this word 
	 */
	public List<TrieNode<V>> getKeyValueNodes(String word) {
		return getKeyValueNodes(word, word.length());
	}
	
	/**
	 * Equivalent to getKeyValueNodes(word.substring(0, maxLength)) for PrefixTrie,
	 * and equivalent to getKeyValueNodes(word.substring(word.length() - maxLength)) for SuffixTrie
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @return the key-value nodes with level &gt;= substringLength  
	 */
	protected List<TrieNode<V>> getKeyValueNodes(String word, int substringLength){
		TrieNode<V> tempSubtreeRoot = getNodeWithLongestCommonPart(word, substringLength);
		if (tempSubtreeRoot.level < substringLength){
			return Collections.emptyList();
		}
		return (tempSubtreeRoot == null) ? null : tempSubtreeRoot.getKeyValueNodes();
	}
	
	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @param condition : for selecting key-value nodes that matches this condition
	 * @return all key-value nodes that match 'condition'
	 */
	public List<TrieNode<V>> getKeyValueNodes(String word, Function<TrieNode<V>, Boolean> condition){
		return getKeyValueNodes(word, word.length(), condition);
	}
	
	/**
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @param condition : for selecting key-value nodes that matches this condition
	 * @return the matched key-value nodes with level &gt;= substringLength
	 */
	public List<TrieNode<V>> getKeyValueNodes(String word, int substringLength, Function<TrieNode<V>, Boolean> condition){
		TrieNode<V> tempSubtreeRoot = getNodeWithLongestCommonPart(word, substringLength);
		if (tempSubtreeRoot.level < substringLength){
			return Collections.emptyList();
		}
		return tempSubtreeRoot.getKeyValueNodes(condition);
	}
	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @return the key-value node for the exact word, or null if it does not exist
	 */
	public TrieNode<V> getKeyValueNode(String word){
		TrieNode<V> theNode = getNodeWithLongestCommonPart(word);
		return (theNode.isKeyValueNode && theNode.level == word.length()) ? theNode : null;
	}
	
	
	/**
	 * @return the top scored leaf node
	 */
	public TrieNode<V> getBestKeyValueNode(){
		return getRoot().getBestKeyValueNode();
	}
	

	/**
	 * @param comparator : comparator for key-value nodes comparison and selection
	 * @return the top scored key-value node according to the comparator
	 */
	public TrieNode<V> getBestKeyValueNode(Comparator<TrieNode<V>> comparator){
		return getRoot().getBestKeyValueNode(comparator);
	}
	
	
	
	/**
	 * see getBestKeyValueNode(word, length)
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @return the highest scored key-value node
	 */
	public TrieNode<V> getBestKeyValueNode(String word){
		return getBestKeyValueNode(word, word.length());
	}
	

	/**
	 * 
	 * Equivalent to getBestKeyValueNode(word.substring(0, maxLength)) for PrefixTrie,
	 * and equivalent to getBestKeyValueNode(word.substring(word.length() - maxLength)) for SuffixTrie
	 * 
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @return 
	 * the key-value node with the highest score among key-value nodes with level &gt;= substringLength 
	 */
	protected TrieNode<V> getBestKeyValueNode(String word, int substringLength) {
		return getBestKeyValueNode(word, substringLength, (a, b) -> (a.score - b.score));
	}


	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @param comparator : a normal comparator
	 * <pre> 
	 * if a &lt; b the comparator returns a negative value 
	 * if a == b the comparator returns 0 
	 * if a &gt; b the comparator returns a positive value
	 * </pre>
	 * @return the best key-value node with level &gt;= word.length();  
	 */
	public TrieNode<V> getBestKeyValueNode(String word, Comparator<TrieNode<V>> comparator) {
		return getBestKeyValueNode(word, word.length(), comparator);
	}
	
	
	/**
	 * Similar to getBestKeyValueNode, with TrieNode comparator 'comparator'
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @param comparator : comparator for selecting the best key-value node 
	 * @return the best key-value node with level &gt;= substringLength
	 */
	protected TrieNode<V> getBestKeyValueNode(String word, int substringLength, Comparator<TrieNode<V>> comparator) {
		TrieNode<V> tempSubtreeRoot = getNodeWithLongestCommonPart(word, substringLength);
		if (tempSubtreeRoot.level < substringLength){
			return null;
		}
		return (tempSubtreeRoot == null) ? null : tempSubtreeRoot.getBestKeyValueNode(comparator);
	}
	
	//////////////////////////////////////////////////////////
	
	/**
	 * @param word : prefix for PrefixTrie and suffix for SuffixTrie
	 * @param numTopKeyValueNodes : number of top key-value nodes
	 * @return best key-value nodes
	 */
	public List<TrieNode<V>> getBestKeyValueNodes(String word, int numTopKeyValueNodes) {
		return getBestKeyValueNodes(word, word.length(), numTopKeyValueNodes, (a, b) -> (a.score - b.score));
	}
	
	
	public List<TrieNode<V>> getBestKeyValueNodes(String word, int numTopKeyValueNodes, Comparator<TrieNode<V>> comparator) {
		return getBestKeyValueNodes(word, word.length(), numTopKeyValueNodes, comparator);
	}
	
	
	/**
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @param numTopKeyValueNodes : number of top key-value nodes to retrieve
	 * @param comparator : a comparator for comparison of key-value nodes
	 * @return the best key-value nodes with level &gt;= substringLength according to comparator 'comparator'
	 */
	private List<TrieNode<V>> getBestKeyValueNodes(String word, int substringLength, int numTopKeyValueNodes, Comparator<TrieNode<V>> comparator) {
		TrieNode<V> tempSubtreeRoot = getNodeWithLongestCommonPart(word, substringLength);
		if (tempSubtreeRoot.level < substringLength){
			return Collections.emptyList();
		}
		return (tempSubtreeRoot == null) ? null : tempSubtreeRoot.getBestKeyValueNodes(numTopKeyValueNodes, comparator);
	}
	
	
	
	/**
	 * inserts a key and its value, a key-value pair, with score 'score', into this trie.
	 * @param key : the key 
	 * @param value : the value 
	 * @param score : the score of the key value pair
	 * @return true if succesfully added, and false if the word contains unsupported characters
	 */
	protected abstract boolean put(String key, V value, int score);
	
	
	/**
	 * See implementation in PrefixTrie.java or SuffixTrie.java 
	 * @param word : a word
	 * @param substringLength : substring length of 'word' for prefix for PrefixTrie and suffix for SuffixTrie;
	 * for example, if word is "abcde" and substringLength is 3, then it's "abc" for PrefixTrie and "cde" for SuffixTrie
	 * @return the node that has the longest common suffix with word
	 */
	protected abstract TrieNode<V> getNodeWithLongestCommonPart(String word, int substringLength);
	
	
	
}
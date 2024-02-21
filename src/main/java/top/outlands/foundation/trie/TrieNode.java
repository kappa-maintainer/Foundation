package top.outlands.foundation.trie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * a class for nodes of the trie graph
 * 
 * @author Lin Chi-Min (v381654729@gmail.com)
 *
 */
public class TrieNode<V> {

	/**
	 * the default characters; 
	 */
	private static final String DEFAULT_CHARS_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+._";
	public static final int CHAR_LENGTH = DEFAULT_CHARS_STRING.length();
	
	static {
		/*
		  1. The default supported chars are "abcdefghijklmnopqrstuvwxyz0123456789";
		  2. warning: words that contain one or more unsupported chars are automatically not added to a trie
		  to check if all words are added, check trie.isAllAdded() to see if all characters are added;
		  3. Minimize the number of supported chars for speed optimization;
		 */
		setSupportedChars();
	}
	
	private static char[] chars;
	protected static int[] CHAR_TO_INDEX_MAP;
	
	
	/**
	 * 1. The default supported chars are "abcdefghijklmnopqrstuvwxyz0123456789";
	 * 2. warning: words that contain one or more unsupported chars are automatically not added to a trie
	 * to check if all words are added, check trie.isAllAdded() to see if all characters are added;
	 * 3. Minimize the number of supported chars for speed optimization;
	 */
	private static void setSupportedChars(){
		chars = TrieNode.DEFAULT_CHARS_STRING.toCharArray();
		int maxChar = -1;
		for (char c : chars) {
			if (c > maxChar){
				maxChar = c;
			}
		}
		int[] ciMap = new int[maxChar + 1];
		Arrays.fill(ciMap, -1);
		for (int i = 0; i < chars.length; i++) {
			ciMap[chars[i]] = i;
		}
		CHAR_TO_INDEX_MAP = ciMap;
		AbstractTrie.CHAR_TO_INDEX_MAP = ciMap;
		
	}

	
	/**
	 * the children nodes of this node
	 */
	TrieNode<V>[] children;
	
	/**
	 * the parent node of this node
	 */
	TrieNode<V> parent;
	
	
	/**
	 * whether this node is a key-value node
	 */
	boolean isKeyValueNode;
	
	/**
	 * a value stored in this node
	 */
	V value;
	
	/**the level of root is 0, 
	 * and the level of other nodes are 1, 2, 3, etc.
	 */
	int level;
	
	/**
	 * snippet for this node
	 */
	String snippet;
	

	/**
	 * current number of children;
	 * the current offset at 'childrenIndices'
	 */
	int numChildren;
	
	/**the children indices;
	 * children[childrenIndices[i]] where 0 &lt;= i &lt; numChildren is the ith children. 
	 */
	transient int[] childrenIndices;
	
	List<TrieNode<V>> KeyValueNodes;
	
	/**
	 * @param snippet : char snippet field of this node
	 * @param level : the level of this node in the trie
	 */
	@SuppressWarnings("unchecked")
	protected TrieNode(String snippet, int level) {
		// english characters, and -'._
		this.children = new TrieNode[chars.length];
		this.value = null;
		this.level = level;
		this.snippet = snippet;
		this.childrenIndices = new int[chars.length];
		Arrays.fill(childrenIndices, -1);
	}
	
	/**
	 * @return the value
	 */
	public V getValue() {
		return value;
	}
	
	/**
	 * @param value : the value of this node
	 */
	public void setValue(V value) {
		this.value = value;
	}
	
	/**
	 * the level of root is 0, 
	 * and the level of other nodes are 1, 2, 3, etc. 
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * @return wether this node is a key-value or not
	 */
	public boolean isKeyValueNode(){
		return isKeyValueNode;
	}
	
	/**
	 * @return the parent
	 */
	public TrieNode<V> getParent() {
		return parent;
	}

	
	/**
	 * @return key based on ancestors and this node 
	 */
	public String getKey(){
		StringBuilder keyChars = new StringBuilder();
		TrieNode<V> node = this;
		while (!node.isRoot()){
			keyChars.insert(0, node.snippet);
			node = node.parent;
		}
		return new String(keyChars);
	}
	
	/**
	 * @return the current number of children of this node 
	 */
	public int getNumChildren() {
		return numChildren;
	}
	
	/**
	 * @return non-null elements of 'children'	
	 */
	public List<TrieNode<V>> getNonNullChildren(){
		ArrayList<TrieNode<V>> result = new ArrayList<>(numChildren);
		for (int i = 0; i < numChildren; i++) {
			TrieNode<V> child = children[childrenIndices[i]];
			result.add(child);
		}
		return result;
	}

	
	@Override
	public String toString() {
		return "SuffixTrieNode [isKeyValueNode=" + isKeyValueNode + ", value=" + value + ", level=" + level + ", key=" + getKey() + ", snippet=" + snippet +"]";
	}

	protected void addChild(TrieNode<V> child) {
		int index = CHAR_TO_INDEX_MAP[child.snippet.charAt(0)];
		children[index] = child;
		addChildIndex(index);
		child.parent = this;
	}

	public String getSnippet() {
		return snippet;
	}

	/**
	 * @param index : the child index to add; 
	 * the index is according CHAR_TO_INDEX_MAP
	 */
	public void addChildIndex(int index) {
		
		childrenIndices[numChildren++] = index;
	}


	/**
	 * @return the first non-null child, or null if it has no children
	 */
	public TrieNode<V> getFirstChild(){
		return numChildren == 0 ? null : children[childrenIndices[0]];
	}
	
	/**
	 * @return whether this is the root node of the trie or not
	 */
	public boolean isRoot() {
		return level == 0;
	}

	
	/**
	 * @return a list, which contains all key-value nodes among sub-tree nodes
	 */
	public List<TrieNode<V>> getKeyValueNodes() {
		if (KeyValueNodes == null){
			ArrayList<TrieNode<V>> lns = new ArrayList<TrieNode<V>>();
			if (isKeyValueNode) {
				lns.add(this);
			}
			for (int i = 0; i < numChildren; i++) {
				TrieNode<V> child = children[childrenIndices[i]];
				lns.addAll(child.getKeyValueNodes());
			}
			KeyValueNodes = Collections.unmodifiableList(lns);
		}
		return KeyValueNodes;
	}
	
	/**
	 * @param condition : a condition for key-value nodes selection
	 * @return all key-value nodes that match 'condition'
	 */
	public List<TrieNode<V>> getKeyValueNodes(Function<TrieNode<V>, Boolean> condition) {
		List<TrieNode<V>> kvNodes = getKeyValueNodes();
		List<TrieNode<V>> result = new ArrayList<TrieNode<V>>();
		for (TrieNode<V> kvNode : kvNodes) {
			if (condition.apply(kvNode)){
				result.add(kvNode);
			}
		}
		return result;
	}
	
	/**
	 * @param comparator : for comparison of nodes
	 * @return the best key-value node according to the comparator
	 */
	public TrieNode<V> getBestKeyValueNode(Comparator<TrieNode<V>> comparator) {
		List<TrieNode<V>> KvNodes = getKeyValueNodes();
		return Collections.max(KvNodes, comparator);
	}

	
	/**@param numTopKeyValueNodes : number of top key-value nodes to select
	 * @param comparator : comparator
	 * @return the top key-value nodes according to the comparator
	 */
	public List<TrieNode<V>> getBestKeyValueNodes(int numTopKeyValueNodes, Comparator<TrieNode<V>> comparator) {
		
		if (numTopKeyValueNodes <= 0){
			throw new IllegalArgumentException("IllegalArgumentException: numTopKeyValueNodes (" + numTopKeyValueNodes + ") should be positive "); 
		} 
		List<TrieNode<V>> kvNodes = getKeyValueNodes();
		if (numTopKeyValueNodes == 1){
			TrieNode<V> kvNode = Collections.max(kvNodes, comparator);
			List<TrieNode<V>> result = new ArrayList<TrieNode<V>>();
			result.add(kvNode);
			return result;
		} else {
			ArrayList<TrieNode<V>> tempNodes = new ArrayList<>(kvNodes);
			Collections.sort(tempNodes, comparator.reversed());
			return tempNodes.subList(0, Math.min(numTopKeyValueNodes, kvNodes.size()));
			
//			Collections.sort(kvNodes, comparator.reversed());
//			return kvNodes.subList(0, Math.min(numTopKeyValueNodes, kvNodes.size()));	
		}
	}
	
	
	
	
}
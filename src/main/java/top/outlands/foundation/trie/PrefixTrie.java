
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
	 * constructor for constructing a trie with the keys and values
	 * @param keys : the keys for trie construction 
	 * @param values : the corresponding values of the keys
	 */
	public PrefixTrie(String[] keys, V[] values) {
		super(Arrays.asList(keys), Arrays.asList(values));
	}

	
	/**
	 * {@inheritDoc}
	 */
	public boolean put(String key, V value) {
		
		TrieNode<V> node = root;
		char[] chars = key.toCharArray();
		int[] indices = lookupIndices(chars);
		if (indices == null){
			// not allowed to add this word if one of the chars is unsupported
			return false;
		}
		TrieNode<V> temp;
		int fed = 0;
		int i;
		while (true){
			for (i = 0; i < node.snippet.length(); i++) {
				if (node.snippet.charAt(i) != chars[fed + i]) {
					breakApart(node, i);
					node.addChild(genValueNode(key.substring(fed + i), value, key.length()));
					size++;
					return true;
				}
				if (fed + i == key.length() -1) {
					if (i < node.snippet.length() - 1) {
						breakApart(node, i + 1);
						size++;
					} else if (!node.isKeyValueNode) {
						size++;
					}
					node.isKeyValueNode = true;
					node.value = value;
					return true;
				}
				if (i == node.snippet.length() - 1) {
					temp = node.children[indices[fed + i + 1]];
					if (temp == null) {
						node.addChild(genValueNode(key.substring(fed + i + 1), value, key.length()));
						size++;
						return true;
					} else {
						fed += node.snippet.length();
						node = temp;
						break;
					}
				}
			}
			if (node == root) {
				temp = node.children[charToIndex(chars[0])];
				if (temp == null) {
					node.addChild(genValueNode(key, value, key.length()));
					size++;
					return true;
				} else {
					node = temp;
				}
			}
		}
    }

	@SuppressWarnings("unchecked")
	private void breakApart(TrieNode<V> node, int i) {
		// generate new child node and copy children data from current node
		TrieNode<V> child = new TrieNode<>(node.snippet.substring(i), node.level);
		child.isKeyValueNode = node.isKeyValueNode;
		child.children = node.children;
		child.numChildren = node.numChildren;
		child.childrenIndices = node.childrenIndices;
		child.parent = node;
		child.value = node.value;
		for (var c : node.getNonNullChildren()) {
			c.parent = child;
		}
		// recreate children index, reset value and cut snippet
		node.children = new TrieNode[TrieNode.CHAR_LENGTH];
		node.isKeyValueNode = false;
		node.value = null;
		node.level = node.parent.level + i;
		node.snippet = node.snippet.substring(0, i);
		// add new child to node
		int index = charToIndex(child.snippet.charAt(0));
		node.children[index] = child;
		node.childrenIndices = new int[TrieNode.CHAR_LENGTH];
		node.numChildren = 0;
		node.addChildIndex(index);
	}

	private TrieNode<V> genValueNode(String snippet, V value, int fullLength) {
		TrieNode<V> node = new TrieNode<>(snippet, fullLength);
		node.isKeyValueNode = true;
		node.value = value;
		return node;
	}


	
	
	/**
	 * 1. For input word "abcde", 
	 * if the node that has the longest common prefix with level &lt;= maxPrefixLength is "abc3", 
	 * return node 'c'
	 * <p>
	 * 2. Equivalent to getNodeWithLongestCommonPart(word.substring(0, maxPrefixLength))
	 *  
	 * @param key : a word
	 * @param maxPrefixLength : as described above
	 * @return the node that has the longest common prefix with word
	 */
	@Override
	protected TrieNode<V> getNodeWithLongestCommonPart(String key, int maxPrefixLength) {
		if (maxPrefixLength < 0) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxPrefixLength' (" + maxPrefixLength + ") should be non-negative.");
		} else if (maxPrefixLength > key.length()) {
			throw new IllegalArgumentException(
					"IllegalArgumentException: the argument 'maxPrefixLength' (" + maxPrefixLength + ") should not be larger than word.length().");
		}
		
		TrieNode<V> node = root;
		int i = 0;
        while (i < maxPrefixLength) {
            if (key.substring(i).startsWith(node.snippet)) {
                i += node.snippet.length();
				if (i == key.length()) {
					return node;
				}
            } else {
                return node;
            }
            int index = charToIndex(key.charAt(i));
            if (index >= 0 && node.children[index] != null) {
                node = node.children[index];
            } else {
                return node;
            }
        }
        return node;
	}
	
	public TrieNode<V> getFirstKeyValueNode(String key) {
		TrieNode<V> node = root;
		int i = 0;
        while (i < key.length()) {
			if (key.substring(i).startsWith(node.snippet)) {
				if (node.isKeyValueNode) {
					return node;
				}
				i += node.snippet.length();
				if (i == key.length()) {
					break;
				}
			} else {
				return null;
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

package quadtree;

import java.io.Serializable;

/**
 * Enumeration of node types.
 * @enum {number}
 */
public enum NodeType implements Serializable {
    EMPTY,
    LEAF,
    POINTER
}

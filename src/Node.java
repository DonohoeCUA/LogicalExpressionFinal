public class Node{
    public char value;
    public ConditionalValidity validity;
    public Node left, right;

    public Node(char value){
        left = right = null;
        validity = ConditionalValidity.UNKNOWN;
        this.value = value;
    }

    //adapted from answers from https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram-in-java
    public String toString() {
        return this.toString(new StringBuilder(), true, new StringBuilder()).toString();
    }

    /*
     * Code Breakdown:
     * takes a StringBuilder which contains a more efficient string concatenation.
     * https://techurp.com/stringbuilder-vs-concatenation-in-javas-tostring-method-how-to-optimize-performance/?utm_source=chatgpt.com
     *
     * ends up using O(n^2) vs O(n), so were vastly improving performance.
     * after reading this article, im updating some  incorporate high quantity concatenations with StringBuilder.
     *
     * the size of the string is unknown, hence an empty constructor use.
     * pass by reference because sb is an object makes recursion possible and readable
     *
     * the "isTail" boolean indicates if the node is the only remaining child of the branch or not.
     * the right subtree prints '|' when there is another node "above" (to the right of) it, whereas left prints '|' below.
     * if its not the tail of something, then it prints space until that specific line IS a tail, because every
     * printed line will have a tail on it.
     * */
    private StringBuilder toString(StringBuilder prefix, boolean isTail, StringBuilder sb) {
        if(right!=null) {
            right.toString(new StringBuilder().append(prefix).append(isTail ? "│   " : "    "), false, sb);
        }
        String newValue = String.valueOf(value);
        if (value == '?') newValue = "OR";
        else if (value == '&') newValue = "AND";
        else if (value == '~') newValue = "NOT";

        sb.append(prefix).append(isTail ? "└── " : "┌── ").append(newValue).append(": ").append(validity).append("\n");
        if(left!=null) {
            left.toString(new StringBuilder().append(prefix).append(isTail ? "    " : "│   "), true, sb);
        }
        return sb;
    }
}


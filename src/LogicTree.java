import java.util.LinkedList;

public class LogicTree {
    Node root;
    Node[] nodeReferences = new Node[26];//for A-Z

    public LogicTree() {
        root = null;
    }
    public LogicTree(String expression){
        //only works with ^, |, -
        root = populateTreeInPreorder(expression);
    }
    public LogicTree(String expression, String... predicates){
        root = populateTreeInPreorder(expression);
        evaluatePredicates(predicates);
    }
    //Parsing and Formatting input
    public String parseInputToPreorder(String input) {
        //replace any whitespace from string so we don't have to use trim
        return getPreorderRecursively(input.replaceAll("\\s", ""));
    }
    private String getPreorderRecursively(String input) {
        // De Morgan's Law is checked before opperators incase of (-(...))
        if (input.startsWith("~(") && input.endsWith(")")) {
            String innerExpression = input.substring(2, input.length() - 1);
            int centerOperatorIndex = findMainOperatorIndex(innerExpression);//check if it's just a single variable or a statement

            if (centerOperatorIndex != -1) {
                char operator = innerExpression.charAt(centerOperatorIndex);
                String left = innerExpression.substring(0, centerOperatorIndex);
                String right = innerExpression.substring(centerOperatorIndex + 1);

                //replace operators
                char newOp = (operator == '&') ? '?' : '&';
                String newLeft = "~" + left;
                String newRight = "~" + right;

                //recurse De Morgans law to variables
                return newOp + getPreorderRecursively(newLeft) + getPreorderRecursively(newRight);
            } else {
                return "~" + getPreorderRecursively(innerExpression);
            }
        }

        //single variable, removing parentheses if needed
        if (!input.contains("&") && !input.contains("?")) {
            if(input.charAt(0) == '(')
                return input.substring(1, input.length()-1);
            else
                return input;
        }

        //expressions eg. p^q without De Morgans Law applied
        int index = findMainOperatorIndex(input);
        if (index == -1) {
            System.out.println("Error: Cant find main operator");
            return input;
        }

        char operator = input.charAt(index);
        String left = getVarBetweenParentheses(input.substring(0, index));
        String right = getVarBetweenParentheses(input.substring(index + 1));

        System.out.println(operator + "\t" + left + "\t" + right);

        return operator + getPreorderRecursively(left) + getPreorderRecursively(right);
    }

    //Populating Tree
    private Node populateTreeInPreorder(String expression) {
        if (expression.isEmpty()) {
            System.out.println("Error: String empty");
            return null;
        }
        
        String preorderedExpression = parseInputToPreorder(expression);
        System.out.println("Preorder Expression: " + preorderedExpression);

        /*
         Im using a LinkedList, because it enables pass by reference, which a string doesn't offer in java.
         I could have used two arrays, one for storing characters, and the other for an index, but that isn't very readable

         By using a LinkedList instead of an Arraylist, im able to reduce the cost of remove, which is O(n)
         down to O(1) by just removing the llNode, while still maintaining the reference and readability I wanted.
        */
        LinkedList<Character> list = new LinkedList<>();
        for (char c : preorderedExpression.toCharArray()) {//toCharArray is faster because it deals with memory in cpp
            list.add(c);
        }
        return populateRecursively(list);
    }
    private Node populateRecursively(LinkedList<Character> list) {
        if (list.isEmpty()) {
            return null;
        }
        char c = list.pollFirst();

        //all variable nodes are leafs, and all variables will have the same value
        //either true or false. Having direct access to every node will make the assignment
        //process cheaper when getting values for the tables
        Node node;
        if (c-'A' >= 0 && c-'A' <= 25 && nodeReferences[c-'A'] != null)
            node = nodeReferences[c-'A'];
        else
            node = new Node(c);

        //OPERANDS
        if (c == '&' || c == '?') {
            //both subtrees
            node.left = populateRecursively(list);
            node.right = populateRecursively(list);
        }
        else if (c == '~'){
            //catch
            if (list.isEmpty()) {
                return null;
            }
            char characterAfterNegative = list.pollFirst();
            node.left = new Node(characterAfterNegative);
        }
        //OPERATORS (eg. 'A' or 'Z') will not have any children, thus being a leaf
        return node;
    }

    //Helper Methods
    private int findMainOperatorIndex(String s) {
        int parenCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if ((c == '&' || c == '?') && parenCount == 0) {
                return i;
            }
        }
        return -1;
    }
    public String getVarBetweenParentheses(String string){
        // Get contents from this layer between any parentheses `(...)`
        if (string.charAt(0) == '(') {
            int openings = 1;
            for (int i = 1; i < string.length(); i++) {
                if (string.charAt(i) == '(') openings++;
                else if (string.charAt(i) == ')') openings--;

                if (openings == 0) {
                    return string.substring(1, i);
                }
            }
        }
        return string;
    }

    private void evaluatePredicates(String[] predicates){
        //types of predicates:
        //if then:          p > q
        //if and only if:   p iff q
        //statement:        p


        //convert p > q  into  -p | q
    }

    public void printTree() {
        if (root != null) {
            System.out.println(root);
        } else {
            System.out.println("Tree is empty.");
        }
    }
}

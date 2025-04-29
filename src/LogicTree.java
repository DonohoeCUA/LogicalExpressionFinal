import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LogicTree {
    private Node root;
    private Node[] nodeReferences = new Node[26];//for A-Z

    public LogicTree(String expression){
        root = populateTreeInPreorder(expression);
    }
    public LogicTree(String expression, String... predicates){
        root = populateTreeInPreorder(expression);
        evaluatePredicates(predicates);
    }


    /////////////////   POPULATING TREE    /////////////////
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

    /////////////////   PARSING AND FORMATTING    /////////////////
    public String parseInputToPreorder(String input) {
        //replace any whitespace from string so we don't have to use trim
        String formattedExpression = input.replaceAll("\\s", "");
        //checks if main expression is a predicate eg. p>(...) and converts to expression
        if(formattedExpression.contains(">"))//char for predicates
        {
            System.out.println("main expression is a predicate, has >");
            String[] tokens = formattedExpression.split(">");
            if (tokens.length != 2) {
                System.out.println("Error: Invalid predicate-based expression: " + input);
                return "";
            }
            //turn to ~p ? q       not p or q      form
            formattedExpression = "~" + tokens[0].charAt(0) + "?" + tokens[1];
            System.out.println("main expression now: " + formattedExpression);

        }
        return getPreorderRecursively(formattedExpression);
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
        int index = findMainOperatorIndex(input);
        if (index == -1) {
            if(input.charAt(0) == '(')
                return input.substring(1, input.length()-1);
            else
                return input;
        }

        //expressions eg. p^q without De Morgans Law applied
        char operator = input.charAt(index);
        String left = getVarBetweenParentheses(input.substring(0, index));
        String right = getVarBetweenParentheses(input.substring(index + 1));
        //System.out.println(operator + "\t" + left + "\t" + right);//debug top to bottom
        return operator + getPreorderRecursively(left) + getPreorderRecursively(right);
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

    private void evaluatePredicates(String[] predicates) {
        int index = 0;
        for (String currentPredicate : predicates) {
            String formattedPredicate = currentPredicate.replaceAll("\\s", "").toUpperCase();
            if (formattedPredicate.isEmpty()) continue;

            System.out.print("P" + ++index + ":" + formattedPredicate);

            // 1) IFF (equality) predicates, e.g. "P=Q"
            if (formattedPredicate.contains("=") && !formattedPredicate.contains(">")) {
                System.out.println("\t is a IFF");

                String[] tokens = formattedPredicate.split("=");
                if (tokens.length != 2) {
                    System.out.println("Error: Invalid iff predicate: " + currentPredicate);
                    continue;
                }
                //get variable
                char variable1 = tokens[0].charAt(0);
                char variable2 = tokens[1].charAt(0);
                int index1 = variable1 - 'A';
                int index2 = variable2 - 'A';
                //made node reference if not initialized
                if (nodeReferences[index1] == null) nodeReferences[index1] = new Node(variable1);
                if (nodeReferences[index2] == null) nodeReferences[index2] = new Node(variable2);
                Node node1 = nodeReferences[index1];
                Node node2 = nodeReferences[index2];

                // Check equality or invalidate.
                if (node1.validity.value != ConditionalValidity.UNKNOWN && node2.validity.value != ConditionalValidity.UNKNOWN) {
                    if (node1.validity.value != node2.validity.value) {
                        node1.validity = node2.validity = new ValidityRef(ConditionalValidity.INVALID);
                    } else {
                        node2.validity = node1.validity;
                    }
                }
                // Propagate known values
                else if (node1.validity.value != ConditionalValidity.UNKNOWN) {
                    node2.validity = node1.validity;
                } else if (node2.validity.value != ConditionalValidity.UNKNOWN) {
                    node1.validity = node2.validity;
                } else {
                    // Both unknown: link references so future sets affect both.
                    node2.validity = node1.validity;
                }
            }
            // 2) Implication predicates, e.g. "P>Q" or "P>~Q"
            else if (formattedPredicate.contains(">")) {
                System.out.println("\t is a IF THEN");

                String[] tokens = formattedPredicate.split(">");
                if (tokens.length != 2) {
                    System.out.println("Error: Invalid if-then predicate: " + currentPredicate);
                    continue;
                }
                //check if either value is negative and get char
                String antecedentStr = tokens[0];
                String consequentStr = tokens[1];
                boolean isAntecNegated = antecedentStr.startsWith("~");
                boolean isConseqNegated = consequentStr.startsWith("~");
                char antecedentChar = isAntecNegated ? antecedentStr.charAt(1) : antecedentStr.charAt(0);
                char consequentChar = isConseqNegated ? consequentStr.charAt(1) : consequentStr.charAt(0);

                int antecedentIndex = antecedentChar - 'A';
                int consequentIndex = consequentChar - 'A';
                //define undefined nodes
                if (nodeReferences[antecedentIndex] == null) nodeReferences[antecedentIndex] = new Node(antecedentChar);
                if (nodeReferences[consequentIndex] == null) nodeReferences[consequentIndex] = new Node(consequentChar);
                Node antecedentNode = nodeReferences[antecedentIndex];
                Node consequentNode = nodeReferences[consequentIndex];

                //if its negated, make sure antecedent is TRUE, otherwise it holds no value
                boolean isAntecedentTrue = isAntecNegated
                        ? (antecedentNode.validity.value == ConditionalValidity.FALSE)
                        : (antecedentNode.validity.value == ConditionalValidity.TRUE);
                ConditionalValidity requiredConsequentValue = isConseqNegated ? ConditionalValidity.FALSE : ConditionalValidity.TRUE;

                // MOTUS POTENS: If antecedent holds true, set any value that is unknown to appropriate value
                // If it has to "override" a value, somewhere some predicate is WRONG, and thus invalidates that part of the arguement
                if (isAntecedentTrue) {
                    if (consequentNode.validity.value == ConditionalValidity.UNKNOWN) {
                        consequentNode.validity = new ValidityRef(requiredConsequentValue);
                    } else if (consequentNode.validity.value != requiredConsequentValue) {
                        antecedentNode.validity = consequentNode.validity = new ValidityRef(ConditionalValidity.INVALID);
                    }
                }
                // MOTUS TOLENS/CONTRAPOSITIVE: if consequent is known false (relative to expectation), enforce antecedent false.
                if (consequentNode.validity.value != ConditionalValidity.UNKNOWN && consequentNode.validity.value != requiredConsequentValue) {
                    ConditionalValidity requiredAntecedentValue = isAntecNegated ? ConditionalValidity.TRUE : ConditionalValidity.FALSE;
                    if (antecedentNode.validity.value == ConditionalValidity.UNKNOWN) {
                        antecedentNode.validity = new ValidityRef(requiredAntecedentValue);
                    } else if (antecedentNode.validity.value != requiredAntecedentValue) {
                        antecedentNode.validity = consequentNode.validity = new ValidityRef(ConditionalValidity.INVALID);
                    }
                }
            }
            // 3) Simple assignments: "P", "~P"
            else {
                System.out.println("\t is a simple assignment");

                boolean isNegated = formattedPredicate.startsWith("~");
                char variableChar = isNegated ? formattedPredicate.charAt(1) : formattedPredicate.charAt(0);
                int variableIndex = variableChar - 'A';
                //define nodes if undefined
                if (nodeReferences[variableIndex] == null) nodeReferences[variableIndex] = new Node(variableChar);
                Node variableNode = nodeReferences[variableIndex];

                ConditionalValidity assignedValidity = isNegated ? ConditionalValidity.FALSE : ConditionalValidity.TRUE;

                //if already assigned a value, statement becomes invalid
                if (variableNode.validity.value == ConditionalValidity.UNKNOWN) {
                    variableNode.validity = new ValidityRef(assignedValidity);
                } else if (variableNode.validity.value != assignedValidity) {
                    variableNode.validity = new ValidityRef(ConditionalValidity.INVALID);
                }
                System.out.println("NODE " + variableNode.value + " IS SET TO : " + variableNode.validity.value);

            }
        }
    }

    /////////////////   EVALUATE TREE    /////////////////
    public ConditionalValidity evaluate() {
        if (root == null) {
            System.out.println("Error: Tree is empty.");
            return ConditionalValidity.UNKNOWN;
        }
        return evaluateNode(root);
    }

    private ConditionalValidity evaluateNode(Node node) {
        if (node == null) return ConditionalValidity.UNKNOWN;

        // If it's a variable (leaf node)
        if (Character.isLetter(node.value)) {
            return node.validity.value;
        }

        // If it's an operator
        if (node.value == '~') { // not
            ConditionalValidity childValidity = evaluateNode(node.left);
            return negate(childValidity);
        } else if (node.value == '&') { // and
            ConditionalValidity leftValidity = evaluateNode(node.left);
            ConditionalValidity rightValidity = evaluateNode(node.right);
            return and(leftValidity, rightValidity);
        } else if (node.value == '?') { // or
            ConditionalValidity leftValidity = evaluateNode(node.left);
            ConditionalValidity rightValidity = evaluateNode(node.right);
            return or(leftValidity, rightValidity);
        } else {
            System.out.println("Error: Unknown operator " + node.value);
            return ConditionalValidity.UNKNOWN;
        }
    }

    // evaluate node helpers
    private ConditionalValidity negate(ConditionalValidity val) {
        switch (val) {
            case TRUE: return ConditionalValidity.FALSE;
            case FALSE: return ConditionalValidity.TRUE;
            case INVALID: return ConditionalValidity.INVALID;
            case UNKNOWN: return ConditionalValidity.UNKNOWN;
        }
        return ConditionalValidity.UNKNOWN; // just in case
    }
    private ConditionalValidity and(ConditionalValidity a, ConditionalValidity b) {
        // return most negative value
        if (a == ConditionalValidity.FALSE || b == ConditionalValidity.FALSE) return ConditionalValidity.FALSE;
        if (a == ConditionalValidity.INVALID || b == ConditionalValidity.INVALID) return ConditionalValidity.INVALID;
        if (a == ConditionalValidity.UNKNOWN || b == ConditionalValidity.UNKNOWN) return ConditionalValidity.UNKNOWN;
        return ConditionalValidity.TRUE;
    }
    private ConditionalValidity or(ConditionalValidity a, ConditionalValidity b) {//return least negative value
        if (a == ConditionalValidity.TRUE || b == ConditionalValidity.TRUE) return ConditionalValidity.TRUE;
        if (a == ConditionalValidity.UNKNOWN || b == ConditionalValidity.UNKNOWN) return ConditionalValidity.UNKNOWN;
        if (a == ConditionalValidity.INVALID || b == ConditionalValidity.INVALID) return ConditionalValidity.INVALID;
        return ConditionalValidity.FALSE;
    }

    /////////////////      OUTPUTS      /////////////////
    public void printTree() {
        if (root != null) {
            System.out.println(root);
        } else {
            System.out.println("Error: Tree is empty.");
        }
    }
    public void printTable() {
        /*
        // Find used variables
        List<Character> variables = new ArrayList<>();
        for (int i = 0; i < nodeReferences.length; i++) {
            if (nodeReferences[i] != null) {
                variables.add((char) ('A' + i));
            }
        }

        if (variables.isEmpty()) {
            System.out.println("No variables to print in the table.");
            return;
        }

        // Print header
        for (char var : variables) {
            System.out.print(var + "\t");
        }
        System.out.println("| Result");

        int numRows = 1 << variables.size(); // 2^n rows

        // For each row
        for (int i = 0; i < numRows; i++) {
            // Assign TRUE/FALSE based on bits
            for (int j = 0; j < variables.size(); j++) {
                char var = variables.get(j);
                boolean isTrue = ((i >> (variables.size() - j - 1)) & 1) == 1;
                int varIndex = var - 'A';
                if (nodeReferences[varIndex] == null) {
                    nodeReferences[varIndex] = new Node(var);
                }
                nodeReferences[varIndex].validity = new ValidityRef(isTrue ? ConditionalValidity.TRUE : ConditionalValidity.FALSE);
                System.out.print((isTrue ? "T" : "F") + "\t");
            }

            // Evaluate with current assignment
            ConditionalValidity result = evaluate();
            System.out.println("| " + result);
        }
        */
    }

}

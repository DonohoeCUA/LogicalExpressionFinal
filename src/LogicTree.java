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
        evaluatePredicatesUntilStable(predicates);
        //evaluatePredicates(predicates);
    }


    /////////////////   POPULATING TREE    /////////////////
    private Node populateTreeInPreorder(String expression) {
        if (expression.isEmpty()) {
            System.out.println("Error: String empty");
            return null;
        }

        String preorderedExpression = parseInputToPreorder(expression);
        //System.out.println("Preorder Expression: " + preorderedExpression);

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
        if (list.isEmpty()) return null;
        char c = list.pollFirst();

        Node node;
        if (c == '&' || c == '?' || c == '~') {
            node = new Node(c);
        } else if (Character.isLetter(c)) {
            // new var?
            if (nodeReferences[c - 'A'] == null) {
                nodeReferences[c - 'A'] = new Node(c);
            }
            node = nodeReferences[c - 'A'];
        } else {
            // any other symbol (shouldn’t happen)
            node = new Node(c);
        }

        // 2) Recurse to build its subtree
        if (c == '&' || c == '?') {
            node.left  = populateRecursively(list);
            node.right = populateRecursively(list);
        }
        else if (c == '~') {
            node.left = populateRecursively(list);
        }
        // letters are leaves

        return node;
    }

    /////////////////   PARSING AND FORMATTING    /////////////////
    public String parseInputToPreorder(String input) {
        //replace any whitespace from string so we don't have to use trim
        String formattedExpression = input.replaceAll("\\s", "");
        //checks if main expression is a predicate eg. p>(...) and converts to expression
        if(formattedExpression.contains(">"))//char for predicates
        {
            String[] tokens = formattedExpression.split(">");
            if (tokens.length != 2) {
                System.out.println("Error: Invalid predicate-based expression: " + input);
                return "";
            }
            //turn to ~p ? q       not p or q      form
            formattedExpression = "~" + tokens[0].charAt(0) + "?" + tokens[1];

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

    /*some predicates like motus potens or tollens require ordering, P>Q, Q, but it never knew Q was true
    * The repititon is like a shimmy, where it keeps updating so that it will always finish with the final solution
    * so even if you do, R>S, S>Q, R, it will eventually find its way down to Q, and update it.
    * */
    private void evaluatePredicatesUntilStable(String[] predicates) {
        //order from least to greatest so ideally all of the variable setting (p, ~p etc) is done first
        // cutting down on any Motus Tollens or Motus Potens opperations
        for(int i=0;i<predicates.length-1;i++)
        {
            for(int j=i+1;j<predicates.length;j++)
            {
                if(predicates[i].length()>predicates[j].length())
                {
                    String temp=predicates[i];
                    predicates[i]=predicates[j];
                    predicates[j]=temp;
                }
            }
        }

        boolean changed;
        do {
            changed = false;
            ConditionalValidity[] before = new ConditionalValidity[26];
            for (int i = 0; i < 26; i++) {
                if (nodeReferences[i] != null) {
                    before[i] = nodeReferences[i].validity.value;
                }
            }

            evaluatePredicates(predicates); // apply all again

            for (int i = 0; i < 26; i++) {
                if (nodeReferences[i] != null && before[i] != nodeReferences[i].validity.value) {
                    changed = true;
                }
            }
        } while (changed);
    }

    private void evaluatePredicates(String[] predicates) {
        for (String currentPredicate : predicates) {
            String formattedPredicate = currentPredicate.replaceAll("\\s", "").toUpperCase();
            if (formattedPredicate.isEmpty()) continue;

            // 1) IFF (equality) predicates, e.g. "P=Q"
            if (formattedPredicate.contains("=") && !formattedPredicate.contains(">")) {

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
                // MOTUS TOLENS: if consequent is known false (relative to expectation), enforce antecedent false.
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
            node.validity.value = negate(childValidity);
            return node.validity.value;
        } else if (node.value == '&') { // and
            ConditionalValidity leftValidity = evaluateNode(node.left);
            ConditionalValidity rightValidity = evaluateNode(node.right);
            node.validity.value = and(leftValidity, rightValidity);
            return node.validity.value;
        } else if (node.value == '?') { // or
            ConditionalValidity leftValidity = evaluateNode(node.left);
            ConditionalValidity rightValidity = evaluateNode(node.right);
            node.validity.value = or(leftValidity, rightValidity);
            return node.validity.value;
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
    public void printPartialTruthTable() {
        // 1: Split variables into known and unknown
        List<Character> unknownVars = new ArrayList<>();
        List<Character> knownVars = new ArrayList<>();
        for (int i = 0; i < nodeReferences.length; i++) {
            Node node = nodeReferences[i];
            if (node != null) {
                if (node.validity.value == ConditionalValidity.UNKNOWN) {
                    unknownVars.add((char) ('A' + i));
                } else {
                    knownVars.add((char) ('A' + i));
                }
            }
        }

        if (unknownVars.isEmpty()) {
            System.out.println("No unknown variables to build a truth table with.");
            System.out.println("Result: " + evaluate());
            return;
        }

        // 2: get all operators for different columns
        List<Node> subexpressions = new ArrayList<>();
        collectOperatorNodes(root, subexpressions);

        // 3: Fill in headers
        List<String> headers = new ArrayList<>();
        for (char c : knownVars) headers.add(String.valueOf(c));
        for (char c : unknownVars) headers.add(String.valueOf(c));
        List<String> expressionLabels = new ArrayList<>();
        for (Node n : subexpressions) expressionLabels.add(getSubexpressionString(n));
        headers.addAll(expressionLabels);

        // 4: Figure out the width for formatting
        int colCount = headers.size();
        int[] colWidths = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            colWidths[i] = headers.get(i).length();
        }

        int rowCount = (int) Math.pow(2, unknownVars.size());//2 possibilities (true, false) for every variable
        List<List<String>> allRows = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            ValidityRef[] backup = new ValidityRef[26];
            for (int j = 0; j < 26; j++) {
                if (nodeReferences[j] != null) {
                    backup[j] = nodeReferences[j].validity;
                    nodeReferences[j].validity = new ValidityRef(backup[j].value);
                }
            }

            // Set unknowns
            for (int j = 0; j < unknownVars.size(); j++) {
                char var = unknownVars.get(j);
                boolean isTrue = ((i >> (unknownVars.size() - j - 1)) & 1) == 1;
                nodeReferences[var - 'A'].validity = new ValidityRef(
                        isTrue ? ConditionalValidity.TRUE : ConditionalValidity.FALSE
                );
            }

            List<String> row = new ArrayList<>();
            for (char c : knownVars) {
                String val = formatValidity(nodeReferences[c - 'A'].validity.value);
                row.add(val);
            }
            for (char c : unknownVars) {
                String val = formatValidity(nodeReferences[c - 'A'].validity.value);
                row.add(val);
            }
            for (Node n : subexpressions) {
                String val = formatValidity(evaluateNode(n));
                row.add(val);
            }

            // Update max widths
            for (int j = 0; j < row.size(); j++) {
                colWidths[j] = Math.max(colWidths[j], row.get(j).length());
            }

            allRows.add(row);

            // Restore
            for (int j = 0; j < 26; j++) {
                if (nodeReferences[j] != null) {
                    nodeReferences[j].validity = backup[j];
                }
            }
        }

        //header
        for (int i = 0; i < colCount; i++) {
            System.out.print(pad(headers.get(i), colWidths[i]));
            if (i < colCount - 1) System.out.print(" | ");
        }
        System.out.println();

        //rows
        for (List<String> row : allRows) {
            for (int i = 0; i < row.size(); i++) {
                System.out.print(pad(row.get(i), colWidths[i]));
                if (i < row.size() - 1) System.out.print(" | ");
            }
            System.out.println();
        }
    }
    //helper to get nodes
    private void collectOperatorNodes(Node node, List<Node> list) {
        if (node == null) return;
        if (!Character.isLetter(node.value)) list.add(node);
        collectOperatorNodes(node.left, list);
        collectOperatorNodes(node.right, list);
    }

    // Converts nodes back into strings
    private String getSubexpressionString(Node node) {
        if (node == null) return "";
        if (Character.isLetter(node.value)) return String.valueOf(node.value);
        String left = getSubexpressionString(node.left);
        String right = getSubexpressionString(node.right);
        switch (node.value) {
            case '~': return "~" + left;
            case '&': return "(" + left + "&" + right + ")";
            case '?': return "(" + left + "?" + right + ")";
            default: return "(" + left + node.value + right + ")";
        }
    }

    //helper convert enum
    private String formatValidity(ConditionalValidity val) {
        return switch (val) {
            case TRUE -> "T";
            case FALSE -> "F";
            case UNKNOWN -> "?";
            case INVALID -> "X";
        };
    }

    // Pad string to fixed width (left-aligned)
    private String pad(String s, int width) {
        while (s.length() < width) {
            s += " ";
        }
        return s;
    }
}

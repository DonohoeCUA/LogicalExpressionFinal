import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Write your logical expression, followed by its predicates.\n");
        System.out.println("\tFormatting Examples:\nExpression:\tP & ~(S ? Q)\nPredicates:\tP\n\t\t\tQ > ~P");
        System.out.println("\nAnd: &\t Or: ?\t Not: ~\t Iff: =\t If-Then: >\n");
        System.out.print("Expression:\t");

        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        String expression = scanner.nextLine();
        expression = expression.toUpperCase();
//        String expression = "P ^ -((S) | (Q|P))";

        LinkedList<String> predicates = new LinkedList<>();
        int count = 0;
        System.out.println("Enter predicates (type 'done' to finish):\n");
        while (true) {
            System.out.print("P" + (++count) + ": ");
            String predicate = scanner.nextLine().toUpperCase();
            if (predicate.equals("DONE")) {
                break;
            }
            predicates.add(predicate);
        }

        //String[0] when using toArray just takes the size of the list rather than a specified size
        //you could use toArray(new String[predicates.length()]) but that's messier and not convention
        String[] predicatesArray = predicates.toArray(new String[0]);
        LogicTree evaluator = new LogicTree(expression, predicatesArray);

        System.out.println();
        System.out.println("The statement is " + evaluator.evaluate().toString());

        System.out.println();
        evaluator.printTree();

        System.out.println();
        evaluator.printPartialTruthTable();
    }
}

import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Write your logical expression, followed by its predicates.");
        System.out.println("Formatting Examples:\n\nExpression:\nP ^ (S | Q)\nQ iff S\n\nPredicates:\nP -> TRUE\nQ -> - Q");
        System.out.print("Expression:\t");
        //String expression = scanner.nextLine();
        System.out.println();
        String expression = "P ^ -((S) | (Q|P))";

//        LinkedList<String> predicates = new LinkedList<>();
//        int count = 0;
//        System.out.println("Enter predicates (type 'done' to finish):\n");
//        while (true) {
//            System.out.println("P" + (count+1));
//            String predicate = scanner.nextLine().toUpperCase();
//            if (predicate.equals("DONE")) {
//                break;
//            }
//            predicates.add(predicate);
//        }

//        String[] predicatesArray = predicates.toArray(new String[0]);
        LogicTree evaluator = new LogicTree(expression);
        evaluator.printTree();
    }
}
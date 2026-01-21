// to run the program.
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        SeCoGA test = new SeCoGA();
        String input = "newCredit.arff"; // input the newcredit file to use the refined dataset. 
        test.covering(input);
    }
}
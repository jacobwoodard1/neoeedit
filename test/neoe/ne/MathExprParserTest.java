package neoe.ne;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class MathExprParserTest {

    @Test
    public void testSimpleAddition() {
        String expression = "2 + 3";
        MathExprParser parser = new MathExprParser(expression);
        BigDecimal result = parser.parse();
        assertEquals(5.0, result.doubleValue());
    }

    @Test
    public void testOrderOfOperations() {
        String expression = "2 + 3 * 4";
        MathExprParser parser = new MathExprParser(expression);
        BigDecimal result = parser.parse();
        assertEquals(14.0, result.doubleValue());
    }

    @Test
    public void testParentheses() {
        String expression = "(2 + 3) * 4";
        MathExprParser parser = new MathExprParser(expression);
        BigDecimal result = parser.parse();
        assertEquals(20.0, result.doubleValue());
    }

    @Test
    public void testDivision() {
        String expression = "10 / 2";
        MathExprParser parser = new MathExprParser(expression);
        BigDecimal result = parser.parse();
        assertEquals(5.0, result.doubleValue());
    }
}

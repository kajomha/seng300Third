import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.FileNotFoundException;
import java.io.IOException;

public class basicLocal {
    @Test
    public void TestCaseJava() {
        
        String path = testSuite.BASEDIR + "\\TestFiles\\BasicLocal";
        String[] args = new String[] {path};
        try {
            CountJavaTypes.main(args);
        } catch (FileNotFoundException e) {
            fail("A");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            fail("B");
        }
        assertEquals(1,CountJavaTypes.allCount[1]);
    }
}
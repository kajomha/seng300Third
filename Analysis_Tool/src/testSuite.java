import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ basicAnonymous.class, basicNested.class, basicLocal.class })
public class testSuite {
    // Please point to the testFiles dir on your machine
    public static String BASEDIR = "C:\\Users\\Ryan\\Repositories\\seng300Third";
}


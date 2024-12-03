import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.commons.jxpath.ri.parser.XPathParser;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Server {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        XPathParser p = new XPathParser(new StringReader(""));
        JXPathContext context = JXPathContext.newContext(new Person());
        context.getValue(getPath());
//        Person p = null;
//        p.foo();
//        sink(p);
    }

    public static String getPath() {
        char[] chs = "java.lang.System.exit(0)".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static String getName(){
        char[] chs = "exit".toCharArray();
        chs[0] = taint();
        chs[0] = 'e';
        return new String(chs);
    }

    public static char taint(){
        return 'j';
    }

    public static class Person {
        public String name;
        int age;
        public Student s;
        public void foo(){
            s.foo();
        }
    }

    public static class Student {
        public String name;
        int age;
        Person p;

        public void foo(){
            p.foo();
        }
    }

    public static void sink(Person p){
        p.foo();
    }

    public static class Man extends Person {

    }
}

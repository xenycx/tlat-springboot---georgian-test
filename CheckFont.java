import java.io.File;
public class CheckFont {
    public static void main(String[] args) {
        File f = new File("src/main/resources/fonts/DejaVuSans.ttf");
        System.out.println("Size: " + f.length());
    }
}


public class Main {
    public static int width = 800;
    public static int height = 600;

    public static void main(String[] args) {
        try {
            int vSync = 1;
            Engine gameEng = new Engine("Game", width, height, vSync);
            gameEng.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

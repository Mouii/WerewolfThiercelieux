import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        try {
            loadImageResourceExample("images/characters/Werewolf.jpg");
        } catch (IOException e) {
            System.err.println("Failed to read resource file!");
        }
    }

    // TODO: Offload this to a class and make it return the actual image, somehow
    /**
     * Example method to load an image from the Resources directory.
     * @throws IOException If the file could not be read.
     */
    private static void loadImageResourceExample(String path) throws IOException {
        Class<Main> mainClass = Main.class;

        try (InputStream imageStream = mainClass.getResourceAsStream(path)) {
            if (imageStream == null) {
                throw new MissingResourceException("Image not found", mainClass.getName(), path);
            }

            // Print the stream (= file) size to show the load was successful
            System.out.printf("File size: %d bytes%n", imageStream.available());
        }
    }
}
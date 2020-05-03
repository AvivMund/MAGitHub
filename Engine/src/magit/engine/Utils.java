package magit.engine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Utils {
    public static String writeToTextFile (String fullPath, String text) {
        try (PrintWriter writer = new PrintWriter(fullPath, "UTF-8")){
            Files.createDirectories(Paths.get(fullPath).getParent());
            writer.println(text);
            return "File saved successfully!";
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Problem when saving file " + fullPath);
            return "ERROR: Problem when saving file";
        } catch (UnsupportedEncodingException e) {
            System.out.println("ERROR: Problem when saving file " + fullPath);
            return "ERROR: Problem when saving file";
        } catch (IOException e) {
            System.out.println("ERROR: Problem when creating directory for file " + fullPath);
            e.printStackTrace();
            return "ERROR: Problem when creating directory for file";
        }
    }

    public static String readFromTextFile(String fullPath) {
        String data = null;
        File file = new File(fullPath);
        try (Scanner scanner = new Scanner(file)) {
            data = scanner.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "ERROR: Failed to load file content";
        }

        return data;
    }
}

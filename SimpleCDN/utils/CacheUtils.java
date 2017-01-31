package utils;

import java.io.*;

/**
 * Created by BenYin on 12/8/16.
 */
public class CacheUtils {

    public static byte[] readFromFile(String filePath) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            return ArrayUtils.shiftToBytes(bufferedInputStream);
        } catch (IOException e) {
            CDNLogger.error("failed to get content from the cache file");
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                CDNLogger.error("error happens when closing cache file input stream");
            }
        }
        return null;
    }

    public static void writeToFile(String filePath, byte[] data) {
        try {
            // write page content in cache
            File file = new File(filePath);
            if (!file.exists()) {
                File directory = file.getParentFile();
                if (directory != null && !directory.exists()) {
                    directory.mkdirs();
                }
                file.createNewFile();
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                output.write(data);
            }
        } catch (IOException e) {
            CDNLogger.error("failed to write cache!");
        }
    }

    public static int clearFileSize(String filePath) {
        int fileSize = 0;
        try {
            // get the size of file
            FileInputStream fileInputStream = new FileInputStream(filePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            fileSize = ArrayUtils.shiftToBytes(bufferedInputStream).length;
            // delete the file
            File file = new File(filePath);
            if (file.exists()) {
                CDNLogger.info("delete cache file: " + filePath);
                file.delete();
            }
        } catch (IOException e) {
            CDNLogger.error("failed to delete the file!");
        }
        return fileSize;
    }
}

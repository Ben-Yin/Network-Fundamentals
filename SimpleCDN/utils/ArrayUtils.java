package utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by HappyMole on 11/28/16.
 */
public class ArrayUtils {

    public static byte[] shiftToBytes(BufferedInputStream bufferedInputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int len;
        byte[] buf = new byte[1024];
        try {
            while ((len = bufferedInputStream.read(buf)) != -1) {
                byteArrayOutputStream.write(buf, 0, len);
                buf = new byte[1024];
            }
        } catch (IOException e) {
            CDNLogger.error("error happens when shifting data from input stream to bytes");
        } finally {
            try {
                bufferedInputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                // do nothing
            }
        }
        return byteArrayOutputStream.toByteArray();
    }


    public static byte[] concatArrays(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
        return result;
    }

    public static byte[] truncateArray(byte[] srcArr, int truncateLen) {
        byte[] destArr = new byte[truncateLen];
        System.arraycopy(srcArr, 0, destArr, 0, truncateLen);
        return destArr;
    }
}

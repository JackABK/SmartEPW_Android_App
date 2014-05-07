package com.example.android.SmartEPW.util;

/**
 * Created by jackabk on 3/1/14.
 */
public class FormatConvert {

    /*this method is used by int to float scale.
    * @param intVal.
    * @return floatVal.
    */
    public static float IntToFloatByScale(int intVal , float scale){
        float floatVal;
        floatVal = ((float)intVal) * scale;
        return floatVal;
    }


    /*there is convert byteArray To int and inverse relative*/
    /*refer to http://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java */
    public  static  int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /*ref to http://stackoverflow.com/questions/4266756/can-we-make-unsigned-byte-in-java*/
    public static int unsignedBytes(byte b) {
        return b & 0xFF;
    }
}

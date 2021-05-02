package com.fivesoft.imagereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class ImageReader {

    public static final int OK = 0;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_OUT_OF_MEMORY = -2;
    public static final int ERROR_WRONG_URI = -3;

    private final Context context;
    private Uri uri = null;
    private OnImageRead onImageRead = null;

    private ImageReader(Context context){
        this.context = context;
    }

    /**
     * Creates new ImageReader instance.
     * @param context A context necessary to read file.
     * @return new ImageReader instance
     */

    public static ImageReader with(Context context){
        return new ImageReader(context);
    }

    /**
     * Sets the file uri.
     * @param uri The uri of file you want to read.
     * @return current ImageReader instance
     */

    public ImageReader setUri(Uri uri){
        this.uri = uri;
        return this;
    }

    /**
     * Sets the interface called when results are ready
     * @param onImageRead The listener
     * @return current ImageReader instance
     */

    public ImageReader setListener(OnImageRead onImageRead){
        this.onImageRead = onImageRead;
        return this;
    }

    /**
     * Reads the image.
     */

    public void read(){
        if(uri == null){
            if(onImageRead != null)
                onImageRead.onResult(null, ERROR_WRONG_URI);
            return;
        }
        readImage();
    }

    private void readImage(){

        if(uri == null){
            if(onImageRead != null)
                onImageRead.onResult(null, ERROR_WRONG_URI);
            return;
        }

        try{
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                new Thread(() -> {
                    Bitmap bitmap = null;
                    try {
                        bitmap = modifyOrientation(BitmapFactory.decodeStream(inputStream), context.getContentResolver().openInputStream(uri));
                        inputStream.close();
                        if(onImageRead != null)
                            onImageRead.onResult(bitmap, OK);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(onImageRead != null)
                            onImageRead.onResult(bitmap, ERROR_UNKNOWN);
                    }
                }).start();
            } catch (OutOfMemoryError e){
                if(onImageRead != null)
                    onImageRead.onResult(null, ERROR_OUT_OF_MEMORY);
            }
        } catch (Exception e) {
            if(onImageRead != null)
                onImageRead.onResult(null, ERROR_UNKNOWN);
            e.printStackTrace();
        }

    }

    public interface OnImageRead {
        /**
         * Interface called when image reading finished.
         * @param res Result Bitmap
         * @param resCode Result code
         *                @value  0 - OK
         *                @value -1 - ERROR_UNKNOWN
         *                @value -2 - ERROR_OUT_OF_MEMORY
         *                @value -3 - ERROR_WRONG_URI
         */
        void onResult(Bitmap res, int resCode);
    }

    private static Bitmap modifyOrientation(Bitmap bitmap, InputStream inputStream) {
        try {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotate(bitmap, 90);

                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotate(bitmap, 180);

                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotate(bitmap, 270);

                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    return flip(bitmap, true, false);

                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    return flip(bitmap, false, true);

                default:
                    return bitmap;
            }

        } catch (IOException e){
            e.printStackTrace();
            return bitmap;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}

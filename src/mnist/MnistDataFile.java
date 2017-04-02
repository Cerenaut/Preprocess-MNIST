/*
 * Copyright (c) 2016.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Project AGI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
 */

package mnist;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by:  Richard Masoumi, based on the code by Gideon on 14/03/2016
 * Date:        28/3/17
 */

public class MnistDataFile {
    public static final int MNIST_MAGIC_NUMBER_IMAGE = 2051;
    public static final int MNIST_MAGIC_NUMBER_LABEl = 2049;


    private int _numberOfImages = 0;
    private int _numberOfRows = 0;
    private int _numberOfColumns = 0;
    private int _numberOfPixels = 0;
    private int _idx = 0;

    private String _labelFilePath = null;
    private String _imageFilePath = null;
    private FileInputStream _inImage = null;
    private FileInputStream _inLabel = null;

    private BufferedImage _image;
    private String _label;

    public MnistDataFile(String labelFilePath, String imageFilePath){
        _labelFilePath = labelFilePath;
        _imageFilePath = imageFilePath;

        init();
    }

    private void init() {
        try {
            _inLabel = new FileInputStream( _labelFilePath );
            _inImage = new FileInputStream( _imageFilePath );


            //skipping the magic number field
            _inImage.read();_inImage.read();_inImage.read();_inImage.read();

            //the input files are presented as "Big endian" format, so it has to be read in this way!
            _numberOfImages = ( _inImage.read() << 24 ) | ( _inImage.read() << 16 ) | ( _inImage.read() << 8 ) | ( _inImage.read() );
            _numberOfRows = ( _inImage.read() << 24 ) | ( _inImage.read() << 16 ) | ( _inImage.read() << 8 ) | ( _inImage.read() );
            _numberOfColumns = ( _inImage.read() << 24 ) | ( _inImage.read() << 16 ) | ( _inImage.read() << 8 ) | ( _inImage.read() );

            _numberOfPixels = _numberOfRows * _numberOfColumns;

            _inLabel.read();_inLabel.read();_inLabel.read();_inLabel.read();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * NOTE: This advances the image stream, so DOES advance to the next image.
     * This breaks the API of the interface.
     * <p/>
     * Needs fixing if this is to be used for anything other than pre-processing.
     *
     * @return
     */
    public BufferedImage getImage() {

        if( _image != null ) {
            return _image;
        }

        try {
            _image = new BufferedImage( _numberOfColumns, _numberOfRows, BufferedImage.TYPE_INT_ARGB );
            int[] imgPixels = new int[ _numberOfPixels ];

            for( int p = 0; p < _numberOfPixels; p++ ) {
                int gray = 255 - _inImage.read();
                imgPixels[ p ] = 0xFF000000 | ( gray << 16 ) | ( gray << 8 ) | gray;
            }

            _image.setRGB( 0, 0, _numberOfColumns, _numberOfRows, imgPixels, 0, _numberOfColumns );
            Integer labelInt = _inLabel.read();
            _label = labelInt.toString();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }

        return _image;
    }

    public String getLabel() {
        return _label;
    }

    public Map<String, Integer> getImageSize() {
        int w = 0;
        int h = 0;
        getImage();

        if( _image != null ) {
            w = _image.getWidth();
            h = _image.getHeight();
        }

        Map<String, Integer> dimension = new HashMap<>();
        dimension.put("width", w);
        dimension.put("height", h);
        return dimension;
    }

    public int nextImage() {
        _idx++;

        if( _idx >= _numberOfImages ) { // wrap around
            _idx = 0;
        }

        _image = null;      // clear 'cached' copy
        return _idx;
    }


    /**
     * THIS IS INEFFICIENT. It re-sets-up the file streams and seeks from the start each time.
     * It is a quick implementation as the class is not currently used for anything but pre-processing.
     */
    public boolean seek( int index ) {

        init();
        return skip( index );
    }


    public boolean skip( int index ) {
        if( index >= 0 && index < _numberOfImages ) {
            _idx = index;

            long numBytes = _idx * _numberOfPixels;
            try {
                _inImage.skip( numBytes );
                _inLabel.skip( _idx );
            }
            catch( IOException e ) {
                e.printStackTrace();
            }

            _image = null;      // clear 'cached' copy

            return true;
        }

        return false;
    }

    public int bufferSize() {
        return _numberOfImages;
    }

    public static boolean isValidMnistFile(String fileName, boolean isImageFile){
        boolean result;

        try {
            if(isImageFile){
                if(getIdxFileMagicNumber(fileName) == MNIST_MAGIC_NUMBER_IMAGE) {
                    result = true;
                } else {
                    result = false;
                }

            } else {
                if(getIdxFileMagicNumber(fileName) == MNIST_MAGIC_NUMBER_LABEl){
                    result = true;
                } else {
                    result = false;
                }
            }
        } catch (IOException e) {
            result = false;
        }

        return result;
    }

    private static int getIdxFileMagicNumber(String fileName) throws IOException{
        int magicNumber;
        FileInputStream inputFile;

        inputFile = new FileInputStream(fileName);
        magicNumber = ( inputFile.read() << 24 ) | ( inputFile.read() << 16 ) | ( inputFile.read() << 8 ) | ( inputFile.read() );
        inputFile.close();

        return magicNumber;
    }
}

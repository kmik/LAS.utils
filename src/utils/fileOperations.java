package utils;

import java.io.File;

public class fileOperations {

    /**
     * Create new file with extension
     * @param file
     * @param extension extension (including dot!)
     * @return
     */

    public File createNewFileWithNewExtension(File file, String extension){

        return new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + extension);


    }

    public File createNewFileWithNewExtension(String file, String extension){

        return new File(file.substring(0, file.lastIndexOf(".")) + extension);


    }

    public File createNewFileWithNewExtension(File file, String odir, String extension){

        String temp = odir + System.getProperty("file.separator") + file.getName();

        return new File(temp.substring(0, temp.lastIndexOf(".")) + extension);

    }

    public File transferDirectories(File file, String directory){

        //System.out.println("FILE: " + file.getAbsolutePath());
        //System.out.println("DIRECTORY: " + directory);

        File tempFile = new File(System.getProperty("file.separator") + directory + System.getProperty("file.separator") + file.getName());

        //System.out.println("TEMPFILE: " + tempFile.getAbsolutePath());

        //tempFile = new File("/media/koomikko/B8C80A93C80A4FD41/Linux_downloads/tes/asdi.las");

        //System.out.println("dir: " + directory);
        //System.out.println("fileName: " + file.getName());

        //System.out.println("WHAT THE FUCK! " + tempFile.getAbsolutePath());

        return tempFile;

    }


}

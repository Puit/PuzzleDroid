package com.p5m.puzzledroid.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Singleton class that stores the list of unresolved cat images.
 */
public class UnsolvedImages {

    private static UnsolvedImages instance = null;
    private List<String> unsolvedImages;
    private int numberOfImages;

    /**
     * Private constructor restricted to this class.
      */
    private UnsolvedImages() {
        unsolvedImages = new ArrayList<>();
        numberOfImages = 0;
    }

    /**
     * Procedure that returns the singleton.
     * @return
     */
    public static UnsolvedImages getInstance() {
        if (instance == null) {
            instance = new UnsolvedImages();
        }
        return instance;
    }

    /**
     * Return a random unsolved image.
     * @return
     */
    public static String getRandomUnsolvedImage() {
        List<String> unsolvedImages = getInstance().getUnsolvedImages();
        Random random = new Random();
        return unsolvedImages.get(random.nextInt(unsolvedImages.size()));
    }

    /**
     * Remove an unsolved image.
     */
    public static void removeUnsolvedImage(String image) {
        List<String> unsolvedImages = getInstance().getUnsolvedImages();
        unsolvedImages.remove(image);
        getInstance().setUnsolvedImages(unsolvedImages);
    }

    /**
     * Return the number of completed images.
     */
    public static int getNumberOfSolvedImages() {
        UnsolvedImages unsolvedImages = getInstance();
        return unsolvedImages.getNumberOfImages() - unsolvedImages.getUnsolvedImages().size() + 1;
    }

    /**
     * To display them better.
     * @return
     */
    @Override
    public String toString() {
        return "UnsolvedImages{" +
                "unsolvedImages=" + unsolvedImages +
                ", numberOfImages=" + numberOfImages +
                '}';
    }

    /**
     * Getters and setters.
     * @return
     */
    public List<String> getUnsolvedImages() {
        return unsolvedImages;
    }

    public void setUnsolvedImages(List<String> unsolvedImages) {
        this.unsolvedImages = unsolvedImages;
    }

    public static void setInstance(UnsolvedImages instance) {
        UnsolvedImages.instance = instance;
    }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public void setNumberOfImages(int numberOfImages) {
        this.numberOfImages = numberOfImages;
    }
}

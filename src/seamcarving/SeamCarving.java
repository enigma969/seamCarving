/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seamcarving;

import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 *
 * @author enigma
 */
public class SeamCarving {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String src = args[0];
        String width = args[1];
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(src));
        } catch (IOException e) {
            System.err.println("Image source not valid.");
        }
        BufferedImage gray = grayScaleImage(img);
//        double[][] energyMap = energyMap2(gray);
//        seamCarving(img, energyMap, 100);
    }

    private static void seamCarving(BufferedImage img, int[][] energyMap, int columnsToRemove) {

        for (int i = 0; i < columnsToRemove; i++) {
            int[][] cumulatedSeams = calcCumulatedSeams(energyMap);
            int x = findLowestPixelOfColumn(cumulatedSeams);

            int[] seamPath = new int[cumulatedSeams[0].length];
            seamPath[0] = x;
            int j = 1;

            for (int y = energyMap[0].length - 1; y > 0; y--) {
                seamPath[j] = getClosestNeighbour(cumulatedSeams, x, y);
                j++;
            }

            energyMap = removeSeamFromEnergyMap(energyMap, seamPath);
            img = paintSeam(img, seamPath);
        }
        showImage(img);
    }

    private static BufferedImage paintSeam(BufferedImage img, int path[]) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                img.setRGB(path[y], y, 255);
            }
        }
        return img;
    }

    private static void displayHistogram(final int[][] energy) {

        final int max = getMaxHeightOfArray(energy) / 2000000;
        final JFrame frame = buildFrame(energy.length, max);

        JPanel pane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int i = 0; i < energy.length - 1; i++) {
                    g.drawLine(i, frame.getHeight(), i, frame.getHeight() - energy[i][energy[0].length - 1] / 2000000);
                }
            }
        };

        frame.add(pane);
    }

    private static int getMaxHeightOfArray(int[][] arr) {
        int max = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i][arr[i].length - 1] > max) {
                max = arr[i][arr[i].length - 1];
            }
        }
        return max;
    }

    public static BufferedImage removeSeamFromImg(BufferedImage img, int[] seamPath) {
        BufferedImage newImg = new BufferedImage(img.getWidth() - 1, img.getHeight(), img.getType());
        /*
         Copy all pixels as they are to the new array; limit is the x position of the seam 
         */
        for (int y = 0; y < seamPath.length - 1; y++) {
            for (int x = 0; x < seamPath[y]; x++) {
                newImg.setRGB(x, y, img.getRGB(x, y));
            }
        }
        /*
         Shift all remaining pixels to the left
         */
        for (int y = 0; y < seamPath.length; y++) {
            for (int x = seamPath[y]; x < img.getWidth() - 1; x++) {
                newImg.setRGB(x, y, img.getRGB(x + 1, y));
            }
        }
        return newImg;
    }

    public static int[][] removeSeamFromEnergyMap(int[][] arr, int[] seamPath) {
        int[][] newArr = new int[arr.length - 1][arr[0].length];
        /*
         Copy all pixels as they are to the new array; limit is the x position of the seam 
         */
        for (int y = 0; y < seamPath.length; y++) {
            for (int x = 0; x < seamPath[y]; x++) {
                newArr[x][y] = arr[x][y];
            }
        }
        /*
         Shift all remaining pixels to the left
         */
        for (int y = 0; y < seamPath.length; y++) {
            for (int x = seamPath[y]; x < arr.length - 1; x++) {
                newArr[x][y] = arr[x + 1][y];
            }
        }
        return newArr;
    }

    private static int getClosestNeighbour(int[][] cumulatedSeams, int x, int y) {
        int[] tmp = new int[3];
        tmp[0] = (x - 1) > 0 ? cumulatedSeams[x - 1][y - 1] : Integer.MAX_VALUE;
        tmp[1] = cumulatedSeams[x][y - 1];
        tmp[2] = (x + 1) < cumulatedSeams.length ? cumulatedSeams[x + 1][y - 1] : Integer.MAX_VALUE;

        if (tmp[0] <= tmp[1] && tmp[0] <= tmp[2]) {
            return x - 1;
        } else if (tmp[1] <= tmp[0] && tmp[1] <= tmp[2]) {
            return x;
        } else {
            return x + 1;
        }
    }

    private static int findLowestPixelOfColumn(int[][] cumulatedSeams) {
        int tmp = Integer.MAX_VALUE;
        int pos = 0;

        for (int x = 0; x < cumulatedSeams.length - 1; x++) {
            if (cumulatedSeams[x][cumulatedSeams[x].length - 1] < tmp) {
                tmp = cumulatedSeams[x][cumulatedSeams[x].length - 1];
                pos = x;
            }
        }
        return pos;
    }

    private static int[][] energyMap(BufferedImage img) {
        int[][] energy = new int[img.getWidth()][img.getHeight()];

        BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        for (int x = 0; x < img.getWidth() - 1; x++) {
            for (int y = 0; y < img.getHeight() - 1; y++) {
                for (int x1 = -1; x1 < 2; x1++) {
                    for (int y1 = -1; y1 < 2; y1++) {
                        if (x + x1 > 0 && x + x1 < img.getWidth() && y + y1 > 0 && y + y1 < img.getHeight() && x + x1 != x && y + y1 != y) {
                            energy[x][y] += Math.abs(img.getRGB(x, y) - img.getRGB(x + x1, y + y1));
                        }
                    }
                }
                img2.setRGB(x, y, energy[x][y]);
            }
        }
        showImage(img2);
        displayHistogram(energy);
        return energy;
    }

    private static int[][] calcCumulatedSeams(int[][] energy) {
        int[][] cumulatedEnergy = new int[energy.length][energy[0].length];

        for (int x = 0; x < energy.length - 1; x++) {
            for (int y = 0; y < energy[0].length - 1; y++) {
                cumulatedEnergy[x][y] = energy[x][y];
            }
        }

        for (int y = 1; y < energy[0].length; y++) {
            for (int x = 1; x < energy.length - 1; x++) {
                int temp;
                int tempArray3[] = new int[3];
                tempArray3[0] = cumulatedEnergy[x - 1][y - 1];
                tempArray3[1] = cumulatedEnergy[x][y - 1];
                tempArray3[2] = cumulatedEnergy[x + 1][y - 1];
                temp = getSmallestValueOfArray(tempArray3) + cumulatedEnergy[x][y];
                cumulatedEnergy[x][y] = temp;
            }
        }
        return cumulatedEnergy;
    }

    private static void cumuImg(int[][] cumulated) {
        BufferedImage img = new BufferedImage(cumulated.length, cumulated[0].length, BufferedImage.TYPE_BYTE_GRAY);
        for (int x = 0; x < cumulated.length; x++) {
            for (int y = 0; y < cumulated[0].length; y++) {
                img.setRGB(x, y, cumulated[x][y]);
            }
        }
        showImage(img);
    }

    public static int getSmallestValueOfArray(int[] numbers) {
        int minValue = numbers[0];
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] < minValue) {
                minValue = numbers[i];
            }
        }
        return minValue;
    }

    private static BufferedImage grayScaleImage(BufferedImage img) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        BufferedImage gray = op.filter(img, null);
        return gray;
    }

    private static void showImage(final BufferedImage img) {
        JFrame frame = buildFrame(img.getWidth(), img.getHeight());
        JPanel pane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
            }
        };
        frame.add(pane);
    }

    private static JFrame buildFrame(int width, int height) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);
        return frame;
    }

}

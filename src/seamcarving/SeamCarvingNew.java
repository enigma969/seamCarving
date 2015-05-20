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
public class SeamCarvingNew {

    public static void main(String[] args) {
        String src = "berge.jpg";
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(src));
            BufferedImage sobelImg = applySobelFilter(img);
            showImage(sobelImg);
            double[][] cumulativeEnergyArray = getCumulativeEnergyArray(sobelImg);
            seamCarving(cumulativeEnergyArray, img);
        } catch (IOException e) {
            System.err.println("Source not valid!");
        }
    }

    public static void seamCarving(double[][] cumulativeEnergyArray, BufferedImage img) {
        for (int n = 0; n < 200; ++n) {
            printLowestCumulated(cumulativeEnergyArray);
            int[] path = findPath(cumulativeEnergyArray);
            cumulativeEnergyArray = removePathEnergyArray(cumulativeEnergyArray, path);
            img = removePathFromImage(img, path);
        }
        showImage(img);
    }

    private static void printLowestCumulated(double[][] cumulated) {
        for (int x = 0; x < cumulated[0].length; x++) {
            System.out.println(cumulated[cumulated.length - 1][x]);
        }
    }


    /*
     Not used yet.
     */
    private static BufferedImage drawPathOnImage(BufferedImage img, int[] path) {
        for (int y = 0; y < img.getHeight() - 1; y++) {
            img.setRGB(path[y], y, 255);
        }
        return img;
    }

    public static double[][] removePathEnergyArray(double[][] cumulativeEnergyArray, int[] path) {
        int width = cumulativeEnergyArray[0].length;
        int height = cumulativeEnergyArray.length;
        double[][] new_cumulativeEnergyArray = new double[height][width - 1];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x <= path[y] - 1; ++x) {
                new_cumulativeEnergyArray[y][x] = cumulativeEnergyArray[y][x];
            }
            for (int x = path[y]; x < width - 1; ++x) {
                new_cumulativeEnergyArray[y][x] = cumulativeEnergyArray[y][x + 1];
            }
        }
        return new_cumulativeEnergyArray;
    }

    public static int[] findPath(double[][] cumulativeEnergyArray) {
        int width = cumulativeEnergyArray[0].length;
        int height = cumulativeEnergyArray.length;
        int[] path = new int[height];

        double tmp = Double.MAX_VALUE;
        int start_index = 0;

        for (int x = 0; x < width; x++) {
            if (cumulativeEnergyArray[height - 1][x] < tmp) {
                tmp = cumulativeEnergyArray[height - 1][x];
                start_index = x;
            }
        }
        path[height - 1] = start_index;

        int minIndex;
        double[] tempArray2 = new double[3];
        for (int y = height - 1; y > 0; y--) {
            tempArray2[0] = (path[y] - 1) > 0 ? cumulativeEnergyArray[y - 1][path[y] - 1] : Double.MAX_VALUE;
            tempArray2[1] = cumulativeEnergyArray[y - 1][path[y]];
            tempArray2[2] = (path[y] + 1) < width ? cumulativeEnergyArray[y - 1][path[y] + 1] : Double.MAX_VALUE;
            minIndex = getMinIndex(tempArray2);
            path[y - 1] = path[y] + minIndex;
        }
        return path;
    }

    public static int getMinIndex(double[] arr) {
        double minValue = arr[0];
        int minIndex = -1;
        for (int i = -1; i < 2; i++) {
            if (arr[i + 1] < minValue) {
                minValue = arr[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    public static BufferedImage removePathFromImage(BufferedImage img, int[] path) {
        int type = img.getType();
        int width = img.getWidth();
        int height = img.getHeight();

        BufferedImage removePathImg = new BufferedImage(width - 1, height, type);

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < path[y]; ++x) {
                removePathImg.setRGB(x, y, img.getRGB(x, y));
            }
            for (int x = path[y]; x < width - 1; ++x) {
                removePathImg.setRGB(x, y, img.getRGB(x + 1, y));
            }
        }
        return removePathImg;
    }

    public static double[][] getCumulativeEnergyArray(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        double[][] cumulativeEnergy = new double[height][width];

        for (int y = 1; y < height; ++y) {
            for (int x = 1; x < width - 1; ++x) {
                cumulativeEnergy[y][x] = getGrayValue(x, y, img);
            }
        }

        for (int y = 1; y < height; ++y) {
            for (int x = 1; x < width - 1; ++x) {
                double temp = 0.0;
                double[] tempArr = new double[3];
                tempArr[0] = cumulativeEnergy[y - 1][x - 1];
                tempArr[1] = cumulativeEnergy[y - 1][x];
                tempArr[2] = cumulativeEnergy[y - 1][x + 1];
                temp = getMinValue(tempArr) + cumulativeEnergy[y][x];
                cumulativeEnergy[y][x] = temp;
            }
        }
        return cumulativeEnergy;
    }

    public static double getMinValue(double[] arr) {
        double minValue = arr[0];
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < minValue) {
                minValue = arr[i];
            }
        }
        return minValue;
    }

    private static BufferedImage applySobelFilter(BufferedImage img) {
        int[][] sobel_x = new int[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobel_y = new int[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        int sobelX;
        int sobelY;

        BufferedImage sobeledImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        for (int x = 1; x < img.getWidth() - 2; x++) {
            for (int y = 1; y < img.getHeight() - 2; y++) {
                sobelX = (sobel_x[0][0] * getGrayValue(x - 1, y - 1, img)) + (sobel_x[0][1] * getGrayValue(x, y - 1, img)) + (sobel_x[0][2] * getGrayValue(x + 1, y - 1, img))
                        + (sobel_x[1][0] * getGrayValue(x - 1, y, img)) + (sobel_x[1][1] * getGrayValue(x, y, img)) + (sobel_x[1][2] * getGrayValue(x + 1, y, img))
                        + (sobel_x[2][0] * getGrayValue(x - 1, y + 1, img)) + (sobel_x[2][1] * getGrayValue(x, y + 1, img)) + (sobel_x[2][2] * getGrayValue(x + 1, y + 1, img));

                sobelY = (sobel_y[0][0] * getGrayValue(x - 1, y - 1, img)) + (sobel_y[0][1] * getGrayValue(x, y - 1, img)) + (sobel_y[0][2] * getGrayValue(x + 1, y - 1, img))
                        + (sobel_y[1][0] * getGrayValue(x - 1, y, img)) + (sobel_y[1][1] * getGrayValue(x, y, img)) + (sobel_y[1][2] * getGrayValue(x + 1, y, img))
                        + (sobel_y[2][0] * getGrayValue(x - 1, y + 1, img)) + (sobel_y[2][1] * getGrayValue(x, y + 1, img)) + (sobel_y[2][2] * getGrayValue(x + 1, y + 1, img));
                int energy = (int) Math.sqrt((sobelX * sobelX) + (sobelY * sobelY));
                sobeledImg.setRGB(x, y, energy);
            }
        }

        return sobeledImg;
    }

    private static int getGrayValue(int x, int y, BufferedImage img) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        return (r + g + b) / 3;
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

    /*
     Not used.
     */
    private static BufferedImage grayScaleImage(BufferedImage img) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        BufferedImage gray = op.filter(img, null);
        return gray;
    }
}

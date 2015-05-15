/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seamcarving;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String src = args[0];
        String width = args[1];
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(src));
            BufferedImage sobelImg = applySobelFilter(img);
            double[][] cumulativeEnergyArray = getCumulativeEnergyArray(sobelImg);
            seamCarving(cumulativeEnergyArray, img);
        } catch (IOException e) {
            System.err.println("Image source not valid.");
        }
    }

    public static void seamCarving(double[][] cumulativeEnergyArray, BufferedImage img) {
        BufferedImage imgPainted = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int n = 0; n < 100; ++n) {
            int[] path = findPath(cumulativeEnergyArray);
            cumulativeEnergyArray = removePathEnergyArray(cumulativeEnergyArray, path);
            img = removePathFromImage(img, path);
            imgPainted = drawPathOnImage(img, path);
        }
        showImage(imgPainted);
        showImage(img);
    }

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

        double[] tempArray = new double[width - 10];
        int y = height - 1;
        for (int x = 5; x < width - 5; ++x) {
            tempArray[x - 5] = cumulativeEnergyArray[y][x];
        }

        int ind_bot = getMinIndex(tempArray) + 5;
        path[height - 1] = ind_bot;

        int ind_temp = 0;
        double[] tempArray2 = new double[3];
        for (int i = height - 1; i > 0; --i) {
            tempArray2[0] = cumulativeEnergyArray[i - 1][path[i] - 1];
            tempArray2[1] = cumulativeEnergyArray[i - 1][path[i]];
            tempArray2[2] = cumulativeEnergyArray[i - 1][path[i] + 1];
            ind_temp = getMinIndex(tempArray2);
            path[i - 1] = path[i] + ind_temp - 1;
            if (path[i - 1] <= 0) {
                path[i - 1] = 1;
            } else if (path[i - 1] >= width - 1) {
                path[i - 1] = width - 2;
            }
        }
        return path;
    }

    public static int getMinIndex(double[] numbers) {
        double minValue = numbers[0];
        int minIndex = 0;
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] < minValue) {
                minValue = numbers[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    public static BufferedImage removePathFromImage(BufferedImage img, int[] path) {
        int type = img.getType();
        int width = img.getWidth();
        int height = img.getHeight();
        int band = 3;
        BufferedImage removePathImg = new BufferedImage(width - 1, height, type);
        WritableRaster raster = removePathImg.getRaster();

        for (int b = 0; b < band; ++b) {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x <= path[y] - 2; ++x) {
                    double temp = 0.0;
                    temp = img.getRaster().getSample(x, y, b);
                    raster.setSample(x, y, b, Math.round(temp));
                }
                for (int x = path[y] - 1; x < width - 1; ++x) {
                    double temp = 0.0;
                    temp = img.getRaster().getSample(x + 1, y, b);
                    raster.setSample(x, y, b, Math.round(temp));
                }
            }
        }
        return removePathImg;
    }

    public static double[][] getCumulativeEnergyArray(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        double[][] cumulative_energy_array = new double[height][width];

        for (int y = 1; y < height; ++y) {
            for (int x = 1; x < width - 1; ++x) {
                cumulative_energy_array[y][x] = (double) img.getRaster().getSample(x, y, 0);
            }
        }

        for (int y = 1; y < height; ++y) {
            for (int x = 1; x < width - 1; ++x) {
                double temp = 0.0;
                double tempArray3[] = new double[3];
                tempArray3[0] = cumulative_energy_array[y - 1][x - 1];
                tempArray3[1] = cumulative_energy_array[y - 1][x];
                tempArray3[2] = cumulative_energy_array[y - 1][x + 1];
                temp = getMinValue(tempArray3) + (double) img.getRaster().getSample(x, y, 0);
                cumulative_energy_array[y][x] = temp;
            }
        }
        return cumulative_energy_array;
    }

    public static double getMinValue(double[] numbers) {
        double minValue = numbers[0];
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] < minValue) {
                minValue = numbers[i];
            }
        }
        return minValue;
    }

    private static BufferedImage applySobelFilter(BufferedImage img) {
        int[][] sobel_x = new int[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobel_y = new int[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        int sobelX;
        int sobelY;

        BufferedImage sobeledImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 1; x < img.getWidth() - 2; x++) {
            for (int y = 1; y < img.getHeight() - 2; y++) {
                sobelX = (sobel_x[0][0] * getGrayValue(x - 1, y - 1, img)) + (sobel_x[0][1] * getGrayValue(x, y - 1, img)) + (sobel_x[0][2] * getGrayValue(x + 1, y - 1, img))
                        + (sobel_x[1][0] * getGrayValue(x - 1, y, img)) + (sobel_x[1][1] * getGrayValue(x, y, img)) + (sobel_x[1][2] * getGrayValue(x + 1, y, img))
                        + (sobel_x[2][0] * getGrayValue(x - 1, y + 1, img)) + (sobel_x[2][1] * getGrayValue(x, y + 1, img)) + (sobel_x[2][2] * getGrayValue(x + 1, y + 1, img));

                sobelY = (sobel_y[0][0] * getGrayValue(x - 1, y - 1, img)) + (sobel_y[0][1] * getGrayValue(x, y - 1, img)) + (sobel_y[0][2] * getGrayValue(x + 1, y - 1, img))
                        + (sobel_y[1][0] * getGrayValue(x - 1, y, img)) + (sobel_y[1][1] * getGrayValue(x, y, img)) + (sobel_y[1][2] * getGrayValue(x + 1, y, img))
                        + (sobel_y[2][0] * getGrayValue(x - 1, y + 1, img)) + (sobel_y[2][1] * getGrayValue(x, y + 1, img)) + (sobel_y[2][2] * getGrayValue(x + 1, y + 1, img));
                int energy = (int) Math.sqrt((sobelX * sobelX) + (sobelY * sobelY)) * 500;
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

    private static BufferedImage grayScaleImage(BufferedImage img) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        BufferedImage gray = op.filter(img, null);
        return gray;
    }
}

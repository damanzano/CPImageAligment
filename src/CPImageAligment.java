import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class CPImageAligment extends PApplet {
	// File representing the folder that you select using a FileChooser
	static final File dir = new File("../data");

	// array of supported extensions (use a List if you prefer)
	static final String[] EXTENSIONS = new String[] { "jpg", "png", "bmp" };

	// filter to identify images based on their extensions
	static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

		@Override
		public boolean accept(final File dir, final String name) {
			for (final String ext : EXTENSIONS) {
				if (name.endsWith("." + ext)) {
					return (true);
				}
			}
			return (false);
		}
	};

	ArrayList<PImage> rawImages = new ArrayList<PImage>();
	ArrayList<PImage> blueImages = new ArrayList<PImage>();
	ArrayList<PImage> greenImages = new ArrayList<PImage>();
	ArrayList<PImage> redImages = new ArrayList<PImage>();
	ArrayList<PImage> resultImages = new ArrayList<PImage>();

	public void setup() {
		processImages();
		size(displayWidth, displayHeight);
		System.out.println("Raw images count:" + rawImages.size());
		System.out.println("Blue images count:" + blueImages.size());
		System.out.println("Green images count:" + greenImages.size());
		System.out.println("Red images count:" + redImages.size());
		System.out.println("Result images count:" + resultImages.size());

		noLoop();
	}

	public void draw() {
		if (!resultImages.isEmpty()) {
			int x = 0;
			int y = 0;

			for (int i = 0; i < resultImages.size(); i++) {
				PImage resImage = resultImages.get(i);

				if (x + resImage.width > width) {
					x = 0;
					y += resImage.height;
				}

				image(resImage, x, y);

				x += resImage.width + 5;
			}
		} else {
			System.out.println("Noy hay imágenes que mostrar");
		}
	}

	void processImages() {
		if (dir.isDirectory()) { // make sure it's a directory
			for (final File f : dir.listFiles(IMAGE_FILTER)) {
				PImage rawImage = loadImage("../data/" + f.getName());
				rawImages.add(rawImage);
				// you probably want something more involved here
				// to display in your UI
				System.out.println("image: " + f.getName());
				System.out.println(" width : " + rawImage.width);
				System.out.println(" height: " + rawImage.height);
				System.out.println("size  : " + f.length());

				transformImage(rawImage, f.getName());

			}
		}
	}

	void transformImage(PImage rawImage, String fileName) {
		System.out.println("Transforming image " + fileName);
		int imagesWidth = rawImage.width;
		int imagesHeight = rawImage.height / 3;

		// Get each color crop and save it
		PImage blueImage = rawImage.get(0, 0, imagesWidth, imagesHeight);
		blueImages.add(blueImage);
		PImage greenImage = rawImage.get(0, imagesHeight, imagesWidth,
				imagesHeight);
		greenImages.add(greenImage);
		PImage redImage = rawImage.get(0, imagesHeight * 2, imagesWidth,
				imagesHeight);
		redImages.add(redImage);

		// Get the better alignments between blue, green and blue, red images.
		System.out.println("Calculating blue, green alignment");
		PVector bgAligment = getBetterOffsetVector(blueImage, greenImage);
		System.out.println(bgAligment);
		System.out.println("Calculating blue, red alignment");
		PVector brAligment = getBetterOffsetVector(blueImage, redImage);
		System.out.println(brAligment);
		

		// Create new image and set its colors
		int maxXOffset = (int) max(abs(bgAligment.x),abs(brAligment.x));
		System.out.println("offsetX");
		int maxYOffset = (int) max(abs(bgAligment.y),abs(brAligment.y));
		int resultWidth = (imagesWidth+(2*maxXOffset));
		int resultHeight = (imagesHeight+(2*maxYOffset));
		PImage resultImage = createImage(resultWidth, resultHeight, RGB);
		
		System.out.println("Result image data");
		System.out.println(" width: "+resultWidth);
		System.out.println(" height: "+resultHeight);
		
		resultImage.loadPixels();
		for (int x = 0; x < imagesWidth; x++) {
			for (int y = 0; y < imagesHeight; y++) {
//				int greenX = (int) (x - bgAligment.x);
//				int greenY = (int) (y - bgAligment.y);
//				int redX = (int) (x - brAligment.x);
//				int redY = (int) (y - brAligment.y);
//
//				float blue = red(blueImage.get(x, y));
//				float green = red(greenImage.get(greenX, greenY));
//				float red = red(redImage.get(redX, redY));
//				
//				int resultPos = ((y+maxYOffset) * resultWidth) + x + maxXOffset;
//				resultImage.pixels[resultPos] = color(red, green,
//						blue);
				float rp = 0;
				float gp = 0;
				float bp = 0;
				
				int bluePos = ((y+maxYOffset) * resultWidth) + x + maxXOffset;
				float blue = blue(blueImage.get(x, y));
				
				// Get the actual levels at this pixel
				rp = red(resultImage.pixels[bluePos]);
				gp = green(resultImage.pixels[bluePos]);
				resultImage.pixels[bluePos] = color(0,0, blue);
				
				int greenPos = (int) (((y+maxYOffset+bgAligment.y) * resultWidth) + x + maxXOffset + bgAligment.x);
				float green = green(greenImage.get(x, y));
				
				// Get the actual levels at this pixel
				rp = red(resultImage.pixels[greenPos]);
				bp = blue(resultImage.pixels[greenPos]);
				resultImage.pixels[greenPos] = color(0,green, 0);
				
				int redPos = (int) (((y+maxYOffset+brAligment.y) * resultWidth) + x + maxXOffset + brAligment.x);
				float red = green(greenImage.get(x, y));
				
				// Get the actual levels at this pixel
				gp = green(resultImage.pixels[redPos]);
				bp = blue(resultImage.pixels[redPos]);
				resultImage.pixels[redPos] = color(red,0, 0);
				
			}
		}
		resultImage.updatePixels();
		resultImage.save("../naive_results/" + fileName);
		resultImages.add(resultImage);
	}

	public PVector getBetterOffsetVector(PImage image1, PImage image2) {
		// If no vector is found there is no displacement between images
		PVector offsetVector = new PVector(0, 0);
		int minSSD = ssd(image1, image2, offsetVector);
		
		// Test displacements up to 10% of the width or height
		int maxWoffset = (int) (image1.width*0.1);
		int maxHoffset = (int) (image1.height*0.1);

		for (int x = (-maxWoffset); x < maxWoffset; x++) {
			for (int y = (-maxHoffset); y < maxHoffset; y++) {
				PVector offsetTest = new PVector(x, y);
				int sqrSumTest = ssd(image1, image2, offsetTest);
				if (sqrSumTest < minSSD) {
					minSSD = sqrSumTest;
					offsetVector = offsetTest;
				}
			}
		}
		// System.out.println(offsetVector);
		return offsetVector;
	}

	public int ssd(PImage image1, PImage image2, PVector offset) {
		int ssd = 0;
		int xi = (int) max(0, offset.x);
		int xlim = (int) min(image1.width, image1.width + offset.x);
		int yi = (int) max(0, offset.y);
		int ylim = (int) min(image1.height, image1.height + offset.x);

		for (int x = xi; x < xlim; x++) {
			for (int y = yi; y < ylim; y++) {
				int x2 = (int) (x - offset.x);
				int y2 = (int) (y - offset.y);
				ssd += PApplet.pow(
						red(image1.get(x, y)) - red(image2.get(x2, y2)), 2);
			}
		}

		return ssd;
	}
}

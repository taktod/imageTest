package com.ttProject.test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import com.ttProject.util.HexUtil;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ImageTest {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * 画像のやりとりテスト
		URL url = new URL("http://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Googlelogo.png/250px-Googlelogo.png");
		BufferedImage img = ImageIO.read(url);
//		ImageIO.write(img, "PNG", new File("output.png"));
		BufferedImage base = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = base.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();

		ImageIO.write(base, "PNG", new File("output.png"));
		*/
		System.out.println("開始");
		new ImageTest();
		System.out.println("おわり");
	}
	private IContainer outContainer;
	private IStream outStream;
	private IStreamCoder coder;
	private IRational frameRate;
	public ImageTest() throws Exception {
		outContainer = IContainer.make();
		String outFile = "output.flv";
		int retVal = outContainer.open(outFile, IContainer.Type.WRITE, null);
		if(retVal < 0) {
			throw new RuntimeException("could not open output file.");
		}
		outStream = outContainer.addNewStream(ID.CODEC_ID_FLV1);
		coder = outStream.getStreamCoder();
		// fpsは15に設定
		frameRate = IRational.make(15, 1);
		coder.setNumPicturesInGroupOfPictures(30);
		
		coder.setBitRate(250000);
		coder.setBitRateTolerance(9000);
		coder.setPixelType(IPixelFormat.Type.YUV420P);
		coder.setHeight(240);
		coder.setWidth(320);
		coder.setGlobalQuality(100);
		
		coder.setFrameRate(frameRate);
		coder.setTimeBase(IRational.make(1, 1000));
		System.out.println(coder);

		retVal = coder.open(null, null);
		if(retVal < 0) {
			throw new RuntimeException("failed to open coder");
		}
		outContainer.writeHeader();

		// ここから画像データを書き込んでいくテスト
		// googleのロゴを取得しておく。
		URL url = new URL("http://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Googlelogo.png/250px-Googlelogo.png");
		BufferedImage img = ImageIO.read(url);

		int index = 0;
		System.out.println(frameRate.getDouble());
		long firstTimestamp = -1;
		IPacket packet = IPacket.make();
		while(index < 100) {
			index ++;
			System.out.println(index);
			Thread.sleep((long)(1000 / frameRate.getDouble()));
			long now = System.currentTimeMillis();
			BufferedImage base = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
			Graphics g = base.getGraphics();
			g.drawImage(img, 0, 0, null);
			g.dispose();
			if(firstTimestamp == -1) {
				firstTimestamp = now;
			}
			
			IConverter converter = null;
			try {
				converter = ConverterFactory.createConverter(base, IPixelFormat.Type.YUV420P);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			IVideoPicture picture = converter.toPicture(base, 1000 * (now - firstTimestamp));
//			picture.setQuality(0);
			retVal = coder.encodeVideo(packet, picture, 0);
			if(retVal < 0) {
				throw new RuntimeException("変換失敗");
			}
			if(packet.isComplete()) {
				// このgetByteBufferの部分がデータ本体になっているらしい。
				System.out.println(HexUtil.toHex(packet.getByteBuffer(), true));
//				System.out.println(packet.getData());
				retVal = outContainer.writePacket(packet);
				if(retVal < 0) {
					throw new RuntimeException("could not save packet to container.");
				}
			}
			else {
				System.out.println("packet is not complete.");
			}
		}
		outContainer.writeTrailer();
	}
}

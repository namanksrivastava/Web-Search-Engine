package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Vector;

/**
 * Created by sanchitmehta on 28/10/16.
 */
public class IndexCompressor {

    public static Vector<Byte> vByteEncoder(Vector<Integer> intArr) {
        Vector<Byte> outputStream = new Vector<Byte>();
        for (int n : intArr) {
            int count = 0;
            Vector<Byte> number = new Vector<Byte>();
            while (true) {
                int no = n % 128;
                number.add(0, (byte) no);
                if (n < 128)
                    break;
                n = n / 128;
                count++;
            }
            number.set(count, (byte) ((int) number.get(count) + 128));

            outputStream.addAll(number);
        }
        return outputStream;
    }

    public static Vector<Byte> vByteEncoder(int n) {
        int count = 0;
        Vector<Byte> number = new Vector<Byte>();
        while (true) {
            int no = n % 128;
            number.add(0, (byte) no);
            if (n < 128)
                break;
            n = n / 128;
            count++;
        }
        number.set(count, (byte) ((int) number.get(count) + 128));
        return number;
    }


    public static Vector<Integer> vByteDecoder(Vector<Byte> byteStream) {
        Vector<Integer> numbers = new Vector<Integer>();
        int n=0;
        for (int i = 0; i < byteStream.size(); i++) {
            int byteNo = (int) byteStream.get(i)& 0xFF;
            if (byteNo < 128)
                n = n * 128 + byteNo;
            else {
                n = n * 128 + byteNo - 128;
                numbers.add(n);
                n=0;
            }
        }
        return numbers;
    }

    public static void main(String args[]) throws IOException {
//        Vector<Integer> vecInt = new Vector<Integer>();
//        vecInt.add(44);
//        vecInt.add(209);
//        vecInt.add(123213);
//        vecInt.add(857);
//        vecInt.add(93939);
//        Vector<Byte> b= vByteEncoder(vecInt);
//        for (byte b1 : b) {
//            System.out.println(Integer.toBinaryString(b1 & 255 | 256).substring(1));
//        }
//
//        System.out.println(vByteDecoder(b));


//        BufferedWriter bf = new BufferedWriter(new FileWriter("data/abc.tsv", true));
//
//        for (Byte bt : b) {
//            bf.write(bt+"\t");
//        }
//
//        bf.close();

        String fileName = "data/index/index-comp-part-145.tsv";

        byte[] bytes = Files.readAllBytes(new File(fileName).toPath());


        Vector<Byte> vb = new Vector<>();
        for (byte b : bytes) {
            vb.add(b);
        }

        Vector<Integer> numbers = IndexCompressor.vByteDecoder(vb);

        System.out.println(numbers);


    }

}
